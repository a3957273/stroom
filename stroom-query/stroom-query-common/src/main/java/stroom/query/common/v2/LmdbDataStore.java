/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Any.AnySelector;
import stroom.dashboard.expression.v1.Bottom.BottomSelector;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Last.LastSelector;
import stroom.dashboard.expression.v1.Nth.NthSelector;
import stroom.dashboard.expression.v1.Selection;
import stroom.dashboard.expression.v1.Selector;
import stroom.dashboard.expression.v1.Top.TopSelector;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValSerialiser;
import stroom.pipeline.refdata.util.ByteBufferPool;
import stroom.query.api.v2.TableSettings;
import stroom.util.io.ByteSizeUnit;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.CursorIterable.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.KeyRange;
import org.lmdbjava.PutFlags;
import org.lmdbjava.Txn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

public class LmdbDataStore implements DataStore {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbDataStore.class);
    private static final int MIN_VALUE_SIZE = (int) ByteSizeUnit.KIBIBYTE.longBytes(1);
    private static final int MAX_VALUE_SIZE = (int) ByteSizeUnit.MEBIBYTE.longBytes(1);
    private static final int MIN_PAYLOAD_SIZE = (int) ByteSizeUnit.MEBIBYTE.longBytes(1);
    private static final int MAX_PAYLOAD_SIZE = (int) ByteSizeUnit.GIBIBYTE.longBytes(1);

    private static final long COMMIT_FREQUENCY_MS = 1000;
    private final LmdbEnvironment lmdbEnvironment;
    private final LmdbConfig lmdbConfig;
    private final Dbi<ByteBuffer> lmdbDbi;
//    private final ByteBufferPool byteBufferPool;

    private final CompiledField[] compiledFields;
    private final CompiledSorter<HasGenerators>[] compiledSorters;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final AtomicLong resultCount = new AtomicLong();
    private final ItemSerialiser itemSerialiser;
    private final boolean hasSort;

    private final AtomicBoolean hasEnoughData = new AtomicBoolean();
    private final AtomicBoolean drop = new AtomicBoolean();
    private final AtomicBoolean dropped = new AtomicBoolean();

    private final AtomicBoolean createPayload = new AtomicBoolean();
    private final AtomicReference<byte[]> currentPayload = new AtomicReference<>();

    private final LinkedBlockingQueue<Optional<QueueItem>> queue = new LinkedBlockingQueue<>(1000000);
    private final AtomicLong uniqueKey = new AtomicLong();

    private final CountDownLatch addedData;
    private final LmdbKey rootParentRowKey;

    LmdbDataStore(final LmdbEnvironment lmdbEnvironment,
                  final LmdbConfig lmdbConfig,
                  final ByteBufferPool byteBufferPool,
                  final String queryKey,
                  final String componentId,
                  final TableSettings tableSettings,
                  final FieldIndex fieldIndex,
                  final Map<String, String> paramMap,
                  final Sizes maxResults,
                  final Sizes storeSize) {
        this.lmdbEnvironment = lmdbEnvironment;
        this.lmdbConfig = lmdbConfig;
        this.maxResults = maxResults;

        compiledFields = CompiledFields.create(tableSettings.getFields(), fieldIndex, paramMap);
        compiledDepths = new CompiledDepths(compiledFields, tableSettings.showDetail());
        compiledSorters = CompiledSorter.create(compiledDepths.getMaxDepth(), compiledFields);

        itemSerialiser = new ItemSerialiser(compiledFields);
        rootParentRowKey = new LmdbKey.Builder()
                .keyBytes(itemSerialiser.toBytes(Key.root()))
                .build();

        this.lmdbDbi = lmdbEnvironment.openDbi(queryKey, UUID.randomUUID().toString());
//        this.byteBufferPool = byteBufferPool;

        // Find out if we have any sorting.
        boolean hasSort = false;
        for (final CompiledSorter<HasGenerators> sorter : compiledSorters) {
            if (sorter != null) {
                hasSort = true;
                break;
            }
        }
        this.hasSort = hasSort;

        // Start transfer loop.
        addedData = new CountDownLatch(1);
        // TODO : Use provided executor but don't allow it to be terminated by search termination.
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this::transfer);
    }

    private void commit(final Txn<ByteBuffer> writeTxn) {
        Metrics.measure("Commit", () -> {
            writeTxn.commit();
            writeTxn.close();
        });
    }

    @Override
    public CompletionState getCompletionState() {
        return new CompletionState() {
            @Override
            public void complete() {
                try {
                    queue.put(Optional.empty());
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                    addedData.countDown();
                }
            }

            @Override
            public boolean isComplete() {
                boolean complete = true;

                try {
                    complete = addedData.await(0, TimeUnit.MILLISECONDS);
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return complete;
            }

            @Override
            public void awaitCompletion() throws InterruptedException {
                addedData.await();
            }

            @Override
            public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
                return addedData.await(timeout, unit);
            }

            @Override
            public void accept(final Long value) {
                complete();
            }
        };
    }

    @Override
    public void add(final Val[] values) {
        final int[] groupSizeByDepth = compiledDepths.getGroupSizeByDepth();
        final boolean[][] groupIndicesByDepth = compiledDepths.getGroupIndicesByDepth();
        final boolean[][] valueIndicesByDepth = compiledDepths.getValueIndicesByDepth();

        Key key = Key.root();
        LmdbKey parentRowKey = rootParentRowKey;

        for (int depth = 0; depth < groupIndicesByDepth.length; depth++) {
            final LmdbKey.Builder rowKeyBuilder = new LmdbKey.Builder();
            final Generator[] generators = new Generator[compiledFields.length];

            final int groupSize = groupSizeByDepth[depth];
            final boolean[] groupIndices = groupIndicesByDepth[depth];
            final boolean[] valueIndices = valueIndicesByDepth[depth];

            Val[] groupValues = ValSerialiser.EMPTY_VALUES;
            if (groupSize > 0) {
                groupValues = new Val[groupSize];
            }

            int groupIndex = 0;
            for (int fieldIndex = 0; fieldIndex < compiledFields.length; fieldIndex++) {
                final CompiledField compiledField = compiledFields[fieldIndex];

                final Expression expression = compiledField.getExpression();
                if (expression != null) {
                    Generator generator = null;
                    Val value = null;

                    // If this is the first level then check if we should filter out this data.
                    if (depth == 0) {
                        final CompiledFilter compiledFilter = compiledField.getCompiledFilter();
                        if (compiledFilter != null) {
                            generator = expression.createGenerator();
                            generator.set(values);

                            // If we are filtering then we need to evaluate this field
                            // now so that we can filter the resultant value.
                            value = generator.eval();

                            if (!compiledFilter.match(value.toString())) {
                                // We want to exclude this item so get out of this method ASAP.
                                return;
                            }
                        }
                    }

                    // If we are grouping at this level then evaluate the expression and add to the group values.
                    if (groupIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (value == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                            value = generator.eval();
                        }
                        groupValues[groupIndex++] = value;
                    }

                    // If we need a value at this level then evaluate the expression and add the value.
                    if (valueIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (generator == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                        }
                        generators[fieldIndex] = generator;
                    }
                }
            }

//            // Trim group values.
//            if (groupIndex < groupSize) {
//                groupValues = Arrays.copyOf(groupValues, groupIndex);
//            }
//

            final boolean grouped = depth <= compiledDepths.getMaxGroupDepth();
            final byte[] keyBytes;
            if (grouped) {
                // This is a grouped item.
                final KeyPart keyPart = new GroupKeyPart(groupValues);
                key = key.resolve(keyPart);
                keyBytes = itemSerialiser.toBytes(key);

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .keyBytes(keyBytes)
                        .group(true)
                        .build();
                final LmdbValue rowValue = new LmdbValue(itemSerialiser, keyBytes, generators);
                parentRowKey = rowKey;
                put(new QueueItem(rowKey, rowValue));

            } else {
                // This item will not be grouped.
                final long uniqueId = getUniqueId();
                final KeyPart keyPart = new UngroupedKeyPart(uniqueId);
                key = key.resolve(keyPart);
                keyBytes = itemSerialiser.toBytes(key);

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .uniqueId(uniqueId)
                        .group(false)
                        .build();
                final LmdbValue rowValue = new LmdbValue(itemSerialiser, keyBytes, generators);
                put(new QueueItem(rowKey, rowValue));
            }
        }
    }

    private long getUniqueId() {
        return uniqueKey.incrementAndGet();
    }

    private void put(final QueueItem item) {
        LOGGER.trace(() -> "put");
        if (Thread.currentThread().isInterrupted() || hasEnoughData.get()) {
            return;
        }

        totalResultCount.incrementAndGet();

        // Some searches can be terminated early if the user is not sorting or grouping.
        if (!hasSort && !compiledDepths.hasGroup()) {
            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
            // the client
            if (maxResults != null && totalResultCount.get() >= maxResults.size(0)) {
                hasEnoughData.set(true);
            }
        }

        try {
            queue.put(Optional.of(item));
        } catch (final InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
        }
    }

    private void transfer() {
        Metrics.measure("Transfer", () -> {
            try {
                Txn<ByteBuffer> writeTxn = null;
                boolean run = true;
                boolean needsCommit = false;
                long lastCommitMs = System.currentTimeMillis();

                while (run) {
                    final Optional<QueueItem> optional = queue.poll(1, TimeUnit.SECONDS);
                    if (optional != null) {
                        if (optional.isPresent()) {
                            if (writeTxn == null) {
                                writeTxn = lmdbEnvironment.txnWrite();
                            }

                            final QueueItem item = optional.get();
                            insert(writeTxn, item);

                        } else {
                            // Stop looping.
                            run = false;
                            // Ensure final commit.
                            lastCommitMs = 0;
                        }

                        // We have either added something or need a final commit.
                        needsCommit = true;
                    }

                    // TODO : @66 I'm not sure that the final payload will be created as this code will be skipped if
                    //  the current payload hasn't been taken, unless the completion state is only set after the
                    //  penultimate payload transfer.
                    if (createPayload.get() && currentPayload.get() == null) {
                        // Commit
                        if (writeTxn != null) {
                            // Commit
                            lastCommitMs = System.currentTimeMillis();
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }

                        // Create payload and clear the DB.
                        currentPayload.set(createPayload());

                    } else if (needsCommit && writeTxn != null) {
                        final long now = System.currentTimeMillis();
                        if (lastCommitMs < now - COMMIT_FREQUENCY_MS) {
                            // Commit
                            lastCommitMs = now;
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }
                    }
                }

            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                // Continue to interrupt.
                Thread.currentThread().interrupt();
            } finally {
                addedData.countDown();

                // Drop the DB if we have been instructed to do so.
                if (drop.get()) {
                    drop();
                }
            }
        });
    }

    private void insert(final Txn<ByteBuffer> txn, final QueueItem queueItem) {
        Metrics.measure("Insert", () -> {
            try {
                LOGGER.trace(() -> "insert");

                final LmdbKey rowKey = queueItem.getRowKey();
                final LmdbValue rowValue = queueItem.getRowValue();

                // Just try to put first.
                final boolean success = put(
                        txn,
                        rowKey.getByteBuffer(),
                        rowValue.getByteBuffer(),
                        PutFlags.MDB_NOOVERWRITE);
                if (success) {
                    resultCount.incrementAndGet();

                } else if (rowKey.isGroup()) {
                    // Get the existing entry for this key.
                    final ByteBuffer existingValueBuffer = lmdbDbi.get(txn, rowKey.getByteBuffer());

                    final int minValueSize = Math.max(MIN_VALUE_SIZE, existingValueBuffer.remaining());
                    try (final UnsafeByteBufferOutput output =
                            new UnsafeByteBufferOutput(minValueSize, MAX_VALUE_SIZE)) {
                        boolean merged = false;

                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(existingValueBuffer)) {
                            while (!input.end()) {
                                final LmdbValue existingRowValue = LmdbValue.read(itemSerialiser, input);

                                // If this is the same value the update it and reinsert.
                                if (Arrays.equals(existingRowValue.getFullKey(), rowValue.getFullKey())) {
                                    final Generator[] generators = existingRowValue.getGenerators();
                                    final Generator[] newValue = rowValue.getGenerators();
                                    final Generator[] combined = combine(generators, newValue);

                                    LOGGER.debug("Merging combined value to output");
                                    final LmdbValue combinedValue = new LmdbValue(
                                            itemSerialiser,
                                            existingRowValue.getFullKey(),
                                            combined);
                                    combinedValue.write(output);

                                    // Copy any remaining values.
                                    if (!input.end()) {
                                        final byte[] remainingBytes = input.readAllBytes();
                                        output.writeBytes(remainingBytes, 0, remainingBytes.length);
                                    }

                                    merged = true;

                                } else {
                                    LOGGER.debug("Copying value to output");
                                    existingRowValue.write(output);
                                }
                            }
                        }

                        // Append if we didn't merge.
                        if (!merged) {
                            LOGGER.debug("Appending value to output");
                            rowValue.write(output);
                            resultCount.incrementAndGet();
                        }

                        final ByteBuffer newValue = output.getByteBuffer().flip();
                        final boolean ok = put(txn, rowKey.getByteBuffer(), newValue);
                        if (!ok) {
                            throw new RuntimeException("Unable to update");
                        }
                    }

                } else {
                    // We do not expect a key collision here.
                    throw new RuntimeException("Unexpected collision");
                }

            } catch (final RuntimeException | IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Error putting " + queueItem, e);
            }
        });
    }

    private boolean put(final Txn<ByteBuffer> txn,
                        final ByteBuffer key,
                        final ByteBuffer val,
                        final PutFlags... flags) {
        try {
            return lmdbDbi.put(txn, key, val, flags);
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    private Generator[] combine(final Generator[] existing, final Generator[] value) {
        return Metrics.measure("Combine", () -> {
            // Combine the new item into the original item.
            for (int i = 0; i < existing.length; i++) {
                Generator existingGenerator = existing[i];
                Generator newGenerator = value[i];
                if (newGenerator != null) {
                    if (existingGenerator == null) {
                        existing[i] = newGenerator;
                    } else {
                        existingGenerator.merge(newGenerator);
                    }
                }
            }

            return existing;
        });
    }

    @Override
    public void clear() {
        // If the queue is still being transferred then set the drop flag and tell the transfer process to complete.
        drop.set(true);
        queue.clear();
        getCompletionState().complete();

        try {
            // If we are already complete then drop the DB directly.
            final boolean complete = addedData.await(0, TimeUnit.MILLISECONDS);
            if (complete) {
                drop();
            }
        } catch (final InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
            drop();
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void drop() {
        if (!dropped.get()) {
            try (final Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                LOGGER.info("Dropping: " + new String(lmdbDbi.getName(), StandardCharsets.UTF_8));
                lmdbDbi.drop(writeTxn, true);
                writeTxn.commit();
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                lmdbEnvironment.list();
            } finally {
                resultCount.set(0);
                totalResultCount.set(0);
                dropped.set(true);
                lmdbEnvironment.list();
            }
        }
    }

    private byte[] createPayload() {
        final PayloadOutput payloadOutput = new PayloadOutput(MIN_PAYLOAD_SIZE, MAX_PAYLOAD_SIZE);

        Metrics.measure("createPayload", () -> {
            try (Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                final long limit = lmdbConfig.getPayloadLimit().getBytes();
                if (limit > 0) {
                    final AtomicLong count = new AtomicLong();

                    try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(writeTxn)) {
                        final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();
                        while (count.get() < limit && iterator.hasNext()) {
                            final KeyVal<ByteBuffer> kv = iterator.next();
                            final ByteBuffer keyBuffer = kv.key();
                            final ByteBuffer valBuffer = kv.val();

                            // Add to the size of the current payload.
                            count.addAndGet(keyBuffer.remaining());
                            count.addAndGet(valBuffer.remaining());

                            payloadOutput.writeInt(keyBuffer.remaining());
                            payloadOutput.writeByteBuffer(keyBuffer);
                            payloadOutput.writeInt(valBuffer.remaining());
                            payloadOutput.writeByteBuffer(valBuffer);

                            lmdbDbi.delete(writeTxn, keyBuffer.flip());
                        }
                    }

                    writeTxn.commit();

                } else {
                    lmdbDbi.iterate(writeTxn).forEach(kv -> {
                        final ByteBuffer keyBuffer = kv.key();
                        final ByteBuffer valBuffer = kv.val();

                        payloadOutput.writeInt(keyBuffer.remaining());
                        payloadOutput.writeByteBuffer(keyBuffer);
                        payloadOutput.writeInt(valBuffer.remaining());
                        payloadOutput.writeByteBuffer(valBuffer);
                    });

                    lmdbDbi.drop(writeTxn);
                    writeTxn.commit();
                }

            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Error clearing DB", e);
            }
        });

        return payloadOutput.toBytes();
    }

    @Override
    public void writePayload(final Output output) {
        Metrics.measure("writePayload", () -> {
            try {
                final boolean complete = addedData.await(0, TimeUnit.MILLISECONDS);
                createPayload.set(true);

                final List<byte[]> payloads = new ArrayList<>(2);

                final byte[] payload = currentPayload.getAndSet(null);
                if (payload != null) {
                    payloads.add(payload);
                }

                if (complete) {
                    final byte[] finalPayload = createPayload();
                    payloads.add(finalPayload);
                }

                output.writeInt(payloads.size());
                payloads.forEach(bytes -> {
                    output.writeInt(bytes.length);
                    output.writeBytes(bytes);
                });

            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                // Continue to interrupt this thread.
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public boolean readPayload(final Input input) {
        return Metrics.measure("readPayload", () -> {
            final int count = input.readInt(); // There may be more than one payload if it was the final transfer.
            for (int i = 0; i < count; i++) {
                final int length = input.readInt();
                if (length > 0) {
                    final byte[] bytes = input.readBytes(length);
                    try (final Input in = new Input(new ByteArrayInputStream(bytes))) {
                        while (!in.end()) {
                            final int rowKeyLength = in.readInt();
                            final byte[] key = in.readBytes(rowKeyLength);
                            final ByteBuffer keyBuffer = ByteBuffer.allocateDirect(key.length);
                            keyBuffer.put(key, 0, key.length);
                            keyBuffer.flip();

                            final int valueLength = in.readInt();
                            final byte[] value = in.readBytes(valueLength);
                            final ByteBuffer valueBuffer = ByteBuffer.allocateDirect(value.length);
                            valueBuffer.put(value, 0, value.length);
                            valueBuffer.flip();

                            LmdbKey rowKey = new LmdbKey(keyBuffer);
                            if (!rowKey.isGroup()) {
                                // Create a new unique key if this isn't a group key.
                                rowKey.makeUnique(this::getUniqueId);
                            }

                            final QueueItem queueItem =
                                    new QueueItem(rowKey, new LmdbValue(itemSerialiser, valueBuffer));
                            put(queueItem);
                        }
                    }
                }
            }

            // Return success if we have not been asked to terminate and we are still willing to accept data.
            return !Thread.currentThread().isInterrupted() && !hasEnoughData.get();
        });
    }

    @Override
    public Items get() {
        return get(null);
    }

    @Override
    public Items get(final RawKey rawParentKey) {
        return Metrics.measure("get", () -> {
            Key parentKey = Key.root();
            if (rawParentKey != null) {
                parentKey = itemSerialiser.toKey(rawParentKey);
            }
            final int depth = parentKey.getDepth() + 1;
            final int trimmedSize = maxResults.size(depth);

            final ItemArrayList list = getChildren(parentKey, depth, trimmedSize, true, false);

            return new Items() {
                @Override
                @Nonnull
                public Iterator<Item> iterator() {
                    return new Iterator<>() {
                        private int pos = 0;

                        @Override
                        public boolean hasNext() {
                            return list.size > pos;
                        }

                        @Override
                        public Item next() {
                            return list.array[pos++];
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        });
    }

    private ItemArrayList getChildren(final Key parentKey,
                                      final int depth,
                                      final int trimmedSize,
                                      final boolean allowSort,
                                      final boolean trimTop) {
        final ItemArrayList list = new ItemArrayList(10);

        final ByteBuffer start = LmdbKey.createKeyStem(depth, parentKey, itemSerialiser);
        final KeyRange<ByteBuffer> keyRange = KeyRange.atLeast(start);

        final int maxSize;
        if (trimmedSize < Integer.MAX_VALUE / 2) {
            maxSize = Math.max(1000, trimmedSize * 2);
        } else {
            maxSize = Integer.MAX_VALUE;
        }
        final CompiledSorter<HasGenerators> sorter = compiledSorters[depth];
        boolean trimmed = true;

        boolean inRange = true;
        try (final Txn<ByteBuffer> readTxn = lmdbEnvironment.txnRead()) {
            try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(readTxn, keyRange)) {
                final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();

                while (iterator.hasNext() && inRange && !Thread.currentThread().isInterrupted()) {
                    final KeyVal<ByteBuffer> keyVal = iterator.next();

                    // Make sure the first part of the row key matches the start key we are looking for.
                    boolean match = true;
                    for (int i = 0; i < start.remaining() && match; i++) {
                        if (start.get(i) != keyVal.key().get(i)) {
                            match = false;
                        }
                    }

                    if (match) {
                        final ByteBuffer valueBuffer = keyVal.val();
                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(valueBuffer)) {
                            while (!input.end() && inRange) {
                                final int fullKeyLength = input.readInt();
                                final byte[] fullKey = input.readBytes(fullKeyLength);
                                final int generatorsLength = input.readInt();
                                final byte[] generatorBytes = input.readBytes(generatorsLength);

                                final Key key = itemSerialiser.toKey(fullKey);
                                if (key.getParent().equals(parentKey)) {
                                    final Generator[] generators = itemSerialiser.readGenerators(generatorBytes);

                                    list.add(new ItemImpl(this, new RawKey(fullKey), key, generators));
                                    if (!allowSort && list.size >= trimmedSize) {
                                        // Stop without sorting etc.
                                        inRange = false;

                                    } else {
                                        trimmed = false;
                                        if (list.size() > maxSize) {
                                            list.sortAndTrim(sorter, trimmedSize, trimTop);
                                            trimmed = true;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        inRange = false;
                    }
                }
            }
        }

        if (!trimmed) {
            list.sortAndTrim(sorter, trimmedSize, trimTop);
        }

        return list;
    }


    @Override
    public long getSize() {
        return resultCount.get();
    }

    @Override
    public long getTotalSize() {
        return totalResultCount.get();
    }

    private static class ItemArrayList {

        private final int minArraySize;
        private ItemImpl[] array;
        private int size;

        public ItemArrayList(final int minArraySize) {
            this.minArraySize = minArraySize;
            array = new ItemImpl[minArraySize];
        }

        void sortAndTrim(final CompiledSorter<HasGenerators> sorter,
                         final int trimmedSize,
                         final boolean trimTop) {
            if (sorter != null && size > 0) {
                Arrays.sort(array, 0, size, sorter);
            }
            if (size > trimmedSize) {
                final int len = Math.max(minArraySize, trimmedSize);
                final ItemImpl[] newArray = new ItemImpl[len];
                if (trimTop) {
                    System.arraycopy(array, array.length - trimmedSize, newArray, 0, trimmedSize);
                } else {
                    System.arraycopy(array, 0, newArray, 0, trimmedSize);
                }
                array = newArray;
                size = trimmedSize;
            }
        }

        void add(final ItemImpl item) {
            if (array.length <= size) {
                final ItemImpl[] newArray = new ItemImpl[size * 2];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
            }
            array[size++] = item;
        }

        ItemImpl get(final int index) {
            return array[index];
        }

        int size() {
            return size;
        }
    }

    public static class ItemImpl implements Item, HasGenerators {

        private final LmdbDataStore lmdbDataStore;
        private final RawKey rawKey;
        private final Key key;
        private final Generator[] generators;

        public ItemImpl(final LmdbDataStore lmdbDataStore,
                        final RawKey rawKey,
                        final Key key,
                        final Generator[] generators) {
            this.lmdbDataStore = lmdbDataStore;
            this.rawKey = rawKey;
            this.key = key;
            this.generators = generators;
        }

        @Override
        public RawKey getRawKey() {
            return rawKey;
        }

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public Val getValue(final int index) {
            Val val = null;

            final Generator generator = generators[index];
            if (generator instanceof Selector) {
                if (key.isGrouped()) {
                    int maxRows = 1;
                    boolean sort = true;
                    boolean trimTop = false;

                    if (generator instanceof AnySelector) {
                        sort = false;
//                    } else if (generator instanceof FirstSelector) {
                    } else if (generator instanceof LastSelector) {
                        trimTop = true;
                    } else if (generator instanceof TopSelector) {
                        maxRows = ((TopSelector) generator).getLimit();
                    } else if (generator instanceof BottomSelector) {
                        maxRows = ((BottomSelector) generator).getLimit();
                        trimTop = true;
                    } else if (generator instanceof NthSelector) {
                        maxRows = ((NthSelector) generator).getPos();
                    }

                    final ItemArrayList items = lmdbDataStore.getChildren(
                            key,
                            key.getDepth() + 1,
                            maxRows,
                            sort,
                            trimTop);

                    final Selector selector = (Selector) generator;
                    val = selector.select(new Selection<>() {
                        @Override
                        public int size() {
                            return items.size;
                        }

                        @Override
                        public Val get(final int pos) {
                            if (pos < items.size) {
                                items.get(pos).generators[index].eval();
                            }
                            return ValNull.INSTANCE;
                        }
                    });

                } else {
                    val = generator.eval();
                }
            } else if (generator != null) {
                val = generator.eval();
            }

            return val;
        }

        @Override
        public Generator[] getGenerators() {
            return generators;
        }
    }


    private static class QueueItem {

        private final LmdbKey rowKey;
        private final LmdbValue rowValue;

        public QueueItem(final LmdbKey rowKey,
                         final LmdbValue rowValue) {
            this.rowKey = rowKey;
            this.rowValue = rowValue;
        }

        public LmdbKey getRowKey() {
            return rowKey;
        }

        public LmdbValue getRowValue() {
            return rowValue;
        }
    }
}

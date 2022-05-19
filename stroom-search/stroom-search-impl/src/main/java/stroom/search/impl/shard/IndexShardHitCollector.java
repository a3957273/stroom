/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.search.impl.shard;

import stroom.index.shared.IndexShard;
import stroom.query.api.v2.QueryKey;
import stroom.query.common.v2.SearchProgressLog;
import stroom.query.common.v2.SearchProgressLog.SearchPhase;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskTerminatedException;
import stroom.util.NullSafe;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SimpleCollector;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

class IndexShardHitCollector extends SimpleCollector {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardHitCollector.class);

    private final TaskContext taskContext;
    private final IndexShard indexShard;
    private final QueryKey queryKey;
    private final Query query;

    //an empty optional is used as a marker to indicate no more items will be added
    private final DocIdQueue docIdQueue;
    private final LongAdder totalHitCount;
    private final LongAdder localHitCount = new LongAdder();
    private int docBase;

    IndexShardHitCollector(final TaskContext taskContext,
                           final QueryKey queryKey,
                           final IndexShard indexShard,
                           final Query query,
                           final DocIdQueue docIdQueue,
                           final LongAdder totalHitCount) {
        this.taskContext = taskContext;
        this.indexShard = indexShard;
        this.queryKey = queryKey;
        this.query = query;
        this.docIdQueue = docIdQueue;
        this.totalHitCount = totalHitCount;

        info(() -> "Searching...");
    }

    @Override
    protected void doSetNextReader(final LeafReaderContext context) throws IOException {
        super.doSetNextReader(context);
        docBase = context.docBase;
    }

    @Override
    public void collect(final int doc) {
        // The interrupt status seems to be cleared somewhere is Lucene code so check here if we should terminate.
        if (taskContext.isTerminated()) {
            info(() -> "Quitting...");
            LOGGER.debug("Quitting (terminated). {}, query term [{}]", this, query);
            throw new TaskTerminatedException();
        }

        // Pause the current search if the deque is full.
        final int docId = docBase + doc;

        try {
            SearchProgressLog.increment(queryKey, SearchPhase.INDEX_SHARD_SEARCH_TASK_HANDLER_DOC_ID_STORE_PUT);
            docIdQueue.put(docId);
            info(() -> "Found " + localHitCount + " hits");
            LOGGER.trace("Collect called. {}, query term [{}]", this, query);
        } catch (final InterruptedException e) {
            info(() -> "Quitting...");
            LOGGER.debug("Quitting (interrupted). {}, query term [{}]", this, query);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
            throw new TaskTerminatedException();
        } catch (final RuntimeException e) {
            LOGGER.error("Error logging search progress: {}. {}", e.getMessage(), this, e);
        }

        // Add to the hit count.
        localHitCount.increment();
        totalHitCount.increment();
    }

    private void info(final Supplier<String> message) {
        taskContext.info(message);
        LOGGER.debug(message);
    }

    @Override
    public boolean needsScores() {
        return false;
    }

    @Override
    public String toString() {
        return "Query key: " + queryKey
                + ", shard: " + NullSafe.get(indexShard, IndexShard::getId)
                + ", shard hits: " + localHitCount.sum()
                + ", total hits: " + NullSafe.getOrElse(totalHitCount, LongAdder::sum, -1);
    }
}


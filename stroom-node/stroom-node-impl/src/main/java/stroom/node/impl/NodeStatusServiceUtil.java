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

package stroom.node.impl;

import stroom.node.api.NodeInfo;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataStoreFactory;
import stroom.pipeline.state.RecordCountService;
import stroom.query.common.v2.DataStoreFactory;
import stroom.statistics.api.InternalStatisticEvent;
import stroom.statistics.api.InternalStatisticKey;
import stroom.util.io.StreamUtil;

import com.google.common.collect.ImmutableMap;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class to split out the query of the node status in a separate read
 * only transaction so it queries the slave node.
 */
@Singleton
class NodeStatusServiceUtil {

    private final NodeInfo nodeInfo;
    private final RecordCountService recordCountService;
    private final RefDataStore refDataStore;
    private final DataStoreFactory dataStoreFactory;

    private long time = System.currentTimeMillis();

    private CPUStats previousCPUStats;

    @Inject
    NodeStatusServiceUtil(final NodeInfo nodeInfo,
                          final RecordCountService recordCountService,
                          final RefDataStoreFactory refDataStoreFactory,
                          final DataStoreFactory dataStoreFactory) {
        this.nodeInfo = nodeInfo;
        this.recordCountService = recordCountService;
        this.refDataStore = refDataStoreFactory.getOffHeapStore();
        this.dataStoreFactory = dataStoreFactory;
    }

    /**
     * Read the stats from a line.
     */
    static CPUStats createLinuxStats(String lines) {
        if (lines == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher(lines);
        CPUStats statLine = new CPUStats();
        matcher.find();
        statLine.user = Long.valueOf(matcher.group());
        matcher.find();
        statLine.nice = Long.valueOf(matcher.group());
        matcher.find();
        statLine.system = Long.valueOf(matcher.group());
        matcher.find();
        statLine.idle = Long.valueOf(matcher.group());
        matcher.find();
        statLine.ioWait = Long.valueOf(matcher.group());
        matcher.find();
        statLine.irq = Long.valueOf(matcher.group());
        matcher.find();
        statLine.softirq = Long.valueOf(matcher.group());

        int btimeStart = lines.indexOf("btime");
        matcher = pattern.matcher(lines.substring(btimeStart));
        matcher.find();

        return statLine;
    }

    /**
     * Linux dependent call.
     */
    protected String readSystemStatsInfo() {
        try {
            return StreamUtil.fileToString(Paths.get("/proc/stat"));
        } catch (final RuntimeException e) {
            return null;
        }
    }

    private InternalStatisticEvent buildStatisticEvent(final InternalStatisticKey key,
                                                       final long timeMs,
                                                       final Map<String, String> tags,
                                                       final String typeTagValue,
                                                       final double value) {
        // These stat events are being generated every minute so use a precision
        // of 60s
        final SortedMap<String, String> newTags = new TreeMap<>(tags);
        newTags.put("Type", typeTagValue);
        return InternalStatisticEvent.createValueStat(key, timeMs, newTags, value);
    }

    public List<InternalStatisticEvent> buildNodeStatus() {
        List<InternalStatisticEvent> statisticEventList = new ArrayList<>();

        Map<String, String> tags = ImmutableMap.of("Node", nodeInfo.getThisNodeName());

        final long nowEpochMs = System.currentTimeMillis();

        buildJavaMemoryStats(statisticEventList, tags, nowEpochMs);

        buildCpuStatEvents(statisticEventList, tags);

        buildOffHeapStoreSizeStats(statisticEventList, tags, nowEpochMs);

        return statisticEventList;
    }

    private void buildJavaMemoryStats(final List<InternalStatisticEvent> statisticEventList,
                                      final Map<String, String> tags,
                                      final long nowEpochMs) {
        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        final MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        final MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        final long heapUsed = heapUsage.getUsed();
        final long heapComitted = heapUsage.getCommitted();
        final long heapMax = heapUsage.getMax();
        final long nonHeapUsed = nonHeapUsage.getUsed();
        final long nonHeapComitted = nonHeapUsage.getCommitted();
        final long nonHeapMax = nonHeapUsage.getMax();

        if (heapUsed > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Heap Used",
                    heapUsed));
        }
        if (heapComitted > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Heap Committed",
                    heapComitted));
        }
        if (heapMax > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Heap Max",
                    heapMax));
        }
        if (nonHeapUsed > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Non Heap Used",
                    nonHeapUsed));
        }
        if (nonHeapComitted > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Non Heap Committed",
                    nonHeapComitted));
        }
        if (nonHeapMax > 0) {
            statisticEventList.add(buildStatisticEvent(InternalStatisticKey.MEMORY,
                    nowEpochMs,
                    tags,
                    "Non Heap Max",
                    nonHeapMax));
        }
    }

    private void buildOffHeapStoreSizeStats(final List<InternalStatisticEvent> statisticEventList,
                                            final Map<String, String> tags,
                                            final long nowEpochMs) {
        statisticEventList.add(buildStatisticEvent(InternalStatisticKey.OFF_HEAP_STORE_SIZE,
                nowEpochMs,
                tags,
                "Reference Data",
                refDataStore.getSizeOnDisk()));

        statisticEventList.add(buildStatisticEvent(InternalStatisticKey.OFF_HEAP_STORE_SIZE,
                nowEpochMs,
                tags,
                "Search Results",
                dataStoreFactory.getTotalSizeOnDisk()));
    }


    private void buildCpuStatEvents(final List<InternalStatisticEvent> statisticEventList,
                                    final Map<String, String> tags) {
        long now;
        // Get the current CPU stats.
        final CPUStats cpuStats = createLinuxStats(readSystemStatsInfo());

        // Get the elapsed time in seconds.
        now = System.currentTimeMillis();
        final double duration = (now - time) / 1000D;
        time = now;

        if (duration > 0) {
            // OS dependent code :(
            if (cpuStats != null && previousCPUStats != null) {
                final CPUStats diff = cpuStats.subtract(previousCPUStats);
                final double inc = 1000D / diff.getTotal();

                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "Idle (%)",
                        ((diff.idle * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "IO Wait (%)",
                        ((diff.ioWait * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "User (%)",
                        ((diff.user * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "Irq (%)",
                        ((diff.irq * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "Soft Irq (%)",
                        ((diff.softirq * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "System (%)",
                        ((diff.system * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "Nice (%)",
                        ((diff.nice * inc)) / 10F));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.CPU,
                        now,
                        tags,
                        "Total (%)",
                        ((diff.getTotal() * inc)) / 10F));
            }

            // Calculate the eps values.
            if (recordCountService != null) {
                final double read = recordCountService.getAndResetRead();
                final double written = recordCountService.getAndResetWritten();
                final long readEps = (long) (read / duration);
                final long writeEps = (long) (written / duration);

                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.EVENTS_PER_SECOND,
                        now,
                        tags,
                        "Read",
                        readEps));
                statisticEventList.add(buildStatisticEvent(InternalStatisticKey.EVENTS_PER_SECOND,
                        now,
                        tags,
                        "Write",
                        writeEps));
            }
        }

        previousCPUStats = cpuStats;
    }

    static class CPUStats {

        public Long user;
        public Long nice;
        public Long system;
        public Long idle;
        public Long ioWait;
        public Long irq;
        public Long softirq;

        CPUStats subtract(final CPUStats cpuStats) {
            CPUStats rtn = new CPUStats();
            rtn.user = this.user - cpuStats.user;
            rtn.nice = this.nice - cpuStats.nice;
            rtn.system = this.system - cpuStats.system;
            rtn.idle = this.idle - cpuStats.idle;
            rtn.ioWait = this.ioWait - cpuStats.ioWait;
            rtn.irq = this.irq - cpuStats.irq;
            rtn.softirq = this.softirq - cpuStats.softirq;
            return rtn;
        }

        Long getTotal() {
            return user + nice + system + idle + ioWait + irq + softirq;
        }
    }
}

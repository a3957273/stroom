package stroom.search.impl;

import stroom.query.common.v2.ResultStoreConfig;
import stroom.search.extraction.ExtractionConfig;
import stroom.search.impl.shard.IndexShardSearchConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
public class SearchConfig extends AbstractConfig implements IsStroomConfig {

    /**
     * We don't want to collect more than 10k doc's data into the queue by
     * default. When the queue is full the index shard data tasks will pause
     * until the docs are drained from the queue.
     */
    private static final int DEFAULT_MAX_STORED_DATA_QUEUE_SIZE = 1000;
    private static final int DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT = 1024;

    private final int maxStoredDataQueueSize;
    private final int maxBooleanClauseCount;
    private final String storeSize;
    private final ExtractionConfig extractionConfig;
    private final IndexShardSearchConfig shardConfig;
    private final ResultStoreConfig resultStoreConfig;

    public SearchConfig() {
        maxStoredDataQueueSize = DEFAULT_MAX_STORED_DATA_QUEUE_SIZE;
        maxBooleanClauseCount = DEFAULT_MAX_BOOLEAN_CLAUSE_COUNT;
        storeSize = "1000000,100,10,1";
        extractionConfig = new ExtractionConfig();
        shardConfig = new IndexShardSearchConfig();
        resultStoreConfig = new ResultStoreConfig();
    }

    @JsonCreator
    public SearchConfig(@JsonProperty("maxStoredDataQueueSize") final int maxStoredDataQueueSize,
                        @JsonProperty("maxBooleanClauseCount") final int maxBooleanClauseCount,
                        @JsonProperty("storeSize") final String storeSize,
                        @JsonProperty("extraction") final ExtractionConfig extractionConfig,
                        @JsonProperty("shard") final IndexShardSearchConfig shardConfig,
                        @JsonProperty("resultStore") final ResultStoreConfig resultStoreConfig) {
        this.maxStoredDataQueueSize = maxStoredDataQueueSize;
        this.maxBooleanClauseCount = maxBooleanClauseCount;
        this.storeSize = storeSize;
        this.extractionConfig = extractionConfig;
        this.shardConfig = shardConfig;
        this.resultStoreConfig = resultStoreConfig;
    }

    @JsonPropertyDescription("The maximum number documents that will have stored data retrieved from the index " +
            "shard and queued prior to further processing")
    public int getMaxStoredDataQueueSize() {
        return maxStoredDataQueueSize;
    }

    @JsonPropertyDescription("The maximum number of clauses that a boolean search can contain.")
    public int getMaxBooleanClauseCount() {
        return maxBooleanClauseCount;
    }

    @JsonPropertyDescription("The maximum number of search results to keep in memory at each level.")
    public String getStoreSize() {
        return storeSize;
    }

    @JsonProperty("extraction")
    public ExtractionConfig getExtractionConfig() {
        return extractionConfig;
    }

    @JsonProperty("shard")
    public IndexShardSearchConfig getShardConfig() {
        return shardConfig;
    }

    @JsonProperty("resultStore")
    public ResultStoreConfig getLmdbConfig() {
        return resultStoreConfig;
    }

    @Override
    public String toString() {
        return "SearchConfig{" +
                "maxStoredDataQueueSize=" + maxStoredDataQueueSize +
                ", maxBooleanClauseCount=" + maxBooleanClauseCount +
                ", storeSize='" + storeSize + '\'' +
                '}';
    }
}

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
 */

package stroom.search.elastic.shared;

import stroom.entity.shared.Action;
import stroom.util.shared.SharedString;

public class ElasticConnectionTestAction extends Action<SharedString> {
    private static final long serialVersionUID = 1L;

    private TestType testType;
    private ElasticCluster elasticCluster;
    private ElasticIndex elasticIndex;

    public ElasticConnectionTestAction() { }

    public ElasticConnectionTestAction(final ElasticCluster elasticCluster) {
        this.testType = TestType.CLUSTER;
        this.elasticCluster = elasticCluster;
    }

    public ElasticConnectionTestAction(final ElasticIndex elasticIndex) {
        this.testType = TestType.INDEX;
        this.elasticIndex = elasticIndex;
    }

    public TestType getTestType() { return testType; }
    public ElasticCluster getElasticCluster() { return elasticCluster; }
    public ElasticIndex getElasticIndex() {
        return elasticIndex;
    }

    @Override
    public String getTaskName() {
        return "Test Elasticsearch connection";
    }

    public enum TestType {
        CLUSTER,
        INDEX
    }
}
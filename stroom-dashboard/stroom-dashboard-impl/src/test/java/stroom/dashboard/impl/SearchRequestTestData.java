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

package stroom.dashboard.impl;

import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.Search;
import stroom.dashboard.shared.TableComponentSettings;
import stroom.dashboard.shared.TableResultRequest;
import stroom.docref.DocRef;
import stroom.query.api.v2.DateTimeFormatSettings;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Filter;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.Format.Type;
import stroom.query.api.v2.NumberFormatSettings;
import stroom.query.api.v2.ResultRequest.Fetch;
import stroom.query.api.v2.Sort;
import stroom.query.api.v2.TableSettings;
import stroom.query.api.v2.TimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchRequestTestData {
    static DashboardQueryKey dashboardQueryKey() {
        return new DashboardQueryKey(
                "queryKeyUuid",
                "0",
                "queryId-1");
    }

    static stroom.query.api.v2.SearchRequest apiSearchRequest() {
        stroom.dashboard.shared.SearchRequest dashboardSearchRequest = dashboardSearchRequest();

        SearchRequestMapper searchRequestMapper = new SearchRequestMapper(null);
        return searchRequestMapper.mapRequest(
                dashboardQueryKey(),
                dashboardSearchRequest);
    }

    static stroom.dashboard.shared.SearchRequest dashboardSearchRequest() {
        DocRef docRef = new DocRef("docRefType", "docRefUuid", "docRefName");

        ExpressionOperator.Builder expressionOperator = new ExpressionOperator.Builder(ExpressionOperator.Op.AND);
        expressionOperator.addTerm("field1", ExpressionTerm.Condition.EQUALS, "value1");
        expressionOperator.addOperator(new ExpressionOperator.Builder(ExpressionOperator.Op.AND).build());
        expressionOperator.addTerm("field2", ExpressionTerm.Condition.BETWEEN, "value2");

        final String componentId = "componentSettingsMapKey";
        TableComponentSettings tableSettings = new TableComponentSettings.Builder()
                .queryId("someQueryId")
                .addFields(new Field.Builder()
                        .id("1")
                        .name("name1")
                        .expression("expression1")
                        .sort(new Sort(1, Sort.SortDirection.ASCENDING))
                        .filter(new Filter("include1", "exclude1"))
                        .format(new Format.Builder()
                                .type(Format.Type.NUMBER)
                                .settings(new NumberFormatSettings(1, false))
                                .build())
                        .group(1)
                        .width(200)
                        .visible(true)
                        .special(false)
                        .build())
                .addFields(new Field.Builder()
                        .id("2")
                        .name("name2")
                        .expression("expression2")
                        .sort(new Sort(2, Sort.SortDirection.DESCENDING))
                        .filter(new Filter("include2", "exclude2"))
                        .format(new Format.Builder()
                                .type(Type.DATE_TIME)
                                .settings(createDateTimeFormat())
                                .build())
                        .group(2)
                        .width(200)
                        .visible(true)
                        .special(false)
                        .build())
                .extractValues(false)
                .extractionPipeline(
                        new DocRef("docRefType2", "docRefUuid2", "docRefName2"))
                .maxResults(List.of(1, 2))
                .showDetail(false)
                .build();

        Map<String, ComponentSettings> componentSettingsMap = new HashMap<>();
        componentSettingsMap.put(componentId, tableSettings);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("param1", "val1");
        paramMap.put("param2", "val2");

        final Search search = new Search.Builder()
                .dataSourceRef(docRef)
                .expression(expressionOperator.build())
                .componentSettingsMap(componentSettingsMap)
                .paramMap(paramMap)
                .incremental(true)
                .storeHistory(false)
                .build();

        final List<ComponentResultRequest> componentResultRequests = new ArrayList<>();
        for (final Map.Entry<String, ComponentSettings> entry : componentSettingsMap.entrySet()) {
            final TableComponentSettings tableComponentSettings = (TableComponentSettings) entry.getValue();
            final TableSettings ts = new TableComponentSettings.Builder(tableComponentSettings).buildTableSettings();
            TableResultRequest tableResultRequest = new TableResultRequest.Builder()
                    .componentId(entry.getKey())
                    .tableSettings(ts)
                    .fetch(Fetch.CHANGES)
                    .build();
            componentResultRequests.add(tableResultRequest);
        }

        return new stroom.dashboard.shared.SearchRequest(
                dashboardQueryKey(), search, componentResultRequests, "en-gb");
    }

    private static DateTimeFormatSettings createDateTimeFormat() {
        final TimeZone timeZone = TimeZone.fromOffset(2, 30);
        return new DateTimeFormatSettings("yyyy-MM-dd'T'HH:mm:ss", timeZone);
    }
}
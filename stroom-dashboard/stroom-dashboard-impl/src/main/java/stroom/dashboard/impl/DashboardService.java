package stroom.dashboard.impl;

import stroom.dashboard.shared.DashboardDoc;
import stroom.dashboard.shared.DashboardSearchRequest;
import stroom.dashboard.shared.DashboardSearchResponse;
import stroom.dashboard.shared.DownloadSearchResultsRequest;
import stroom.dashboard.shared.FunctionSignature;
import stroom.dashboard.shared.SearchKeepAliveRequest;
import stroom.dashboard.shared.ValidateExpressionResult;
import stroom.docref.DocRef;
import stroom.util.shared.ResourceGeneration;

import java.util.List;

public interface DashboardService {

    DashboardDoc read(DocRef docRef);

    DashboardDoc update(DashboardDoc doc);

    ValidateExpressionResult validateExpression(String expressionString);

    ResourceGeneration downloadQuery(DashboardSearchRequest request);

    ResourceGeneration downloadSearchResults(DownloadSearchResultsRequest request);

    Boolean keepAlive(SearchKeepAliveRequest request);

    DashboardSearchResponse search(DashboardSearchRequest request);

    List<String> fetchTimeZones();

    List<FunctionSignature> fetchFunctions();
}

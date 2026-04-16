package com.smartbi.service;

import com.smartbi.dto.QueryRequest;
import com.smartbi.dto.QueryResponse;

public interface QueryService {
    /**
     * Orchestrates full query lifecycle:
     * question -> SQL generation -> SQL validation -> DB query -> explanation.
     */
    QueryResponse handleQuery(QueryRequest request);
}

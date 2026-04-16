package com.smartbi.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class QueryResponse {
    /**
     * Generated SQL after safety validation/normalization.
     */
    private String sql;
    /**
     * Query result rows.
     */
    private List<Map<String, Object>> data;
    /**
     * Natural language explanation based on query result.
     */
    private String explanation;
}

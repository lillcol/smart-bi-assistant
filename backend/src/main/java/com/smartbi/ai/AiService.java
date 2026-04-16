package com.smartbi.ai;

import java.util.List;
import java.util.Map;

public interface AiService {
    /**
     * Calls SQL generation model (MiniMax) with a fully built prompt.
     */
    String generateSql(String prompt);

    /**
     * Calls explanation model (DeepSeek) using question + SQL + query result.
     */
    String explainResult(String question, String sql, List<Map<String, Object>> data);
}

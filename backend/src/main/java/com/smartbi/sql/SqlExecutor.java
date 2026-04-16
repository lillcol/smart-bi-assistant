package com.smartbi.sql;

import com.smartbi.common.exception.BusinessException;
import com.smartbi.config.QueryProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqlExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final QueryProperties queryProperties;

    public SqlExecutor(JdbcTemplate jdbcTemplate, QueryProperties queryProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryProperties = queryProperties;
    }

    /**
     * Executes validated SQL against current datasource.
     * <p>
     * - applies query timeout from configuration
     * - converts each row into Map<column, value>
     * - keeps column order stable via LinkedHashMap
     */
    public List<Map<String, Object>> execute(String sql) {
        try {
            return jdbcTemplate.query(
                    connection -> {
                        var ps = connection.prepareStatement(sql);
                        ps.setQueryTimeout(queryProperties.getTimeoutSeconds());
                        return ps;
                    },
                    (rs) -> {
                        List<Map<String, Object>> rows = new java.util.ArrayList<>();
                        var meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new java.util.LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(meta.getColumnLabel(i), rs.getObject(i));
                            }
                            rows.add(row);
                        }
                        return rows;
                    });
        } catch (Exception ex) {
            String preview = sql == null ? "" : sql.length() <= 600 ? sql : sql.substring(0, 600) + "...";
            throw new BusinessException("SQL execution failed. sql=\"" + preview + "\" error=\"" + ex.getMessage() + "\"");
        }
    }
}

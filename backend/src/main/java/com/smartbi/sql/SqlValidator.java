package com.smartbi.sql;

import com.smartbi.common.exception.BusinessException;
import com.smartbi.config.QueryProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\bfrom\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);

    private final QueryProperties queryProperties;

    public SqlValidator(QueryProperties queryProperties) {
        this.queryProperties = queryProperties;
    }

    /**
     * Validates generated SQL and applies safe normalization.
     * <p>
     * Enforced rules in MVP:
     * - only SELECT statements
     * - block common write DML/DDL keywords
     * - FROM table must be in optional whitelist
     * - LIMIT is mandatory and capped by max-limit
     *
     * @return safe SQL string ready for execution
     */
    public String validateAndNormalize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("generated SQL is empty");
        }
        String trimmed = sql.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        // Allow pure SELECT and CTE style query: WITH ... SELECT ...
        if (!(lower.startsWith("select") || lower.startsWith("with "))) {
            throw new BusinessException("only SELECT/CTE query is allowed");
        }
        if (lower.contains(" update ") || lower.contains(" delete ") || lower.contains(" insert ")
                || lower.contains(" drop ") || lower.contains(" alter ") || lower.contains(" truncate ")) {
            throw new BusinessException("write operation is forbidden");
        }

        validateTableWhitelist(lower);
        return ensureLimit(trimmed);
    }

    /**
     * Extracts first FROM table and validates against configured whitelist.
     * If whitelist is empty, this check is skipped.
     */
    private void validateTableWhitelist(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            throw new BusinessException("SQL must contain FROM clause");
        }
        String table = matcher.group(1);
        if (!queryProperties.getTableWhitelist().isEmpty() && !queryProperties.getTableWhitelist().contains(table)) {
            throw new BusinessException("table is not in whitelist: " + table);
        }
    }

    /**
     * Ensures SQL always includes LIMIT and never exceeds max-limit.
     */
    private String ensureLimit(String sql) {
        Matcher matcher = LIMIT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return sql + " LIMIT " + queryProperties.getDefaultLimit();
        }
        int limit = Integer.parseInt(matcher.group(1));
        if (limit > queryProperties.getMaxLimit()) {
            return matcher.replaceFirst("LIMIT " + queryProperties.getMaxLimit());
        }
        return sql;
    }
}

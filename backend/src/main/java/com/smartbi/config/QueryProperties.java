package com.smartbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "smartbi.query")
public class QueryProperties {
    /**
     * Default LIMIT appended when generated SQL has no explicit limit.
     */
    private int defaultLimit = 1000;
    /**
     * Hard upper bound for LIMIT to avoid oversized scans/returns.
     */
    private int maxLimit = 1000;
    /**
     * Database query timeout in seconds.
     */
    private int timeoutSeconds = 10;
    /**
     * Optional whitelist for FROM table names.
     * Empty list means no whitelist restriction.
     */
    private List<String> tableWhitelist = new ArrayList<>();
}

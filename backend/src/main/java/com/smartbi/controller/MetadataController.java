package com.smartbi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartbi.common.exception.BusinessException;
import com.smartbi.entity.MetricDefinition;
import com.smartbi.mapper.MetricDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final JdbcTemplate jdbcTemplate;
    private final MetricDefinitionMapper metricDefinitionMapper;

    @GetMapping("/databases")
    public List<String> databases() {
        return jdbcTemplate.queryForList(
                "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name",
                String.class
        );
    }

    @GetMapping("/tables")
    public List<String> tables(@RequestParam String database) {
        validateIdentifier(database);
        return jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? ORDER BY table_name",
                String.class,
                database
        );
    }

    @GetMapping("/table-schema")
    public List<Map<String, Object>> tableSchema(@RequestParam String database, @RequestParam String table) {
        validateIdentifier(database);
        validateIdentifier(table);
        return jdbcTemplate.queryForList(
                "SELECT column_name AS columnName, data_type AS dataType, column_comment AS columnDesc " +
                        "FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
                database, table
        );
    }

    @GetMapping("/metrics")
    public List<MetricDefinition> metrics() {
        return metricDefinitionMapper.selectList(new LambdaQueryWrapper<>());
    }

    @GetMapping("/preview")
    public List<Map<String, Object>> preview(
            @RequestParam String database,
            @RequestParam String table,
            @RequestParam(defaultValue = "10") int limit
    ) {
        validateIdentifier(database);
        validateIdentifier(table);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = "SELECT * FROM `" + database + "`.`" + table + "` LIMIT " + safeLimit;
        return jdbcTemplate.queryForList(sql);
    }

    private void validateIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_]+")) {
            throw new BusinessException("invalid identifier: " + value);
        }
    }
}

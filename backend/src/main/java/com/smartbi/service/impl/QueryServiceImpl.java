package com.smartbi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartbi.ai.AiService;
import com.smartbi.config.QueryProperties;
import com.smartbi.dto.QueryRequest;
import com.smartbi.dto.QueryResponse;
import com.smartbi.entity.MetricDefinition;
import com.smartbi.entity.TableSchema;
import com.smartbi.mapper.MetricDefinitionMapper;
import com.smartbi.mapper.TableSchemaMapper;
import com.smartbi.service.QueryService;
import com.smartbi.sql.PromptBuilder;
import com.smartbi.sql.SqlExecutor;
import com.smartbi.sql.SqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryServiceImpl implements QueryService {

    private final MetricDefinitionMapper metricDefinitionMapper;
    private final TableSchemaMapper tableSchemaMapper;
    private final AiService aiService;
    private final SqlValidator sqlValidator;
    private final SqlExecutor sqlExecutor;
    private final QueryProperties queryProperties;

    /**
     * Executes the MVP core pipeline:
     * 1) read semantic metadata from MySQL
     * 2) build prompt with schema + metrics
     * 3) call AI to generate SQL
     * 4) validate and normalize SQL for safety
     * 5) execute SQL against current datasource
     * 6) call AI to generate human-readable explanation
     */
    @Override
    public QueryResponse handleQuery(QueryRequest request) {
        long startMillis = System.currentTimeMillis();
        log.info("Start handleQuery. question={}", request.getQuestion());

        String schemaText = buildSchemaText();
        String metricsText = buildMetricText();
        log.info("Loaded semantic metadata. schemaChars={}, metricsChars={}", schemaText.length(), metricsText.length());

        String prompt = PromptBuilder.build(request.getQuestion(), schemaText, metricsText, queryProperties);
        log.info("Prompt built. promptChars={}", prompt.length());

        log.info("Calling AI (MiniMax) to generate SQL...");
        String generatedSql = aiService.generateSql(prompt);
        log.info("AI generated SQL. sqlChars={} ,generatedSql={}", generatedSql == null ? 0 : generatedSql.length(), generatedSql);

        String safeSql = sqlValidator.validateAndNormalize(generatedSql);
        log.info("SQL validated & normalized. safeSqlChars={}", safeSql == null ? 0 : safeSql.length());

        log.info("Executing SQL on datasource...");
        List<Map<String, Object>> data = sqlExecutor.execute(safeSql);
        log.info("SQL executed. rowCount={}", data == null ? 0 : data.size());

        log.info("Calling AI (DeepSeek) to generate explanation...");
        String explanation = aiService.explainResult(request.getQuestion(), safeSql, data);

        return QueryResponse.builder()
                .sql(safeSql)
                .data(data)
                .explanation(explanation)
                .build();
    }

    /**
     * Converts table/column metadata into compact prompt text.
     * Example line: order_info.gmv(total order amount)
     */
    private String buildSchemaText() {
        List<TableSchema> schemas = tableSchemaMapper.selectList(new LambdaQueryWrapper<>());
        log.info("Read table_schema rows={}", schemas == null ? 0 : schemas.size());
        return schemas.stream()
                .map(item -> "%s.%s(%s)".formatted(item.getTableName(), item.getColumnName(), item.getColumnDesc()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Converts metric definitions into compact prompt text.
     * Example line: gmv(Gross Merchandise Volume): total paid amount
     */
    private String buildMetricText() {
        List<MetricDefinition> metrics = metricDefinitionMapper.selectList(new LambdaQueryWrapper<>());
        log.info("Read metric_definition rows={}", metrics == null ? 0 : metrics.size());
        return metrics.stream()
                .map(item -> "%s(%s): %s".formatted(item.getMetricCode(), item.getMetricName(), item.getDescription()))
                .collect(Collectors.joining("\n"));
    }
}

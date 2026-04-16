package com.smartbi.ai.impl;

import com.smartbi.ai.AiService;
import com.smartbi.common.exception.BusinessException;
import com.smartbi.config.AiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MINIMAX_MAX_RETRIES = 3;

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public AiServiceImpl(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Sends prompt to MiniMax (Anthropic-compatible /v1/messages) and expects SQL text.
     */
    @Override
    public String generateSql(String prompt) {
        try {
            String sql = generateSqlByMiniMax(prompt);
            log.info("AI SQL used model=MiniMax. modelName={}", aiProperties.getMinimax().getModel());
            return sql;
        } catch (BusinessException ex) {
            log.warn("MiniMax generateSql failed, fallback to DeepSeek. deepseekModel={}, reason={}",
                    aiProperties.getDeepseek().getModel(), ex.getMessage());
            String sql = generateSqlByDeepSeek(prompt);
            log.info("AI SQL used model=DeepSeek. modelName={}", aiProperties.getDeepseek().getModel());
            return sql;
        }
    }

    /**
     * Sends compact data preview to DeepSeek and expects an explanation field.
     * Only first 10 rows are sent to control token usage.
     */
    @Override
    public String explainResult(String question, String sql, List<Map<String, Object>> data) {
        String prompt = String.format(
                "请基于以下信息输出简洁中文业务解释：%n问题：%s%nSQL：%s%n数据（最多10行）：%s%n",
                question, sql, data.stream().limit(10).toList()
        );
        try {
            String explanation = explainByMiniMax(prompt);
            log.info("AI Explanation used model=MiniMax. modelName={}", aiProperties.getMinimax().getModel());
            return explanation;
        } catch (BusinessException ex) {
            log.warn("MiniMax explainResult failed, fallback to DeepSeek. deepseekModel={}, reason={}",
                    aiProperties.getDeepseek().getModel(), ex.getMessage());
            String explanation = explainByDeepSeek(prompt);
            log.info("AI Explanation used model=DeepSeek. modelName={}", aiProperties.getDeepseek().getModel());
            return explanation;
        }
    }

    private String generateSqlByMiniMax(String prompt) {
        log.info("MiniMax generateSql start. model={}, promptChars={}", aiProperties.getMinimax().getModel(), prompt == null ? 0 : prompt.length());
        Map<String, Object> body = buildAnthropicMessagesBody(
                aiProperties.getMinimax().getModel(),
                prompt
        );
        Map<?, ?> response = postMinimaxAnthropic(
                aiProperties.getMinimax().getBaseUrl(),
                aiProperties.getMinimax().getApiKey(),
                body
        );
        String content = extractAnthropicContentText(response);
        if (content == null || content.isBlank()) {
            content = extractTextContent(response);
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("MiniMax response missing SQL content");
        }
        log.info("MiniMax generateSql done. contentChars={}", content.length());
        return normalizeSql(content);
    }

    private String generateSqlByDeepSeek(String prompt) {
        String deepseekEndpoint = resolveDeepSeekEndpoint(aiProperties.getDeepseek().getBaseUrl());
        log.info("DeepSeek generateSql start. model={}, endpoint={}, promptChars={}",
                aiProperties.getDeepseek().getModel(), deepseekEndpoint, prompt == null ? 0 : prompt.length());
        Map<String, Object> body = buildChatCompletionBody(aiProperties.getDeepseek().getModel(), prompt);
        Map<?, ?> response = post(deepseekEndpoint, aiProperties.getDeepseek().getApiKey(), body);
        String content = extractTextContent(response);
        if (content == null || content.isBlank()) {
            throw new BusinessException("DeepSeek response missing SQL content");
        }
        log.info("DeepSeek generateSql done. contentChars={}", content.length());
        return normalizeSql(content);
    }

    private String explainByMiniMax(String prompt) {
        log.info("MiniMax explainResult start. model={}, promptChars={}", aiProperties.getMinimax().getModel(), prompt == null ? 0 : prompt.length());
        Map<String, Object> body = buildAnthropicMessagesBody(
                aiProperties.getMinimax().getModel(),
                prompt
        );
        Map<?, ?> response = postMinimaxAnthropic(
                aiProperties.getMinimax().getBaseUrl(),
                aiProperties.getMinimax().getApiKey(),
                body
        );
        String explanation = extractAnthropicContentText(response);
        if (explanation == null || explanation.isBlank()) {
            explanation = extractTextContent(response);
        }
        if (explanation == null || explanation.isBlank()) {
            throw new BusinessException("MiniMax response missing explanation content");
        }
        log.info("MiniMax explainResult done. explanationChars={}", explanation.length());
        return explanation;
    }

    private String explainByDeepSeek(String prompt) {
        String deepseekEndpoint = resolveDeepSeekEndpoint(aiProperties.getDeepseek().getBaseUrl());
        log.info("DeepSeek explainResult start. model={}, endpoint={}, promptChars={}",
                aiProperties.getDeepseek().getModel(), deepseekEndpoint, prompt == null ? 0 : prompt.length());
        Map<String, Object> body = buildChatCompletionBody(aiProperties.getDeepseek().getModel(), prompt);
        Map<?, ?> response = post(deepseekEndpoint, aiProperties.getDeepseek().getApiKey(), body);
        String explanation = extractTextContent(response);
        if (explanation == null || explanation.isBlank()) {
            return "已返回数据，请结合SQL进行解读。";
        }
        log.info("DeepSeek explainResult done. explanationChars={}", explanation.length());
        return explanation;
    }

    @SuppressWarnings("unchecked")
    /**
     * Generic JSON POST helper for AI providers.
     * Uses Bearer token authentication and parses response into a map.
     */
    private Map<String, Object> post(String url, String apiKey, Map<String, Object> body) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return Objects.requireNonNullElseGet(response, HashMap::new);
        } catch (Exception ex) {
            throw new BusinessException("AI call failed: " + ex.getMessage());
        }
    }

    private Map<String, Object> postMinimaxAnthropic(String url, String apiKey, Map<String, Object> body) {
        int attempt = 0;
        long sleepMillis = 800L;
        while (true) {
            attempt++;
            try {
                log.info("MiniMax POST. url={}, version={}, model={}, attempt={}",
                        url, ANTHROPIC_VERSION, aiProperties.getMinimax().getModel(), attempt);
                Map<String, Object> response = restClient.post()
                        .uri(url)
                        // MiniMax Anthropic-compatible APIs use x-api-key and anthropic-version headers.
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", ANTHROPIC_VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                return Objects.requireNonNullElseGet(response, HashMap::new);
            } catch (RestClientResponseException ex) {
                if (isOverloaded529(ex) && attempt < MINIMAX_MAX_RETRIES) {
                    log.warn("MiniMax overloaded (529), retrying. attempt={}, sleepMs={}", attempt, sleepMillis);
                    sleepQuietly(sleepMillis);
                    sleepMillis *= 2;
                    continue;
                }
                throw new BusinessException("MiniMax call failed: " + ex.getRawStatusCode() + " : \"" + ex.getResponseBodyAsString() + "\"");
            } catch (Exception ex) {
                throw new BusinessException("MiniMax call failed: " + ex.getMessage());
            }
        }
    }

    private boolean isOverloaded529(RestClientResponseException ex) {
        if (ex.getRawStatusCode() == 529) {
            return true;
        }
        String body = ex.getResponseBodyAsString();
        return body != null && body.contains("overloaded_error");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveDeepSeekEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException("DeepSeek base-url is empty");
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/chat/completions";
    }

    private Map<String, Object> buildChatCompletionBody(String model, String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.1);
        return body;
    }

    private Map<String, Object> buildAnthropicMessagesBody(String model, String prompt) {
        // Based on: https://www.anthropic.com/ (Anthropic Messages API shape)
        // MiniMax supports the same message structure for /v1/messages.
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1000);
        body.put("system", "You are a helpful assistant.");
        body.put("messages", List.of(
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "text",
                                        "text", prompt
                                )
                        )
                )
        ));
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractTextContent(Map<?, ?> response) {
        // 1) OpenAI-compatible format: choices[0].message.content
        Object choicesObj = response.get("choices");
        if (choicesObj instanceof List<?>) {
            List<?> choices = (List<?>) choicesObj;
            if (choices.isEmpty()) {
                return legacyFieldFallback(response);
            }
            Object first = choices.get(0);
            if (first instanceof Map<?, ?>) {
                Map<?, ?> choiceMap = (Map<?, ?>) first;
                Object messageObj = choiceMap.get("message");
                if (messageObj instanceof Map<?, ?>) {
                    Map<?, ?> messageMap = (Map<?, ?>) messageObj;
                    Object content = messageMap.get("content");
                    if (content != null) {
                        return content.toString();
                    }
                }
                Object text = choiceMap.get("text");
                if (text != null) {
                    return text.toString();
                }
            }
        }
        return legacyFieldFallback(response);
    }

    private String extractAnthropicContentText(Map<?, ?> response) {
        Object contentObj = response.get("content");
        if (contentObj instanceof List<?> blocks) {
            StringBuilder sb = new StringBuilder();
            for (Object block : blocks) {
                if (!(block instanceof Map<?, ?> blockMap)) {
                    continue;
                }
                Object type = blockMap.get("type");
                if (!"text".equals(type)) {
                    continue;
                }
                Object text = blockMap.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return null;
    }

    // 2) Legacy flat fields fallback.
    private String legacyFieldFallback(Map<?, ?> response) {
        Object sql = response.get("sql");
        if (sql != null) {
            return sql.toString();
        }
        Object explanation = response.get("explanation");
        if (explanation != null) {
            return explanation.toString();
        }
        return null;
    }

    private String normalizeSql(String content) {
        if (content == null) {
            return "";
        }

        String trimmed = content.trim();
        // Remove any common markdown/code-fence noise.
        trimmed = trimmed.replace("```", "");
        String lower = trimmed.toLowerCase(Locale.ROOT).trim();

        // Keep CTE query as-is (WITH ... SELECT ...), only trim tail noise.
        if (lower.startsWith("with ")) {
            int semicolonIdx = trimmed.indexOf(';');
            if (semicolonIdx > 0) {
                trimmed = trimmed.substring(0, semicolonIdx).trim();
            }
            return trimmed;
        }

        // Extract from the first "select" (case-insensitive) to reduce mixed output.
        int selectIdx = lower.indexOf("select");
        if (selectIdx >= 0) {
            trimmed = trimmed.substring(selectIdx).trim();
        }

        // If there's additional noise after LIMIT, keep only up to LIMIT (best-effort).
        // This prevents "SELECT ... LIMIT 1000\nExplanation..." from breaking SQL grammar.
        java.util.regex.Pattern limitPattern = java.util.regex.Pattern.compile(
                "(?is)\\A\\s*(select\\b[\\s\\S]*?\\blimit\\s+\\d+\\b)");
        java.util.regex.Matcher m = limitPattern.matcher(trimmed);
        if (m.find()) {
            trimmed = m.group(1).trim();
        }

        // If multiple statements are present, keep only the first one.
        int semicolonIdx = trimmed.indexOf(';');
        if (semicolonIdx > 0) {
            trimmed = trimmed.substring(0, semicolonIdx).trim();
        }

        return trimmed.trim();
    }
}

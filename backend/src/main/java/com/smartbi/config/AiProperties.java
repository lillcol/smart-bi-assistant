package com.smartbi.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "smartbi.ai")
public class AiProperties {

    /**
     * SQL generation provider configuration (MiniMax).
     */
    private Provider minimax = new Provider();
    /**
     * Result explanation provider configuration (DeepSeek).
     */
    private Provider deepseek = new Provider();

    @Data
    public static class Provider {
        /**
         * Full HTTP endpoint URL for the provider.
         */
        @NotBlank
        private String baseUrl;
        /**
         * Sensitive credential, should only be configured in local/private profile.
         */
        @NotBlank
        private String apiKey;
        /**
         * Provider model name.
         */
        @NotBlank
        private String model;
    }
}

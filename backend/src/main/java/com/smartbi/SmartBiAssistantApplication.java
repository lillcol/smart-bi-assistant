package com.smartbi;

import com.smartbi.config.AiProperties;
import com.smartbi.config.QueryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AiProperties.class, QueryProperties.class})
public class SmartBiAssistantApplication {

    /**
     * Application bootstrap entry.
     * <p>
     * Starts a standard Spring Boot application and enables
     * binding for custom config objects:
     * - {@link AiProperties}: model endpoint/key/model-name settings
     * - {@link QueryProperties}: SQL safety and execution guardrail settings
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartBiAssistantApplication.class, args);
    }
}

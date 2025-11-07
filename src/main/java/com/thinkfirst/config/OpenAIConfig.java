package com.thinkfirst.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(prefix = "ai.openai", name = "enabled", havingValue = "true", matchIfMissing = false)
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(60));
    }
}


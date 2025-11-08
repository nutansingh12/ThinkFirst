package com.thinkfirst.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ai.openai", name = "enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnExpression("!'${ai.openai.api-key:}'.isEmpty()")
    public OpenAiService openAiService(@Value("${ai.openai.api-key}") String apiKey) {
        return new OpenAiService(apiKey, Duration.ofSeconds(60));
    }
}


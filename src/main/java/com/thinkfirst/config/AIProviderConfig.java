package com.thinkfirst.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIProviderConfig {
    
    private List<String> providerPriority;
    private GeminiConfig gemini;
    private GroqConfig groq;
    private OpenAIConfig openai;
    private ResilienceConfig resilience;
    
    @Data
    public static class GeminiConfig {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        private Map<String, String> models;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeoutSeconds;
    }
    
    @Data
    public static class GroqConfig {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        private Map<String, String> models;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeoutSeconds;
    }
    
    @Data
    public static class OpenAIConfig {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        private Map<String, String> models;
        private Integer maxTokens;
        private Double temperature;
        private Integer timeoutSeconds;
    }
    
    @Data
    public static class ResilienceConfig {
        private RetryConfig retry;
        private CircuitBreakerConfig circuitBreaker;
        
        @Data
        public static class RetryConfig {
            private Integer maxAttempts;
            private Long waitDuration;
        }
        
        @Data
        public static class CircuitBreakerConfig {
            private Integer failureRateThreshold;
            private Long waitDurationInOpenState;
            private Integer slidingWindowSize;
        }
    }
}


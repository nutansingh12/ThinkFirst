package com.thinkfirst.service.ai;

import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.exception.RateLimitException;
import com.thinkfirst.model.Question;
import com.thinkfirst.service.cache.AICacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hybrid AI Provider Service that manages multiple AI providers with fallback logic.
 * Priority: Gemini (free) → Groq (free) → OpenAI (paid)
 */
@Service
public class AIProviderService {

    private static final Logger log = LoggerFactory.getLogger(AIProviderService.class);
    
    private final Map<String, AIProvider> providers;
    private final AIProviderConfig config;
    private final GeminiService geminiService;
    private final GroqService groqService;
    private final OpenAIProviderService openAIService;
    private final AICacheService cacheService;

    public AIProviderService(
            GeminiService geminiService,
            GroqService groqService,
            OpenAIProviderService openAIService,
            AIProviderConfig config,
            AICacheService cacheService
    ) {
        this.geminiService = geminiService;
        this.groqService = groqService;
        this.openAIService = openAIService;
        this.config = config;
        this.cacheService = cacheService;

        // Register all providers
        this.providers = new HashMap<>();
        providers.put("gemini", geminiService);
        providers.put("groq", groqService);
        providers.put("openai", openAIService);
    }
    
    /**
     * Generate educational response with automatic fallback and caching
     */
    public String generateEducationalResponse(String query, int age, String subject) {
        // Try cache first
        Optional<String> cached = cacheService.getCachedResponse(query, age, subject);
        if (cached.isPresent()) {
            log.info("Using cached response for query (saved API call)");
            return cached.get();
        }

        // Cache miss - call AI provider
        String response = executeWithFallback(
            provider -> provider.generateEducationalResponse(query, age, subject),
            "generateEducationalResponse"
        );

        // Cache the response
        cacheService.cacheResponse(query, age, subject, response);

        return response;
    }
    
    /**
     * Generate quiz questions with automatic fallback and caching
     */
    public List<Question> generateQuestions(String topic, String subject, int count, String difficulty) {
        // Try cache first
        Optional<List<Question>> cached = cacheService.getCachedQuiz(topic, subject, count, difficulty);
        if (cached.isPresent()) {
            log.info("Using cached quiz for topic: {} (saved API call)", topic);
            return cached.get();
        }

        // Cache miss - call AI provider
        List<Question> questions = executeWithFallback(
            provider -> provider.generateQuestions(topic, subject, count, difficulty),
            "generateQuestions"
        );

        // Cache the questions
        cacheService.cacheQuiz(topic, subject, count, difficulty, questions);

        return questions;
    }
    
    /**
     * Generate hint with automatic fallback and caching
     */
    public String generateHint(String query, String subject, int age) {
        // Try cache first
        Optional<String> cached = cacheService.getCachedHint(query, subject, age);
        if (cached.isPresent()) {
            log.info("Using cached hint (saved API call)");
            return cached.get();
        }

        // Cache miss - call AI provider
        String hint = executeWithFallback(
            provider -> provider.generateHint(query, subject, age),
            "generateHint"
        );

        // Cache the hint
        cacheService.cacheHint(query, subject, age, hint);

        return hint;
    }
    
    /**
     * Analyze query subject with automatic fallback and caching
     */
    public String analyzeQuerySubject(String query) {
        // Try cache first
        Optional<String> cached = cacheService.getCachedSubject(query);
        if (cached.isPresent()) {
            log.info("Using cached subject classification (saved API call)");
            return cached.get();
        }

        // Cache miss - call AI provider
        String subject = executeWithFallback(
            provider -> provider.analyzeQuerySubject(query),
            "analyzeQuerySubject"
        );

        // Cache the subject
        cacheService.cacheSubject(query, subject);

        return subject;
    }
    
    /**
     * Execute operation with automatic provider fallback
     */
    private <T> T executeWithFallback(ProviderOperation<T> operation, String operationName) {
        List<String> providerPriority = config.getProviderPriority();
        
        Exception lastException = null;
        
        for (String providerName : providerPriority) {
            AIProvider provider = providers.get(providerName.toLowerCase());
            
            if (provider == null) {
                log.warn("Provider '{}' not found in registry", providerName);
                continue;
            }
            
            if (!provider.isAvailable()) {
                log.debug("Provider '{}' is not available, trying next", providerName);
                continue;
            }
            
            try {
                log.info("Attempting {} with provider: {}", operationName, provider.getProviderName());
                T result = operation.execute(provider);
                log.info("Successfully executed {} with provider: {}", operationName, provider.getProviderName());
                return result;
                
            } catch (RateLimitException e) {
                log.warn("Rate limit exceeded for provider '{}': {}", provider.getProviderName(), e.getMessage());
                lastException = e;
                // Continue to next provider
                
            } catch (AIProviderException e) {
                log.error("Error with provider '{}': {}", provider.getProviderName(), e.getMessage());
                lastException = e;
                // Continue to next provider
                
            } catch (Exception e) {
                log.error("Unexpected error with provider '{}': {}", provider.getProviderName(), e.getMessage(), e);
                lastException = e;
                // Continue to next provider
            }
        }
        
        // All providers failed
        String errorMessage = String.format(
            "All AI providers failed for operation '%s'. Last error: %s",
            operationName,
            lastException != null ? lastException.getMessage() : "Unknown error"
        );
        log.error(errorMessage);
        throw new AIProviderException("ALL_PROVIDERS", errorMessage, lastException);
    }
    
    /**
     * Get the status of all providers
     */
    public Map<String, ProviderStatus> getProviderStatus() {
        Map<String, ProviderStatus> status = new HashMap<>();
        
        for (Map.Entry<String, AIProvider> entry : providers.entrySet()) {
            AIProvider provider = entry.getValue();
            status.put(entry.getKey(), new ProviderStatus(
                provider.getProviderName(),
                provider.isAvailable(),
                entry.getKey()
            ));
        }
        
        return status;
    }
    
    /**
     * Manually set OpenAI model (for flexibility)
     */
    public void setOpenAIModel(String modelKey) {
        openAIService.setModel(modelKey);
    }
    
    /**
     * Get current OpenAI model
     */
    public String getCurrentOpenAIModel() {
        return openAIService.getCurrentModel();
    }
    
    /**
     * Test a specific provider
     */
    public boolean testProvider(String providerName) {
        AIProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            log.warn("Provider '{}' not found", providerName);
            return false;
        }

        try {
            String testResponse = provider.analyzeQuerySubject("What is 2+2?");
            log.info("Provider '{}' test successful: {}", providerName, testResponse);
            return true;
        } catch (Exception e) {
            log.error("Provider '{}' test failed: {}", providerName, e.getMessage());
            return false;
        }
    }

    /**
     * Get cache statistics
     */
    public AICacheService.CacheStats getCacheStats() {
        return cacheService.getCacheStats();
    }

    /**
     * Invalidate all quiz caches
     */
    public void invalidateQuizCache() {
        cacheService.invalidateAllQuizzes();
    }

    /**
     * Invalidate all response caches
     */
    public void invalidateResponseCache() {
        cacheService.invalidateAllResponses();
    }
    
    /**
     * Functional interface for provider operations
     */
    @FunctionalInterface
    private interface ProviderOperation<T> {
        T execute(AIProvider provider);
    }
    
    /**
     * Provider status DTO
     */
    public static class ProviderStatus {
        public final String name;
        public final boolean available;
        public final String key;
        
        public ProviderStatus(String name, boolean available, String key) {
            this.name = name;
            this.available = available;
            this.key = key;
        }
    }
}


package com.thinkfirst.service.ai;

import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Smart retry strategy with exponential backoff
 * Optimizes API usage by intelligently retrying failed requests
 */
@Slf4j
@Component
public class RetryStrategy {
    
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(500);
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);
    
    /**
     * Execute operation with exponential backoff retry
     * 
     * @param operation The operation to execute
     * @param operationName Name for logging
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;
        Duration backoff = INITIAL_BACKOFF;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} for {}", attempt, operationName);
                }
                
                return operation.get();
                
            } catch (RateLimitException e) {
                // Don't retry rate limits - let fallback handle it
                log.warn("Rate limit hit for {}, not retrying", operationName);
                throw e;
                
            } catch (AIProviderException e) {
                lastException = e;
                attempt++;
                
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries ({}) reached for {}", MAX_RETRIES, operationName);
                    throw e;
                }
                
                // Check if error is retryable
                if (!isRetryable(e)) {
                    log.warn("Non-retryable error for {}: {}", operationName, e.getMessage());
                    throw e;
                }
                
                // Wait with exponential backoff
                try {
                    log.info("Backing off for {} ms before retry {}", backoff.toMillis(), attempt);
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AIProviderException("Retry", "Retry interrupted", ie);
                }
                
                // Increase backoff for next retry
                backoff = Duration.ofMillis(
                    Math.min(
                        (long) (backoff.toMillis() * BACKOFF_MULTIPLIER),
                        MAX_BACKOFF.toMillis()
                    )
                );
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries ({}) reached for {} with unexpected error", MAX_RETRIES, operationName);
                    throw new AIProviderException("Retry", "Unexpected error after retries", e);
                }
                
                // Wait with exponential backoff
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AIProviderException("Retry", "Retry interrupted", ie);
                }
                
                backoff = Duration.ofMillis(
                    Math.min(
                        (long) (backoff.toMillis() * BACKOFF_MULTIPLIER),
                        MAX_BACKOFF.toMillis()
                    )
                );
            }
        }
        
        // Should never reach here, but just in case
        throw new AIProviderException("Retry", "Max retries exceeded", lastException);
    }
    
    /**
     * Determine if an error is retryable
     */
    private boolean isRetryable(AIProviderException e) {
        String message = e.getMessage().toLowerCase();
        
        // Retryable errors
        if (message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("network") ||
            message.contains("503") ||
            message.contains("502") ||
            message.contains("500")) {
            return true;
        }
        
        // Non-retryable errors
        if (message.contains("401") ||  // Unauthorized
            message.contains("403") ||  // Forbidden
            message.contains("400") ||  // Bad request
            message.contains("invalid api key") ||
            message.contains("authentication")) {
            return false;
        }
        
        // Default: retry
        return true;
    }
    
    /**
     * Execute with simple retry (no backoff) for quick operations
     */
    public <T> T executeWithSimpleRetry(Supplier<T> operation, String operationName, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (RateLimitException e) {
                throw e; // Don't retry rate limits
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt >= maxRetries) {
                    log.error("Max retries ({}) reached for {}", maxRetries, operationName);
                    throw new AIProviderException("Retry", "Max retries exceeded", e);
                }
                
                log.debug("Quick retry {} for {}", attempt, operationName);
            }
        }
        
        throw new AIProviderException("Retry", "Max retries exceeded", lastException);
    }
}


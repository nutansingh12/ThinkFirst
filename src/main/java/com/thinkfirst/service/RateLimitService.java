package com.thinkfirst.service;

import com.thinkfirst.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting service
 * Implements per-user rate limits for API endpoints
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Rate limit configurations
    private static final int CHAT_REQUESTS_PER_HOUR = 100;
    private static final int QUIZ_SUBMISSIONS_PER_HOUR = 10;
    private static final int AUTH_ATTEMPTS_PER_HOUR = 5;
    private static final int DAILY_QUESTIONS_LIMIT = 50;
    
    /**
     * Check if chat request is within rate limit
     * @param childId The child ID
     * @throws RateLimitException if rate limit exceeded
     */
    public void checkChatRateLimit(Long childId) {
        String key = "rate_limit:chat:" + childId;
        checkRateLimit(key, CHAT_REQUESTS_PER_HOUR, Duration.ofHours(1), "Chat requests");
    }
    
    /**
     * Check if quiz submission is within rate limit
     * @param childId The child ID
     * @throws RateLimitException if rate limit exceeded
     */
    public void checkQuizRateLimit(Long childId) {
        String key = "rate_limit:quiz:" + childId;
        checkRateLimit(key, QUIZ_SUBMISSIONS_PER_HOUR, Duration.ofHours(1), "Quiz submissions");
    }
    
    /**
     * Check if authentication attempt is within rate limit
     * @param identifier Email or username
     * @throws RateLimitException if rate limit exceeded
     */
    public void checkAuthRateLimit(String identifier) {
        String key = "rate_limit:auth:" + identifier;
        checkRateLimit(key, AUTH_ATTEMPTS_PER_HOUR, Duration.ofHours(1), "Authentication attempts");
    }
    
    /**
     * Check daily question limit
     * @param childId The child ID
     * @throws RateLimitException if daily limit exceeded
     */
    public void checkDailyQuestionLimit(Long childId) {
        String key = "rate_limit:daily_questions:" + childId;
        checkRateLimit(key, DAILY_QUESTIONS_LIMIT, Duration.ofDays(1), "Daily questions");
    }
    
    /**
     * Generic rate limit checker
     * @param key Redis key for the rate limit counter
     * @param maxRequests Maximum number of requests allowed
     * @param duration Time window for the rate limit
     * @param limitType Description of the limit type for error messages
     * @throws RateLimitException if rate limit exceeded
     */
    private void checkRateLimit(String key, int maxRequests, Duration duration, String limitType) {
        try {
            // Get current count
            Object countObj = redisTemplate.opsForValue().get(key);
            int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
            
            if (currentCount >= maxRequests) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                String message = String.format(
                    "%s rate limit exceeded. Maximum %d requests per %s. Try again in %d seconds.",
                    limitType, maxRequests, formatDuration(duration), ttl != null ? ttl : 0
                );
                log.warn("Rate limit exceeded for key: {}", key);
                throw new RateLimitException("RateLimit", message);
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            
            // Set expiration if this is the first request
            if (currentCount == 0) {
                redisTemplate.expire(key, duration);
            }
            
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fail open - if Redis is down, allow the request
        }
    }
    
    /**
     * Reset rate limit for a specific key
     * @param childId The child ID
     * @param limitType Type of limit to reset (chat, quiz, daily_questions)
     */
    public void resetRateLimit(Long childId, String limitType) {
        String key = "rate_limit:" + limitType + ":" + childId;
        redisTemplate.delete(key);
        log.info("Rate limit reset for key: {}", key);
    }
    
    /**
     * Get remaining requests for a specific limit
     * @param childId The child ID
     * @param limitType Type of limit (chat, quiz, daily_questions)
     * @return Number of remaining requests
     */
    public int getRemainingRequests(Long childId, String limitType) {
        String key = "rate_limit:" + limitType + ":" + childId;
        int maxRequests = getMaxRequests(limitType);
        
        try {
            Object countObj = redisTemplate.opsForValue().get(key);
            int currentCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;
            return Math.max(0, maxRequests - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining requests for key: {}", key, e);
            return maxRequests; // Return max if error
        }
    }
    
    /**
     * Get time until rate limit resets
     * @param childId The child ID
     * @param limitType Type of limit (chat, quiz, daily_questions)
     * @return Seconds until reset, or 0 if no limit active
     */
    public long getTimeUntilReset(Long childId, String limitType) {
        String key = "rate_limit:" + limitType + ":" + childId;
        
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Get max requests for a limit type
     */
    private int getMaxRequests(String limitType) {
        return switch (limitType) {
            case "chat" -> CHAT_REQUESTS_PER_HOUR;
            case "quiz" -> QUIZ_SUBMISSIONS_PER_HOUR;
            case "daily_questions" -> DAILY_QUESTIONS_LIMIT;
            default -> 100;
        };
    }
    
    /**
     * Format duration for user-friendly messages
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
        return duration.getSeconds() + " seconds";
    }
}


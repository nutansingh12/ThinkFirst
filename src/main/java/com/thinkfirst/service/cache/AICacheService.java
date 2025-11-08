package com.thinkfirst.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * AI Response Caching Service
 * Caches quiz questions and educational responses to minimize AI API calls
 * Target: 70-80% cache hit rate
 */
@Service
public class AICacheService {

    private static final Logger log = LoggerFactory.getLogger(AICacheService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public AICacheService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    // Cache TTL configurations
    private static final Duration QUIZ_CACHE_TTL = Duration.ofDays(30); // Quizzes rarely change
    private static final Duration RESPONSE_CACHE_TTL = Duration.ofDays(7); // Responses can be reused
    private static final Duration HINT_CACHE_TTL = Duration.ofDays(7);
    private static final Duration SUBJECT_CACHE_TTL = Duration.ofDays(30); // Subject classification is stable
    
    // Cache key prefixes
    private static final String QUIZ_PREFIX = "quiz:";
    private static final String RESPONSE_PREFIX = "response:";
    private static final String HINT_PREFIX = "hint:";
    private static final String SUBJECT_PREFIX = "subject:";
    
    /**
     * Cache quiz questions
     * Key: topic + subject + count + difficulty
     */
    public void cacheQuiz(String topic, String subject, int count, String difficulty, List<Question> questions) {
        try {
            String cacheKey = generateQuizCacheKey(topic, subject, count, difficulty);
            String jsonValue = objectMapper.writeValueAsString(questions);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, QUIZ_CACHE_TTL);
            log.info("Cached quiz: {} (TTL: {} days)", cacheKey, QUIZ_CACHE_TTL.toDays());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache quiz: {}", e.getMessage());
        }
    }
    
    /**
     * Get cached quiz questions
     */
    public Optional<List<Question>> getCachedQuiz(String topic, String subject, int count, String difficulty) {
        try {
            String cacheKey = generateQuizCacheKey(topic, subject, count, difficulty);
            String jsonValue = redisTemplate.opsForValue().get(cacheKey);
            
            if (jsonValue != null) {
                List<Question> questions = objectMapper.readValue(
                    jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Question.class)
                );
                log.info("Cache HIT: {} ({} questions)", cacheKey, questions.size());
                return Optional.of(questions);
            }
            
            log.debug("Cache MISS: {}", cacheKey);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to retrieve cached quiz: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Cache educational response
     * Key: query + age + subject
     */
    public void cacheResponse(String query, int age, String subject, String response) {
        try {
            String cacheKey = generateResponseCacheKey(query, age, subject);
            redisTemplate.opsForValue().set(cacheKey, response, RESPONSE_CACHE_TTL);
            log.info("Cached response: {} (TTL: {} days)", cacheKey, RESPONSE_CACHE_TTL.toDays());
        } catch (Exception e) {
            log.error("Failed to cache response: {}", e.getMessage());
        }
    }
    
    /**
     * Get cached educational response
     */
    public Optional<String> getCachedResponse(String query, int age, String subject) {
        try {
            String cacheKey = generateResponseCacheKey(query, age, subject);
            String response = redisTemplate.opsForValue().get(cacheKey);
            
            if (response != null) {
                log.info("Cache HIT: {}", cacheKey);
                return Optional.of(response);
            }
            
            log.debug("Cache MISS: {}", cacheKey);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to retrieve cached response: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Cache hint
     * Key: query + subject + age
     */
    public void cacheHint(String query, String subject, int age, String hint) {
        try {
            String cacheKey = generateHintCacheKey(query, subject, age);
            redisTemplate.opsForValue().set(cacheKey, hint, HINT_CACHE_TTL);
            log.info("Cached hint: {} (TTL: {} days)", cacheKey, HINT_CACHE_TTL.toDays());
        } catch (Exception e) {
            log.error("Failed to cache hint: {}", e.getMessage());
        }
    }
    
    /**
     * Get cached hint
     */
    public Optional<String> getCachedHint(String query, String subject, int age) {
        try {
            String cacheKey = generateHintCacheKey(query, subject, age);
            String hint = redisTemplate.opsForValue().get(cacheKey);
            
            if (hint != null) {
                log.info("Cache HIT: {}", cacheKey);
                return Optional.of(hint);
            }
            
            log.debug("Cache MISS: {}", cacheKey);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to retrieve cached hint: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Cache subject classification
     * Key: query (normalized)
     */
    public void cacheSubject(String query, String subject) {
        try {
            String cacheKey = generateSubjectCacheKey(query);
            redisTemplate.opsForValue().set(cacheKey, subject, SUBJECT_CACHE_TTL);
            log.info("Cached subject: {} -> {} (TTL: {} days)", cacheKey, subject, SUBJECT_CACHE_TTL.toDays());
        } catch (Exception e) {
            log.error("Failed to cache subject: {}", e.getMessage());
        }
    }
    
    /**
     * Get cached subject classification
     */
    public Optional<String> getCachedSubject(String query) {
        try {
            String cacheKey = generateSubjectCacheKey(query);
            String subject = redisTemplate.opsForValue().get(cacheKey);
            
            if (subject != null) {
                log.info("Cache HIT: {} -> {}", cacheKey, subject);
                return Optional.of(subject);
            }
            
            log.debug("Cache MISS: {}", cacheKey);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to retrieve cached subject: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Invalidate all quiz caches (use when quiz generation logic changes)
     */
    public void invalidateAllQuizzes() {
        try {
            redisTemplate.keys(QUIZ_PREFIX + "*").forEach(redisTemplate::delete);
            log.info("Invalidated all quiz caches");
        } catch (Exception e) {
            log.error("Failed to invalidate quiz caches: {}", e.getMessage());
        }
    }
    
    /**
     * Invalidate all response caches
     */
    public void invalidateAllResponses() {
        try {
            redisTemplate.keys(RESPONSE_PREFIX + "*").forEach(redisTemplate::delete);
            log.info("Invalidated all response caches");
        } catch (Exception e) {
            log.error("Failed to invalidate response caches: {}", e.getMessage());
        }
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        try {
            long quizCount = redisTemplate.keys(QUIZ_PREFIX + "*").size();
            long responseCount = redisTemplate.keys(RESPONSE_PREFIX + "*").size();
            long hintCount = redisTemplate.keys(HINT_PREFIX + "*").size();
            long subjectCount = redisTemplate.keys(SUBJECT_PREFIX + "*").size();
            
            return new CacheStats(quizCount, responseCount, hintCount, subjectCount);
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            return new CacheStats(0, 0, 0, 0);
        }
    }
    
    // ==================== Cache Key Generation ====================
    
    private String generateQuizCacheKey(String topic, String subject, int count, String difficulty) {
        String normalized = normalizeText(topic) + ":" + normalizeText(subject) + ":" + count + ":" + difficulty.toLowerCase();
        return QUIZ_PREFIX + hashKey(normalized);
    }
    
    private String generateResponseCacheKey(String query, int age, String subject) {
        String normalized = normalizeText(query) + ":" + age + ":" + normalizeText(subject);
        return RESPONSE_PREFIX + hashKey(normalized);
    }
    
    private String generateHintCacheKey(String query, String subject, int age) {
        String normalized = normalizeText(query) + ":" + normalizeText(subject) + ":" + age;
        return HINT_PREFIX + hashKey(normalized);
    }
    
    private String generateSubjectCacheKey(String query) {
        String normalized = normalizeText(query);
        return SUBJECT_PREFIX + hashKey(normalized);
    }
    
    /**
     * Normalize text for consistent cache keys
     * - Convert to lowercase
     * - Remove extra whitespace
     * - Trim
     */
    private String normalizeText(String text) {
        return text.toLowerCase().trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Hash key using SHA-256 for consistent, compact cache keys
     */
    private String hashKey(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16); // Use first 16 chars for shorter keys
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available, using hashCode", e);
            return String.valueOf(input.hashCode());
        }
    }
    
    /**
     * Cache statistics DTO
     */
    public record CacheStats(
        long quizCount,
        long responseCount,
        long hintCount,
        long subjectCount
    ) {
        public long totalCount() {
            return quizCount + responseCount + hintCount + subjectCount;
        }
    }
}


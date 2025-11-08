package com.thinkfirst.service;

import com.thinkfirst.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RateLimitService
 * Tests Redis-based rate limiting for different operations
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // Chat Rate Limit Tests (100 requests/hour)

    @Test
    void testChatRateLimit_FirstRequest_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert - Should not throw exception
        rateLimitService.checkChatRateLimit(childId);

        // Verify
        verify(valueOperations).increment("rate_limit:chat:" + childId);
        verify(redisTemplate).expire(eq("rate_limit:chat:" + childId), eq(Duration.ofHours(1)));
    }

    @Test
    void testChatRateLimit_WithinLimit_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(50L); // 50th request

        // Act & Assert - Should not throw exception
        rateLimitService.checkChatRateLimit(childId);

        // Verify
        verify(valueOperations).increment("rate_limit:chat:" + childId);
    }

    @Test
    void testChatRateLimit_AtLimit_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(100L); // 100th request

        // Act & Assert - Should not throw exception
        rateLimitService.checkChatRateLimit(childId);
    }

    @Test
    void testChatRateLimit_ExceedsLimit_ShouldThrowException() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(101L); // 101st request

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkChatRateLimit(childId))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Chat rate limit exceeded");
    }

    // Quiz Rate Limit Tests (10 requests/hour)

    @Test
    void testQuizRateLimit_FirstRequest_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        rateLimitService.checkQuizRateLimit(childId);

        // Verify
        verify(valueOperations).increment("rate_limit:quiz:" + childId);
        verify(redisTemplate).expire(eq("rate_limit:quiz:" + childId), eq(Duration.ofHours(1)));
    }

    @Test
    void testQuizRateLimit_WithinLimit_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // Act & Assert
        rateLimitService.checkQuizRateLimit(childId);
    }

    @Test
    void testQuizRateLimit_ExceedsLimit_ShouldThrowException() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(11L);

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkQuizRateLimit(childId))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Quiz rate limit exceeded");
    }

    // Auth Rate Limit Tests (5 requests/hour per IP)

    @Test
    void testAuthRateLimit_FirstRequest_ShouldAllow() {
        // Arrange
        String ipAddress = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        rateLimitService.checkAuthRateLimit(ipAddress);

        // Verify
        verify(valueOperations).increment("rate_limit:auth:" + ipAddress);
        verify(redisTemplate).expire(eq("rate_limit:auth:" + ipAddress), eq(Duration.ofHours(1)));
    }

    @Test
    void testAuthRateLimit_WithinLimit_ShouldAllow() {
        // Arrange
        String ipAddress = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act & Assert
        rateLimitService.checkAuthRateLimit(ipAddress);
    }

    @Test
    void testAuthRateLimit_ExceedsLimit_ShouldThrowException() {
        // Arrange
        String ipAddress = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(6L);

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkAuthRateLimit(ipAddress))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Authentication rate limit exceeded");
    }

    // Daily Question Limit Tests (50 requests/day)

    @Test
    void testDailyQuestionLimit_FirstRequest_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        rateLimitService.checkDailyQuestionLimit(childId);

        // Verify
        verify(valueOperations).increment("rate_limit:daily:" + childId);
        verify(redisTemplate).expire(eq("rate_limit:daily:" + childId), eq(Duration.ofDays(1)));
    }

    @Test
    void testDailyQuestionLimit_WithinLimit_ShouldAllow() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(25L);

        // Act & Assert
        rateLimitService.checkDailyQuestionLimit(childId);
    }

    @Test
    void testDailyQuestionLimit_ExceedsLimit_ShouldThrowException() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(51L);

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkDailyQuestionLimit(childId))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Daily question limit exceeded");
    }

    // TTL Expiration Tests

    @Test
    void testChatRateLimit_ExistingKey_ShouldNotSetExpiration() {
        // Arrange
        Long childId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(5L); // Not first request

        // Act
        rateLimitService.checkChatRateLimit(childId);

        // Verify - expire should not be called for existing keys
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    // Multiple Children Tests

    @Test
    void testChatRateLimit_DifferentChildren_ShouldHaveSeparateLimits() {
        // Arrange
        Long childId1 = 1L;
        Long childId2 = 2L;
        when(valueOperations.increment("rate_limit:chat:" + childId1)).thenReturn(1L);
        when(valueOperations.increment("rate_limit:chat:" + childId2)).thenReturn(1L);

        // Act
        rateLimitService.checkChatRateLimit(childId1);
        rateLimitService.checkChatRateLimit(childId2);

        // Verify - Both should be allowed with separate counters
        verify(valueOperations).increment("rate_limit:chat:" + childId1);
        verify(valueOperations).increment("rate_limit:chat:" + childId2);
    }

    // Edge Cases

    @Test
    void testChatRateLimit_NullChildId_ShouldHandleGracefully() {
        // Arrange
        Long childId = null;

        // Act & Assert - Should handle null gracefully
        assertThatThrownBy(() -> rateLimitService.checkChatRateLimit(childId))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAuthRateLimit_DifferentIPs_ShouldHaveSeparateLimits() {
        // Arrange
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";
        when(valueOperations.increment("rate_limit:auth:" + ip1)).thenReturn(1L);
        when(valueOperations.increment("rate_limit:auth:" + ip2)).thenReturn(1L);

        // Act
        rateLimitService.checkAuthRateLimit(ip1);
        rateLimitService.checkAuthRateLimit(ip2);

        // Verify
        verify(valueOperations).increment("rate_limit:auth:" + ip1);
        verify(valueOperations).increment("rate_limit:auth:" + ip2);
    }
}


package com.siddharth.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void testAllowRequestWithinLimit() {
        Long userId = 1L;

        // Should allow 60 requests within the limit
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimiterService.allowRequest(userId));
        }

        // 61st request should be denied
        assertFalse(rateLimiterService.allowRequest(userId));
    }

    @Test
    void testRateLimiterPerUser() {
        Long user1 = 1L;
        Long user2 = 2L;

        // User 1 exhausts their limit
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimiterService.allowRequest(user1));
        }
        assertFalse(rateLimiterService.allowRequest(user1));

        // User 2 should still have tokens available
        assertTrue(rateLimiterService.allowRequest(user2));
    }

    @Test
    void testNullUserIdNotAllowed() {
        assertFalse(rateLimiterService.allowRequest(null));
    }

    @Test
    void testInvalidUserIdNotAllowed() {
        assertFalse(rateLimiterService.allowRequest(0L));
        assertFalse(rateLimiterService.allowRequest(-1L));
    }

    @Test
    void testGetRemainingTokens() {
        Long userId = 1L;

        // Initially should have all tokens
        assertEquals(60, rateLimiterService.getRemainingTokens(userId));

        // After one request
        rateLimiterService.allowRequest(userId);
        assertEquals(59, rateLimiterService.getRemainingTokens(userId));
    }

    @Test
    void testClearAll() {
        Long userId1 = 1L;
        Long userId2 = 2L;

        rateLimiterService.allowRequest(userId1);
        rateLimiterService.allowRequest(userId2);

        rateLimiterService.clearAll();

        // After clear, should have full tokens again
        assertEquals(60, rateLimiterService.getRemainingTokens(userId1));
        assertEquals(60, rateLimiterService.getRemainingTokens(userId2));
    }
}

package com.siddharth.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RateLimiterService: Token bucket rate limiter per user.
 * Each user gets a bucket that refills at a fixed rate.
 * Thread-safe using ConcurrentHashMap and AtomicLong.
 */
@Service
public class RateLimiterService {

    private static class TokenBucket {
        AtomicLong tokens;
        long lastRefillTime;

        TokenBucket(long tokensPerMinute) {
            this.tokens = new AtomicLong(tokensPerMinute);
            this.lastRefillTime = System.currentTimeMillis();
        }
    }

    @Value("${app.rate-limiter.tokens-per-minute:60}")
    private long tokensPerMinute = 60;

    private static final long REFILL_INTERVAL_MS = 60_000; // 1 minute

    private final Map<Long, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks if the user has available tokens for the request.
     * @param userId The user ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }

        TokenBucket bucket = buckets.computeIfAbsent(userId, k -> new TokenBucket(tokensPerMinute));

        synchronized (bucket) {
            refillBucket(bucket);

            if (bucket.tokens.get() > 0) {
                bucket.tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }

    /**
     * Refills the bucket based on elapsed time.
     */
    private void refillBucket(TokenBucket bucket) {
        long now = System.currentTimeMillis();
        long elapsedMs = now - bucket.lastRefillTime;

        if (elapsedMs >= REFILL_INTERVAL_MS) {
            bucket.tokens.set(tokensPerMinute);
            bucket.lastRefillTime = now;
        } else {
            // Partial refill: proportional to elapsed time
            long tokensToAdd = (elapsedMs * tokensPerMinute) / REFILL_INTERVAL_MS;
            if (tokensToAdd > 0) {
                bucket.tokens.addAndGet(tokensToAdd);
                bucket.lastRefillTime += (tokensToAdd * REFILL_INTERVAL_MS) / tokensPerMinute;

                // Cap at max
                if (bucket.tokens.get() > tokensPerMinute) {
                    bucket.tokens.set(tokensPerMinute);
                }
            }
        }
    }

    /**
     * Gets remaining tokens for a user.
     */
    public long getRemainingTokens(Long userId) {
        TokenBucket bucket = buckets.get(userId);
        if (bucket == null) {
            return tokensPerMinute;
        }
        return bucket.tokens.get();
    }

    /**
     * Clears all buckets (for testing or reset).
     */
    public void clearAll() {
        buckets.clear();
    }
}

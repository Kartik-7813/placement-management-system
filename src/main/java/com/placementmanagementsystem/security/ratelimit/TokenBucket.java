package com.placementmanagementsystem.security.ratelimit;

import java.time.Clock;

/**
 * Thread-safe implementation of the Token Bucket algorithm.
 * Tokens accumulate at a continuous constant rate up to capacity.
 * Each request consumes 1 token.
 */
public class TokenBucket {

    private final long capacity;
    private final long refillTokens;
    private final long refillDurationSeconds;
    private final Clock clock;

    private double availableTokens;
    private long lastRefillTimestampMillis;
    private long lastAccessTimestampMillis;

    public TokenBucket(long capacity, long refillTokens, long refillDurationSeconds) {
        this(capacity, refillTokens, refillDurationSeconds, Clock.systemUTC());
    }

    public TokenBucket(long capacity, long refillTokens, long refillDurationSeconds, Clock clock) {
        if (capacity <= 0 || refillTokens <= 0 || refillDurationSeconds <= 0) {
            throw new IllegalArgumentException("Capacity, refillTokens, and refillDurationSeconds must be positive");
        }
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationSeconds = refillDurationSeconds;
        this.clock = clock;
        this.availableTokens = capacity;
        this.lastRefillTimestampMillis = clock.millis();
        this.lastAccessTimestampMillis = clock.millis();
    }

    /**
     * Attempts to consume 1 token.
     *
     * @return true if token was consumed, false if bucket is exhausted
     */
    public synchronized boolean tryConsume() {
        refill();
        this.lastAccessTimestampMillis = clock.millis();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    /**
     * Returns the estimated number of seconds until at least 1 token is available.
     */
    public synchronized long getSecondsUntilNextToken() {
        refill();
        if (availableTokens >= 1.0) {
            return 0;
        }
        double missingTokens = 1.0 - availableTokens;
        double tokensPerSecond = (double) refillTokens / (double) refillDurationSeconds;
        long seconds = (long) Math.ceil(missingTokens / tokensPerSecond);
        return Math.max(1, seconds);
    }

    /**
     * Checks if the bucket has remained fully replenished and inactive for at least maxIdleMillis.
     */
    public synchronized boolean isStale(long maxIdleMillis) {
        refill();
        long idleTime = clock.millis() - lastAccessTimestampMillis;
        return availableTokens >= capacity && idleTime > maxIdleMillis;
    }

    private void refill() {
        long now = clock.millis();
        long elapsedMillis = now - lastRefillTimestampMillis;
        if (elapsedMillis <= 0) {
            return;
        }

        double tokensToAdd = (elapsedMillis / 1000.0) * ((double) refillTokens / (double) refillDurationSeconds);
        this.availableTokens = Math.min(capacity, this.availableTokens + tokensToAdd);
        this.lastRefillTimestampMillis = now;
    }

    public synchronized double getAvailableTokens() {
        refill();
        return availableTokens;
    }
}


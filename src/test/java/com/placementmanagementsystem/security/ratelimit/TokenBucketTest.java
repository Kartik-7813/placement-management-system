package com.placementmanagementsystem.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenBucket Unit Tests")
class TokenBucketTest {

    static class MutableClock extends Clock {
        private final AtomicLong millis;
        private final ZoneId zone;

        MutableClock(long initialMillis) {
            this.millis = new AtomicLong(initialMillis);
            this.zone = ZoneId.of("UTC");
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public long millis() {
            return millis.get();
        }

        public void advanceSeconds(long seconds) {
            millis.addAndGet(seconds * 1000L);
        }

        public void advanceMillis(long delta) {
            millis.addAndGet(delta);
        }
    }

    @Test
    @DisplayName("Requests below and at capacity are allowed, but next request is rejected")
    void tryConsume_AllowsUpToCapacity_ThenRejects() {
        MutableClock clock = new MutableClock(1_000_000L);
        TokenBucket bucket = new TokenBucket(3, 3, 60, clock);

        assertTrue(bucket.tryConsume(), "1st token should be consumed");
        assertTrue(bucket.tryConsume(), "2nd token should be consumed");
        assertTrue(bucket.tryConsume(), "3rd token (at capacity limit) should be consumed");
        assertFalse(bucket.tryConsume(), "4th token (exceeding capacity) must be rejected");
    }

    @Test
    @DisplayName("Tokens are replenished deterministically after elapsed time")
    void tryConsume_ReplenishesTokensOverTime() {
        MutableClock clock = new MutableClock(1_000_000L);
        // 5 tokens per 50 seconds = 1 token every 10 seconds
        TokenBucket bucket = new TokenBucket(5, 5, 50, clock);

        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume(), "Bucket must be empty");

        // Advance clock by 10 seconds -> 1 token should refill
        clock.advanceSeconds(10);
        assertTrue(bucket.tryConsume(), "1 refilled token should be consumed");
        assertFalse(bucket.tryConsume(), "Bucket should be empty again");

        // Advance clock by 50 seconds -> full capacity refilled
        clock.advanceSeconds(50);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume());
    }

    @Test
    @DisplayName("getSecondsUntilNextToken returns accurate retry-after seconds when exhausted")
    void getSecondsUntilNextToken_CalculatesAccurateRetryAfter() {
        MutableClock clock = new MutableClock(1_000_000L);
        // 5 tokens per 60 seconds -> 1 token every 12 seconds
        TokenBucket bucket = new TokenBucket(5, 5, 60, clock);

        for (int i = 0; i < 5; i++) {
            bucket.tryConsume();
        }

        // Available tokens is 0, missing 1.0 token -> 12 seconds
        assertEquals(12, bucket.getSecondsUntilNextToken());

        // Advance by 6 seconds -> missing 0.5 tokens -> 6 seconds
        clock.advanceSeconds(6);
        assertEquals(6, bucket.getSecondsUntilNextToken());

        // Advance another 6 seconds -> 1 full token available -> retry-after is 0
        clock.advanceSeconds(6);
        assertEquals(0, bucket.getSecondsUntilNextToken());
    }

    @Test
    @DisplayName("isStale identifies inactive buckets that have remained full")
    void isStale_DetectsInactiveFullBuckets() {
        MutableClock clock = new MutableClock(1_000_000L);
        TokenBucket bucket = new TokenBucket(5, 5, 60, clock);

        // Bucket is full, but idle time is 0 -> not stale
        assertFalse(bucket.isStale(300_000L));

        // Advance clock by 6 minutes (360,000 ms) while untouched -> stale
        clock.advanceSeconds(360);
        assertTrue(bucket.isStale(300_000L));

        // If a token is consumed, it is no longer full -> not stale
        bucket.tryConsume();
        assertFalse(bucket.isStale(300_000L));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for non-positive arguments")
    void constructor_ValidatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 5, 60));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(5, 0, 60));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(5, 5, 0));
    }
}


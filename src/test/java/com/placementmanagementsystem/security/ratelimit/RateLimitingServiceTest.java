package com.placementmanagementsystem.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitingService Unit Tests")
class RateLimitingServiceTest {

    static class MutableClock extends Clock {
        private final AtomicLong millis;

        MutableClock(long initialMillis) {
            this.millis = new AtomicLong(initialMillis);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        @Override public long millis() { return millis.get(); }
        public void advanceSeconds(long seconds) { millis.addAndGet(seconds * 1000L); }
    }

    private MutableClock clock;
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(1_000_000L);
        rateLimitingService = new RateLimitingService(
                true,   // enabled
                3,      // loginLimit: 3
                60,     // loginDuration: 60s
                2,      // registerLimit: 2
                60,     // registerDuration: 60s
                false,  // trustedProxy: false
                100,    // maxBuckets: 100
                clock
        );
    }

    @Test
    @DisplayName("Requests below and at configured limit are allowed, then rejected")
    void checkRateLimit_EnforcesConfiguredLimits() {
        String clientIp = "192.168.1.10";

        assertTrue(rateLimitingService.checkRateLimit("login", clientIp).allowed());
        assertTrue(rateLimitingService.checkRateLimit("login", clientIp).allowed());
        assertTrue(rateLimitingService.checkRateLimit("login", clientIp).allowed());

        RateLimitResult rejected = rateLimitingService.checkRateLimit("login", clientIp);
        assertFalse(rejected.allowed());
        assertTrue(rejected.retryAfterSeconds() > 0);
    }

    @Test
    @DisplayName("IP separation: different client IPs have independent buckets")
    void checkRateLimit_EnforcesIpSeparation() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        // Exhaust IP 1
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitingService.checkRateLimit("login", ip1).allowed());
        }
        assertFalse(rateLimitingService.checkRateLimit("login", ip1).allowed());

        // IP 2 must still have full allowance
        assertTrue(rateLimitingService.checkRateLimit("login", ip2).allowed());
        assertTrue(rateLimitingService.checkRateLimit("login", ip2).allowed());
        assertTrue(rateLimitingService.checkRateLimit("login", ip2).allowed());
        assertFalse(rateLimitingService.checkRateLimit("login", ip2).allowed());
    }

    @Test
    @DisplayName("Endpoint separation: login and register have independent budgets")
    void checkRateLimit_EnforcesEndpointSeparation() {
        String clientIp = "192.168.1.50";

        // Exhaust login (limit 3)
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitingService.checkRateLimit("login", clientIp).allowed());
        }
        assertFalse(rateLimitingService.checkRateLimit("login", clientIp).allowed());

        // Registration (limit 2) should still be available
        assertTrue(rateLimitingService.checkRateLimit("register", clientIp).allowed());
        assertTrue(rateLimitingService.checkRateLimit("register", clientIp).allowed());
        assertFalse(rateLimitingService.checkRateLimit("register", clientIp).allowed());
    }

    @Test
    @DisplayName("When rate limiting is disabled, all requests are allowed")
    void checkRateLimit_WhenDisabled_AllowsAll() {
        RateLimitingService disabledService = new RateLimitingService(
                false, 1, 60, 1, 60, false, 100, clock
        );

        String ip = "127.0.0.1";
        for (int i = 0; i < 10; i++) {
            assertTrue(disabledService.checkRateLimit("login", ip).allowed());
        }
    }

    @Test
    @DisplayName("Client IP resolution without trusted proxy ignores X-Forwarded-For")
    void resolveClientIp_UntrustedProxy_UsesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");

        // trustedProxy = false
        String resolvedIp = rateLimitingService.resolveClientIp(request);
        assertEquals("192.168.1.100", resolvedIp, "Must use remoteAddr and ignore header when trustedProxy is false");
    }

    @Test
    @DisplayName("Client IP resolution with trusted proxy uses first valid IP from X-Forwarded-For")
    void resolveClientIp_TrustedProxy_UsesFirstForwardedIp() {
        RateLimitingService proxyService = new RateLimitingService(
                true, 5, 60, 5, 60, true, 100, clock
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Load balancer IP
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        String resolvedIp = proxyService.resolveClientIp(request);
        assertEquals("203.0.113.195", resolvedIp);
    }

    @Test
    @DisplayName("Bounded capacity: stale buckets are evicted when cache reaches capacity")
    void evictStaleBuckets_ReclaimsMemory() {
        RateLimitingService smallCapacityService = new RateLimitingService(
                true, 5, 60, 5, 60, false, 2, clock
        );

        smallCapacityService.checkRateLimit("login", "10.0.0.1");
        smallCapacityService.checkRateLimit("login", "10.0.0.2");
        assertEquals(2, smallCapacityService.getBucketCount());

        // Advance clock by 6 minutes so buckets become stale
        clock.advanceSeconds(360);

        // Access by a new IP triggers eviction and allows new bucket
        assertTrue(smallCapacityService.checkRateLimit("login", "10.0.0.3").allowed());
    }
}


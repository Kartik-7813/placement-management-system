package com.placementmanagementsystem.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages in-memory token buckets for sensitive endpoints.
 * Enforces per-IP and per-endpoint rate limits.
 */
@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

    private final boolean enabled;
    private final long loginLimit;
    private final long loginDurationSeconds;
    private final long registerLimit;
    private final long registerDurationSeconds;
    private final boolean trustedProxy;
    private final int maxBuckets;
    private final Clock clock;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingService(
            @Value("${application.rate-limiting.enabled:true}") boolean enabled,
            @Value("${application.rate-limiting.login.limit:5}") long loginLimit,
            @Value("${application.rate-limiting.login.duration-in-seconds:60}") long loginDurationSeconds,
            @Value("${application.rate-limiting.register.limit:5}") long registerLimit,
            @Value("${application.rate-limiting.register.duration-in-seconds:60}") long registerDurationSeconds,
            @Value("${application.rate-limiting.trusted-proxy:false}") boolean trustedProxy,
            @Value("${application.rate-limiting.max-buckets:10000}") int maxBuckets
    ) {
        this(enabled, loginLimit, loginDurationSeconds, registerLimit, registerDurationSeconds, trustedProxy, maxBuckets, Clock.systemUTC());
    }

    public RateLimitingService(
            boolean enabled,
            long loginLimit,
            long loginDurationSeconds,
            long registerLimit,
            long registerDurationSeconds,
            boolean trustedProxy,
            int maxBuckets,
            Clock clock
    ) {
        this.enabled = enabled;
        this.loginLimit = loginLimit;
        this.loginDurationSeconds = loginDurationSeconds;
        this.registerLimit = registerLimit;
        this.registerDurationSeconds = registerDurationSeconds;
        this.trustedProxy = trustedProxy;
        this.maxBuckets = maxBuckets;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getLoginLimit() {
        return loginLimit;
    }

    public long getRegisterLimit() {
        return registerLimit;
    }

    /**
     * Checks whether a request from the given client IP to the specified endpoint type is allowed.
     *
     * @param endpointType "login" or "register"
     * @param clientIp     client IP address
     * @return RateLimitResult indicating allowed status and retryAfterSeconds
     */
    public RateLimitResult checkRateLimit(String endpointType, String clientIp) {
        if (!enabled) {
            return RateLimitResult.allow();
        }

        long limit = "login".equalsIgnoreCase(endpointType) ? loginLimit : registerLimit;
        long duration = "login".equalsIgnoreCase(endpointType) ? loginDurationSeconds : registerDurationSeconds;

        String key = endpointType.toLowerCase() + ":" + clientIp;

        // Bounded storage: prevent memory exhaustion from random IP attacks
        if (buckets.size() >= maxBuckets) {
            evictStaleBuckets();
            if (buckets.size() >= maxBuckets && !buckets.containsKey(key)) {
                log.warn("Rate limit bucket cache reached maximum capacity [{}]", maxBuckets);
                return RateLimitResult.reject(duration);
            }
        }

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit, limit, duration, clock));

        if (bucket.tryConsume()) {
            return RateLimitResult.allow();
        } else {
            long retryAfter = bucket.getSecondsUntilNextToken();
            return RateLimitResult.reject(retryAfter);
        }
    }

    /**
     * Resolves the client IP safely without blindly trusting unverified proxy headers.
     * When trusted-proxy is false (default), getRemoteAddr() is used to avoid header spoofing.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (trustedProxy) {
            String forwardedHeader = request.getHeader("X-Forwarded-For");
            if (forwardedHeader != null && !forwardedHeader.isBlank()) {
                String firstIp = forwardedHeader.split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return firstIp;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        return ip.matches("^[a-fA-F0-9:.]+$");
    }

    /**
     * Evicts buckets that have remained full and untouched for over 5 minutes.
     */
    public void evictStaleBuckets() {
        long fiveMinutesMillis = 5 * 60 * 1000L;
        buckets.entrySet().removeIf(entry -> entry.getValue().isStale(fiveMinutesMillis));
    }

    public void clear() {
        buckets.clear();
    }

    public int getBucketCount() {
        return buckets.size();
    }
}

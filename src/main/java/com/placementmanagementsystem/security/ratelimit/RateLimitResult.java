package com.placementmanagementsystem.security.ratelimit;

/**
 * Result of a rate limiting evaluation.
 *
 * @param allowed            whether the request is allowed to proceed
 * @param retryAfterSeconds  estimated seconds until next token is available if rejected
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult reject(long retryAfterSeconds) {
        return new RateLimitResult(false, Math.max(1, retryAfterSeconds));
    }
}

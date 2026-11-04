package com.placementmanagementsystem.security.ratelimit;

import com.placementmanagementsystem.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Filter intercepting incoming HTTP requests to sensitive authentication endpoints
 * (/api/auth/login, /api/auth/register) to enforce per-IP rate limiting before
 * cryptographic password hashing or database access occurs.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitingService rateLimitingService, ObjectMapper objectMapper) {
        this.rateLimitingService = rateLimitingService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!rateLimitingService.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Enforce rate limiting strictly on POST requests to login and register endpoints
        if ("POST".equalsIgnoreCase(method)) {
            if (LOGIN_PATH.equals(requestUri)) {
                if (handleRateLimit("login", request, response)) {
                    return;
                }
            } else if (REGISTER_PATH.equals(requestUri)) {
                if (handleRateLimit("register", request, response)) {
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean handleRateLimit(String endpointType, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = rateLimitingService.resolveClientIp(request);
        RateLimitResult result = rateLimitingService.checkRateLimit(endpointType, clientIp);

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for [{}] at path [{}]", endpointType, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));

            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Too many requests. Please try again after " + result.retryAfterSeconds() + " seconds.",
                    request.getRequestURI()
            );

            objectMapper.writeValue(response.getWriter(), errorResponse);
            return true; // Rejected, stop further processing
        }

        return false; // Allowed, continue filter chain
    }
}


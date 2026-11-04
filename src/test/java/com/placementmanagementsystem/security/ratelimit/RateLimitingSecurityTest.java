package com.placementmanagementsystem.security.ratelimit;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.controller.AuthController;
import com.placementmanagementsystem.controller.JobController;
import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.AuthService;
import com.placementmanagementsystem.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, JobController.class})
@Import({
        SecurityConfig.class,
        RateLimitingService.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "application.rate-limiting.enabled=true",
        "application.rate-limiting.login.limit=2",
        "application.rate-limiting.login.duration-in-seconds=60",
        "application.rate-limiting.register.limit=2",
        "application.rate-limiting.register.duration-in-seconds=60"
})
@DisplayName("Rate Limiting Filter & Security Integration Tests")
class RateLimitingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        rateLimitingService.clear();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token-123", "student@example.com", "ROLE_STUDENT", 86400000L));
        when(authService.registerStudent(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token-123", "student@example.com", "ROLE_STUDENT", 86400000L));
    }

    @Test
    @DisplayName("Requests to login up to limit are allowed; subsequent request receives HTTP 429 Too Many Requests")
    void login_ExceedingLimit_Returns429TooManyRequests() throws Exception {
        String clientIp = "192.168.1.100";
        String loginPayload = """
                {
                    "email": "student@example.com",
                    "password": "Password123!"
                }
                """;

        // Request 1: allowed (200 OK)
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk());

        // Request 2 (limit = 2): allowed (200 OK)
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk());

        // Request 3: rate limit exceeded -> HTTP 429 with Retry-After and ErrorResponse JSON
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")))
                .andExpect(jsonPath("$.message", containsString("Too many requests")))
                .andExpect(jsonPath("$.path", is("/api/auth/login")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("IP isolation: exhausting rate limit on one IP does not block another client IP")
    void login_IpIsolation_DifferentIpRemainsAllowed() throws Exception {
        String exhaustedIp = "10.0.0.1";
        String freshIp = "10.0.0.2";
        String payload = """
                {
                    "email": "student@example.com",
                    "password": "Password123!"
                }
                """;

        // Exhaust IP 1
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> { request.setRemoteAddr(exhaustedIp); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }

        // IP 1 blocked
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(exhaustedIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests());

        // IP 2 must still succeed
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(freshIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Registration endpoint is also protected by rate limiting")
    void register_ExceedingLimit_Returns429TooManyRequests() throws Exception {
        String clientIp = "172.16.0.5";
        String registerPayload = """
                {
                    "email": "newstudent@example.com",
                    "password": "Password123!",
                    "rollNumber": "CS2026",
                    "name": "Jane Doe"
                }
                """;

        // 2 requests allowed
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .with(request -> { request.setRemoteAddr(clientIp); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerPayload))
                    .andExpect(status().isCreated());
        }

        // 3rd request rejected with 429
        mockMvc.perform(post("/api/auth/register")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")))
                .andExpect(jsonPath("$.path", is("/api/auth/register")));
    }

    @Test
    @DisplayName("Unrelated business endpoints are unaffected even if auth rate limit is exhausted")
    void unrelatedBusinessEndpoint_IsNotRateLimited() throws Exception {
        String clientIp = "192.168.1.200";
        String loginPayload = """
                {
                    "email": "student@example.com",
                    "password": "Password123!"
                }
                """;

        // Exhaust auth limit on this IP
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> { request.setRemoteAddr(clientIp); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginPayload))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests());

        // Public business endpoint GET /api/jobs must NOT be throttled
        when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/jobs")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; }))
                .andExpect(status().isOk());
    }
}


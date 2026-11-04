package com.placementmanagementsystem.security;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.controller.AuthController;
import com.placementmanagementsystem.controller.JobController;
import com.placementmanagementsystem.controller.StudentController;
import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.ratelimit.RateLimitingService;
import com.placementmanagementsystem.service.AuthService;
import com.placementmanagementsystem.service.JobService;
import com.placementmanagementsystem.service.StudentService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, JobController.class, StudentController.class})
@Import({
        SecurityConfig.class,
        RateLimitingService.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "application.cors.allowed-origins=http://localhost:3000,http://localhost:5173",
        "application.rate-limiting.enabled=true",
        "application.rate-limiting.login.limit=2",
        "application.rate-limiting.login.duration-in-seconds=60"
})
@DisplayName("CORS Configuration & Security Filter Chain Tests")
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        rateLimitingService.clear();

        when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("mock-jwt-token", "student@example.com", "ROLE_STUDENT", 86400000L));
    }

    @Test
    @DisplayName("CORS: Requests from http://localhost:3000 receive Access-Control-Allow-Origin header")
    void cors_AllowedOrigin_Localhost3000_ReceivesCorsHeader() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("CORS: Requests from http://localhost:5173 receive Access-Control-Allow-Origin header")
    void cors_AllowedOrigin_Localhost5173_ReceivesCorsHeader() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("CORS: Requests from unapproved origin do not receive Access-Control-Allow-Origin")
    void cors_UnapprovedOrigin_IsNotGrantedCorsAccess() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Origin", "http://unapproved-origin.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: OPTIONS preflight from allowed origin returns 200 with methods and headers")
    void cors_OptionsPreflight_AllowedOrigin_Returns200WithHeaders() throws Exception {
        mockMvc.perform(options("/api/jobs")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("OPTIONS")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    @DisplayName("CORS: OPTIONS preflight from unapproved origin returns 403 Forbidden")
    void cors_OptionsPreflight_UnapprovedOrigin_Returns403Forbidden() throws Exception {
        mockMvc.perform(options("/api/jobs")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: Exposed headers explicitly include Retry-After")
    void cors_ExposedHeaders_IncludesRetryAfter() throws Exception {
        mockMvc.perform(options("/api/jobs")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")));
    }

    @Test
    @DisplayName("CORS: Allow credentials is not enabled (stateless Bearer authentication)")
    void cors_AllowCredentials_IsNotEnabled() throws Exception {
        mockMvc.perform(options("/api/jobs")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("Security: Existing anonymous access rules remain intact for public endpoints")
    void anonymousAccess_JobsEndpoint_RemainsPublic() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Authenticated endpoints still require JWT authentication (401)")
    void authenticatedEndpoint_Students_WithoutJwt_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rate Limiting: Remains active under CORS and preserves CORS headers on HTTP 429")
    void rateLimiting_LoginEndpoint_RemainsActiveUnderCors() throws Exception {
        String clientIp = "192.168.1.50";
        String loginPayload = """
                {
                    "email": "student@example.com",
                    "password": "Password123!"
                }
                """;

        // Request 1: 200 OK with CORS header
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        // Request 2 (limit = 2): 200 OK with CORS header
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        // Request 3: 429 Too Many Requests, still has CORS header and Retry-After header
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr(clientIp); return request; })
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}


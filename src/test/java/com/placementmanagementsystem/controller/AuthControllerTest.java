package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String VALID_REGISTER_JSON = """
            {
                "email": "student@example.com",
                "password": "strongPassword123",
                "rollNumber": "CS2026100",
                "name": "Alex Smith"
            }
            """;

    private static final String INVALID_REGISTER_JSON = """
            {
                "email": "not-an-email",
                "password": "123",
                "rollNumber": "",
                "name": ""
            }
            """;

    private static final String VALID_LOGIN_JSON = """
            {
                "email": "student@example.com",
                "password": "strongPassword123"
            }
            """;

    @Test
    @DisplayName("POST /api/auth/register should return 201 Created with AuthResponse")
    void register_Success() throws Exception {
        AuthResponse authResponse = new AuthResponse("test.jwt.token", "student@example.com", "ROLE_STUDENT", 86400000L);
        when(authService.registerStudent(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("test.jwt.token")))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.email", is("student@example.com")))
                .andExpect(jsonPath("$.role", is("ROLE_STUDENT")))
                .andExpect(jsonPath("$.expiresInMs", is(86400000)));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid data should return 400 Bad Request with validation errors")
    void register_ValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_REGISTER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.rollNumber", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email/rollNumber should return 409 Conflict")
    void register_DuplicateConflict() throws Exception {
        when(authService.registerStudent(any(RegisterRequest.class)))
                .thenThrow(new DuplicateStudentException("An account with email student@example.com already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("An account with email student@example.com already exists")));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK with AuthResponse")
    void login_Success() throws Exception {
        AuthResponse authResponse = new AuthResponse("test.jwt.token", "student@example.com", "ROLE_STUDENT", 86400000L);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("test.jwt.token")))
                .andExpect(jsonPath("$.email", is("student@example.com")))
                .andExpect(jsonPath("$.role", is("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong credentials should return 401 Unauthorized")
    void login_InvalidCredentials_ShouldReturn401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }
}

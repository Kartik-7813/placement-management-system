package com.placementmanagementsystem.security;

import com.placementmanagementsystem.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Security Exception Handlers Unit Tests")
class SecurityExceptionHandlersTest {

    private ObjectMapper objectMapper;
    private JwtAuthenticationEntryPoint entryPoint;
    private JwtAccessDeniedHandler accessDeniedHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should return HTTP 401 with standard ErrorResponse JSON")
    void commence_ShouldReturn401ErrorResponse() throws IOException {
        request.setRequestURI("/api/students");
        AuthenticationException authException = new BadCredentialsException("Bad credentials");

        entryPoint.commence(request, response, authException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getError());
        assertEquals("Full authentication is required to access this resource.", body.getMessage());
        assertEquals("/api/students", body.getPath());
        assertNotNull(body.getTimestamp());
        assertNull(body.getValidationErrors());
    }

    @Test
    @DisplayName("JwtAccessDeniedHandler should return HTTP 403 with standard ErrorResponse JSON")
    void handle_ShouldReturn403ErrorResponse() throws IOException {
        request.setRequestURI("/api/dashboard/summary");
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");

        accessDeniedHandler.handle(request, response, accessDeniedException);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Forbidden", body.getError());
        assertEquals("Access is denied. You do not have permission to access this resource.", body.getMessage());
        assertEquals("/api/dashboard/summary", body.getPath());
        assertNotNull(body.getTimestamp());
        assertNull(body.getValidationErrors());
    }
}

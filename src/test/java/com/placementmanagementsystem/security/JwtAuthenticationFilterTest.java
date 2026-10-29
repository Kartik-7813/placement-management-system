package com.placementmanagementsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private UserDetails enabledUser;
    private UserDetails disabledUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        enabledUser = new User(
                "student@example.com",
                "encodedPassword",
                true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        disabledUser = new User(
                "disabled@example.com",
                "encodedPassword",
                false, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should continue filter chain without authenticating when Authorization header is missing")
    void doFilterInternal_ShouldContinueChain_WhenAuthHeaderMissing() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Should continue filter chain without authenticating when Authorization header does not start with Bearer ")
    void doFilterInternal_ShouldContinueChain_WhenHeaderNotBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Should continue filter chain without authenticating when Bearer token is empty")
    void doFilterInternal_ShouldContinueChain_WhenBearerTokenIsEmpty() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Should authenticate request and populate SecurityContext for valid Bearer token and enabled user")
    void doFilterInternal_ShouldAuthenticate_WhenValidTokenAndEnabledUser() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.extractUsername(validToken)).thenReturn("student@example.com");
        when(userDetailsService.loadUserByUsername("student@example.com")).thenReturn(enabledUser);
        when(jwtService.isTokenValid(validToken, enabledUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
        assertEquals(enabledUser, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("Should not authenticate and should clear context when token is invalid or tampered")
    void doFilterInternal_ShouldNotAuthenticate_WhenTokenInvalid() throws ServletException, IOException {
        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtService.extractUsername(invalidToken)).thenReturn("student@example.com");
        when(userDetailsService.loadUserByUsername("student@example.com")).thenReturn(enabledUser);
        when(jwtService.isTokenValid(invalidToken, enabledUser)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should not authenticate and should clear context when user account is disabled")
    void doFilterInternal_ShouldNotAuthenticate_WhenAccountDisabled() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.extractUsername(validToken)).thenReturn("disabled@example.com");
        when(userDetailsService.loadUserByUsername("disabled@example.com")).thenReturn(disabledUser);
        when(jwtService.isTokenValid(validToken, disabledUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should not authenticate and should clear context when user does not exist")
    void doFilterInternal_ShouldNotAuthenticate_WhenUserNotFound() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtService.extractUsername(validToken)).thenReturn("missing@example.com");
        when(userDetailsService.loadUserByUsername("missing@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should not overwrite existing valid authentication in SecurityContext")
    void doFilterInternal_ShouldNotOverwrite_ExistingAuthentication() throws ServletException, IOException {
        String validToken = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validToken);

        Authentication existingAuth = new UsernamePasswordAuthenticationToken(
                "existingUser", null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername(validToken)).thenReturn("student@example.com");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }
}

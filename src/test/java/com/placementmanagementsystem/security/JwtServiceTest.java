package com.placementmanagementsystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    // 256-bit (32 bytes) test-only secret: Base64 of 32 deterministic test bytes
    private static final String TEST_BASE64_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-material-32-byte".getBytes()
    );

    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_BASE64_SECRET, EXPIRATION_MS);
        userDetails = new User(
                "student@example.com",
                "encodedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    @Test
    @DisplayName("generateToken and extractUsername should correctly create and parse token subject")
    void generateToken_ShouldCreateValidTokenWithSubject() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("student@example.com", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("extractExpiration should extract expiration date in the future")
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtService.generateToken(userDetails);
        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("isTokenValid should return true for a valid, unexpired token matching user details")
    void isTokenValid_ShouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("isTokenValid should return false when token subject does not match user details")
    void isTokenValid_ShouldReturnFalseWhenSubjectMismatches() {
        String token = jwtService.generateToken(userDetails);

        UserDetails differentUser = new User(
                "other@example.com",
                "encodedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("isTokenValid and isTokenExpired should return true/false respectively for expired token")
    void isTokenValid_ShouldReturnFalseForExpiredToken() {
        // Create a JwtService with negative expiration (already expired at creation)
        JwtService expiredJwtService = new JwtService(TEST_BASE64_SECRET, -1000);
        String expiredToken = expiredJwtService.generateToken(userDetails);

        assertTrue(expiredJwtService.isTokenExpired(expiredToken));
        assertFalse(expiredJwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("isTokenValid should return false when token signature or payload is tampered")
    void isTokenValid_ShouldReturnFalseForTamperedToken() {
        String token = jwtService.generateToken(userDetails);

        // Tamper with the token string
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        boolean isValid = jwtService.isTokenValid(tamperedToken, userDetails);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("JwtService should reject empty, malformed, or undersized secret keys on initialization")
    void constructor_ShouldThrowExceptionForInvalidSecret() {
        // Empty secret
        assertThrows(IllegalArgumentException.class, () -> new JwtService("", EXPIRATION_MS));

        // Malformed non-base64 secret
        assertThrows(IllegalArgumentException.class, () -> new JwtService("not-valid-base64!@#$", EXPIRATION_MS));

        // Undersized secret (less than 32 bytes / 256 bits)
        String shortBase64 = Base64.getEncoder().encodeToString("short-secret".getBytes());
        assertThrows(IllegalArgumentException.class, () -> new JwtService(shortBase64, EXPIRATION_MS));
    }
}

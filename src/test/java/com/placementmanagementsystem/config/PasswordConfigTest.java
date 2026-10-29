package com.placementmanagementsystem.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        PasswordConfig passwordConfig = new PasswordConfig();
        passwordEncoder = passwordConfig.passwordEncoder();
    }

    @Test
    @DisplayName("BCrypt encoding produces a hash different from the raw password")
    void testPasswordEncodingProducesHashDifferentFromRaw() {
        String rawPassword = "TestSecretPassword#2026";
        String encodedHash = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedHash);
        assertNotEquals(rawPassword, encodedHash);
        assertTrue(encodedHash.startsWith("$2a$") || encodedHash.startsWith("$2b$"),
                "BCrypt encoded hash should start with $2a$ or $2b$ prefix");
    }

    @Test
    @DisplayName("Correct raw password matches the stored BCrypt hash")
    void testPasswordMatchesCorrectRawPassword() {
        String rawPassword = "TestSecretPassword#2026";
        String encodedHash = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(rawPassword, encodedHash);
        assertTrue(matches, "Password encoder should verify correct raw password against stored hash");
    }

    @Test
    @DisplayName("Incorrect raw password does not match the stored BCrypt hash")
    void testPasswordDoesNotMatchIncorrectPassword() {
        String rawPassword = "TestSecretPassword#2026";
        String wrongPassword = "WrongPasswordAttempt#9999";
        String encodedHash = passwordEncoder.encode(rawPassword);

        boolean matches = passwordEncoder.matches(wrongPassword, encodedHash);
        assertFalse(matches, "Password encoder must reject incorrect password attempts");
    }

    @Test
    @DisplayName("BCrypt applies unique random salts so identical passwords produce distinct hashes")
    void testDifferentSaltsForSamePassword() {
        String rawPassword = "TestSecretPassword#2026";
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(hash1, hash2, "BCrypt must generate unique salts for each encoding operation");
        assertTrue(passwordEncoder.matches(rawPassword, hash1));
        assertTrue(passwordEncoder.matches(rawPassword, hash2));
    }
}

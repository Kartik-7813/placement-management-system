package com.placementmanagementsystem.entity;

import com.placementmanagementsystem.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("User creation should populate sensible defaults (enabled=true, createdAt not null)")
    void testUserCreationDefaults() {
        User user = new User("admin@placement.edu", "$2a$10$hashedPasswordHere", Role.ROLE_ADMIN);

        assertNotNull(user.getEmail());
        assertEquals("admin@placement.edu", user.getEmail());
        assertEquals(Role.ROLE_ADMIN, user.getRole());
        assertTrue(user.isEnabled());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("User toString must never print the password field value to prevent credential leaking in logs")
    void testToStringDoesNotExposePassword() {
        String sensitiveHash = "$2a$10$verySecretHashedPassword123456789";
        User user = new User("admin@placement.edu", sensitiveHash, Role.ROLE_ADMIN);

        String stringRepresentation = user.toString();

        assertNotNull(stringRepresentation);
        assertFalse(stringRepresentation.contains(sensitiveHash),
                "toString() must not expose the password hash");
        assertFalse(stringRepresentation.contains("password="),
                "toString() should omit the password field entirely");
        assertTrue(stringRepresentation.contains("admin@placement.edu"));
        assertTrue(stringRepresentation.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("User equals and hashCode should match when email is identical across instances")
    void testUserEqualsAndHashCode() {
        User user1 = new User(1L, "user@placement.edu", "hash1", Role.ROLE_STUDENT, true, LocalDateTime.now());
        User user2 = new User(2L, "user@placement.edu", "hash2", Role.ROLE_STUDENT, true, LocalDateTime.now());

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    @DisplayName("User instances with different emails should not be equal")
    void testUserDifferentEmailNotEqual() {
        User user1 = new User("user1@placement.edu", "hash1", Role.ROLE_STUDENT);
        User user2 = new User("user2@placement.edu", "hash1", Role.ROLE_STUDENT);

        assertNotEquals(user1, user2);
    }

    @Test
    @DisplayName("HashSet should maintain uniqueness of Users by natural key email")
    void testUserInHashSet() {
        Set<User> users = new HashSet<>();
        User user1 = new User(1L, "duplicate@placement.edu", "hash1", Role.ROLE_COMPANY, true, LocalDateTime.now());
        User user2 = new User(2L, "duplicate@placement.edu", "hash2", Role.ROLE_COMPANY, true, LocalDateTime.now());

        users.add(user1);
        users.add(user2);

        assertEquals(1, users.size());
        assertTrue(users.contains(new User(99L, "duplicate@placement.edu", "hash3", Role.ROLE_COMPANY, true, LocalDateTime.now())));
    }

    @Test
    @DisplayName("Role enum contains all required roles with Spring Security ROLE_ prefix")
    void testRoleEnumValues() {
        assertEquals(3, Role.values().length);
        assertEquals(Role.ROLE_ADMIN, Role.valueOf("ROLE_ADMIN"));
        assertEquals(Role.ROLE_STUDENT, Role.valueOf("ROLE_STUDENT"));
        assertEquals(Role.ROLE_COMPANY, Role.valueOf("ROLE_COMPANY"));
    }
}

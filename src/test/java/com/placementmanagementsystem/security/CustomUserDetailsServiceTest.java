package com.placementmanagementsystem.security;

import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Known email loads the correct user details, password hash, and ROLE_ granted authority")
    void testLoadUserByUsername_success() {
        String email = "student@placement.edu";
        String dummyHash = "$2a$10$dummyHashedPasswordStringForTestPurposeOnly";
        User user = new User(1L, email, dummyHash, Role.ROLE_STUDENT, true, LocalDateTime.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals(dummyHash, userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());

        // Verify authority contains exact ROLE_STUDENT
        assertEquals(1, userDetails.getAuthorities().size());
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals("ROLE_STUDENT", authority.getAuthority());

        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Missing email produces UsernameNotFoundException")
    void testLoadUserByUsername_missingEmailThrowsException() {
        String nonExistentEmail = "unknown@placement.edu";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(nonExistentEmail)
        );

        assertTrue(exception.getMessage().contains("User not found with email: " + nonExistentEmail));
        verify(userRepository).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("Disabled account remains disabled in the returned UserDetails representation")
    void testLoadUserByUsername_disabledAccount() {
        String email = "inactive.company@placement.edu";
        String dummyHash = "$2a$10$dummyHashedPasswordStringForTestPurposeOnly";
        User disabledUser = new User(2L, email, dummyHash, Role.ROLE_COMPANY, false, LocalDateTime.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertFalse(userDetails.isEnabled(), "UserDetails must reflect the entity's disabled state");

        assertEquals(1, userDetails.getAuthorities().size());
        assertEquals("ROLE_COMPANY", userDetails.getAuthorities().iterator().next().getAuthority());
        verify(userRepository).findByEmail(email);
    }
}

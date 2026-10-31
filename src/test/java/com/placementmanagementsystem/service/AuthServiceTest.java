package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "student@example.com",
                "rawPassword123",
                "CS2026099",
                "Jane Doe"
        );

        loginRequest = new LoginRequest(
                "student@example.com",
                "rawPassword123"
        );

        userDetails = new org.springframework.security.core.userdetails.User(
                "student@example.com",
                "hashedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    @Test
    @DisplayName("registerStudent should successfully hash password, save user & student, and return JWT")
    void registerStudent_Success() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(studentRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(studentRepository.existsByRollNumber("CS2026099")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("$2a$10$hashedPasswordVal");
        when(userDetailsService.loadUserByUsername("student@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("generated.jwt.token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.registerStudent(registerRequest);

        assertNotNull(response);
        assertEquals("generated.jwt.token", response.getToken());
        assertEquals("student@example.com", response.getEmail());
        assertEquals(Role.ROLE_STUDENT.name(), response.getRole());
        assertEquals(86400000L, response.getExpiresInMs());

        // Verify password is encrypted and never raw
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("student@example.com", savedUser.getEmail());
        assertEquals("$2a$10$hashedPasswordVal", savedUser.getPassword());
        assertNotEquals("rawPassword123", savedUser.getPassword());
        assertEquals(Role.ROLE_STUDENT, savedUser.getRole());

        // Verify student profile saved
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();
        assertEquals("CS2026099", savedStudent.getRollNumber());
        assertEquals("Jane Doe", savedStudent.getName());
        assertEquals("student@example.com", savedStudent.getEmail());
    }

    @Test
    @DisplayName("registerStudent should reject registration when user email already exists")
    void registerStudent_DuplicateUserEmail_ShouldThrowException() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        DuplicateStudentException ex = assertThrows(DuplicateStudentException.class,
                () -> authService.registerStudent(registerRequest));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent should reject registration when student roll number already exists")
    void registerStudent_DuplicateRollNumber_ShouldThrowException() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(studentRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(studentRepository.existsByRollNumber("CS2026099")).thenReturn(true);

        DuplicateStudentException ex = assertThrows(DuplicateStudentException.class,
                () -> authService.registerStudent(registerRequest));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("login should succeed with valid credentials and return JWT AuthResponse")
    void login_Success() {
        when(userDetailsService.loadUserByUsername("student@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("login.jwt.token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("login.jwt.token", response.getToken());
        assertEquals("student@example.com", response.getEmail());
        assertEquals("ROLE_STUDENT", response.getRole());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("student@example.com", "rawPassword123")
        );
    }

    @Test
    @DisplayName("login should throw BadCredentialsException when authentication fails")
    void login_InvalidCredentials_ShouldThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("AuthService class must be annotated with @Transactional(readOnly = true)")
    void authServiceClass_ShouldHaveTransactionalReadOnly() {
        org.springframework.transaction.annotation.Transactional annotation =
                AuthService.class.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertNotNull(annotation, "AuthService class must have @Transactional annotation");
        assertTrue(annotation.readOnly(), "AuthService default transaction mode must be readOnly = true");
    }

    @Test
    @DisplayName("registerStudent method must be explicitly annotated with write @Transactional")
    void registerStudentMethod_ShouldHaveWriteTransactional() throws NoSuchMethodException {
        var method = AuthService.class.getMethod("registerStudent", RegisterRequest.class);
        org.springframework.transaction.annotation.Transactional annotation =
                method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertNotNull(annotation, "registerStudent method must have @Transactional annotation");
        assertFalse(annotation.readOnly(), "registerStudent method must be a write transaction (readOnly = false)");
    }
}

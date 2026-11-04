package com.placementmanagementsystem.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.service.AuthService;
import com.placementmanagementsystem.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Log Hygiene and Sensitive Data Protection Tests")
class LogHygieneAndSensitiveDataProtectionTest {

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

    @Mock
    private AuditService auditService;

    private AuthService authService;
    private JwtAuthenticationEntryPoint entryPoint;
    private JwtAccessDeniedHandler accessDeniedHandler;
    private GlobalExceptionHandler exceptionHandler;

    private ListAppender<ILoggingEvent> authServiceAppender;
    private ListAppender<ILoggingEvent> entryPointAppender;
    private ListAppender<ILoggingEvent> accessDeniedAppender;
    private ListAppender<ILoggingEvent> exceptionHandlerAppender;

    private Logger authServiceLogger;
    private Logger entryPointLogger;
    private Logger accessDeniedLogger;
    private Logger exceptionHandlerLogger;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                studentRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                userDetailsService,
                auditService
        );

        ObjectMapper objectMapper = new ObjectMapper();
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);
        exceptionHandler = new GlobalExceptionHandler();

        // Setup Logback appenders to capture log events
        authServiceLogger = (Logger) LoggerFactory.getLogger(AuthService.class);
        authServiceLogger.setLevel(Level.DEBUG);
        authServiceAppender = new ListAppender<>();
        authServiceAppender.start();
        authServiceLogger.addAppender(authServiceAppender);

        entryPointLogger = (Logger) LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
        entryPointLogger.setLevel(Level.DEBUG);
        entryPointAppender = new ListAppender<>();
        entryPointAppender.start();
        entryPointLogger.addAppender(entryPointAppender);

        accessDeniedLogger = (Logger) LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
        accessDeniedLogger.setLevel(Level.DEBUG);
        accessDeniedAppender = new ListAppender<>();
        accessDeniedAppender.start();
        accessDeniedLogger.addAppender(accessDeniedAppender);

        exceptionHandlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        exceptionHandlerLogger.setLevel(Level.DEBUG);
        exceptionHandlerAppender = new ListAppender<>();
        exceptionHandlerAppender.start();
        exceptionHandlerLogger.addAppender(exceptionHandlerAppender);
    }

    @AfterEach
    void tearDown() {
        if (authServiceLogger != null && authServiceAppender != null) {
            authServiceLogger.detachAppender(authServiceAppender);
        }
        if (entryPointLogger != null && entryPointAppender != null) {
            entryPointLogger.detachAppender(entryPointAppender);
        }
        if (accessDeniedLogger != null && accessDeniedAppender != null) {
            accessDeniedLogger.detachAppender(accessDeniedAppender);
        }
        if (exceptionHandlerLogger != null && exceptionHandlerAppender != null) {
            exceptionHandlerLogger.detachAppender(exceptionHandlerAppender);
        }
    }

    @Test
    @DisplayName("User registration should log at INFO level with internal IDs, never exposing email, roll number, password, or token")
    void registerStudent_ShouldLogOperationalInfoWithoutExposingSecretsOrPii() {
        String rawPassword = "SuperSecretPassword123!";
        String mockJwtToken = "header.payload.signature-mock-token";
        String email = "student.hygiene@example.com";
        String rollNumber = "HYG202601";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(rawPassword);
        request.setName("Hygiene Test");
        request.setRollNumber(rollNumber);
        request.setPhone("9876543210");
        request.setDob(LocalDate.of(2003, 1, 1));
        request.setGender("Other");
        request.setDepartment("Information Technology");
        request.setCgpa(new BigDecimal("8.50"));
        request.setPassingYear(2026);
        request.setAddress("Campus Hostel");

        User savedUser = new User(email, "encoded-password", Role.ROLE_STUDENT);
        savedUser.setUserId(42L);

        Student savedStudent = new Student();
        savedStudent.setStudentId(101L);
        savedStudent.setRollNumber(rollNumber);
        savedStudent.setEmail(email);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "encoded-password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(studentRepository.existsByEmail(email)).thenReturn(false);
        when(studentRepository.existsByRollNumber(rollNumber)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(mockJwtToken);
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.registerStudent(request);

        assertNotNull(response);
        assertEquals(mockJwtToken, response.getToken());

        List<ILoggingEvent> logEvents = authServiceAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected at least one log event during registration");

        boolean foundRegistrationLog = false;
        for (ILoggingEvent event : logEvents) {
            String formattedMessage = event.getFormattedMessage();

            // Verify operational visibility
            if (event.getLevel() == Level.INFO && formattedMessage.contains("Student registered successfully")) {
                foundRegistrationLog = true;
            }

            // Verify strict PII and secret protection
            assertFalse(formattedMessage.contains(email), "Log must NEVER contain email address!");
            assertFalse(formattedMessage.contains(rollNumber), "Log must NEVER contain student roll number!");
            assertFalse(formattedMessage.contains(rawPassword), "Log must NEVER contain plain text password!");
            assertFalse(formattedMessage.contains(mockJwtToken), "Log must NEVER contain JWT token!");
            assertFalse(formattedMessage.contains("encoded-password"), "Log should not contain password hash!");
        }

        assertTrue(foundRegistrationLog, "Expected to find an INFO log confirming registration with internal IDs");
    }

    @Test
    @DisplayName("Successful login should log at INFO level without exposing email, password, or token")
    void login_Success_ShouldLogWithoutExposingEmailPasswordOrToken() {
        String email = "student.hygiene@example.com";
        String rawPassword = "ValidPassword123!";
        String mockJwtToken = "valid.jwt.token.secret";

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(rawPassword);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "encoded-password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(mockJwtToken);
        when(jwtService.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);

        List<ILoggingEvent> logEvents = authServiceAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected log events during login");

        boolean foundLoginLog = false;
        for (ILoggingEvent event : logEvents) {
            String formattedMessage = event.getFormattedMessage();

            if (event.getLevel() == Level.INFO && formattedMessage.contains("User logged in successfully")) {
                foundLoginLog = true;
            }

            assertFalse(formattedMessage.contains(email), "Log must NEVER contain user email!");
            assertFalse(formattedMessage.contains(rawPassword), "Log must NEVER contain plain text password!");
            assertFalse(formattedMessage.contains(mockJwtToken), "Log must NEVER contain JWT token!");
        }

        assertTrue(foundLoginLog, "Expected to find an INFO log confirming successful login");
    }

    @Test
    @DisplayName("Failed login should log at WARN level without exposing attempted email or password")
    void login_Failure_ShouldLogWarningWithoutExposingAttemptedEmailOrPassword() {
        String email = "attacker@example.com";
        String wrongPassword = "AttackerSecretAttempt123!";

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(wrongPassword);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        List<ILoggingEvent> logEvents = authServiceAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected at least one log event for failed login");

        boolean foundWarnLog = false;
        for (ILoggingEvent event : logEvents) {
            String formattedMessage = event.getFormattedMessage();

            if (event.getLevel() == Level.WARN && formattedMessage.contains("Authentication failed for login attempt")) {
                foundWarnLog = true;
            }

            assertFalse(formattedMessage.contains(email), "Log must NEVER contain attempted email!");
            assertFalse(formattedMessage.contains(wrongPassword), "Log must NEVER contain attempted password!");
        }

        assertTrue(foundWarnLog, "Expected a safe WARN level log identifying failed login attempt without PII");
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should log at WARN level with path only, never logging raw exception message or tokens")
    void commence_ShouldLogWarningWithPathWithoutRawExceptionMessage() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/system");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rawSecretInException = "Malformed-bearer-token-content-xyz";
        entryPoint.commence(request, response, new BadCredentialsException(rawSecretInException));

        List<ILoggingEvent> logEvents = entryPointAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected a log event for unauthorized entry point");

        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("/api/admin/system"));
        assertFalse(event.getFormattedMessage().contains(rawSecretInException),
                "Log must not expose raw exception messages containing potential user input or token data");
    }

    @Test
    @DisplayName("JwtAccessDeniedHandler should log at WARN level with path only, never logging raw exception message")
    void handle_ShouldLogWarningWithPathWithoutRawExceptionMessage() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/placements/1/status");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rawSecretInException = "Internal permission check failed with sensitive role data";
        accessDeniedHandler.handle(request, response, new AccessDeniedException(rawSecretInException));

        List<ILoggingEvent> logEvents = accessDeniedAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected a log event for access denied handler");

        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("/api/placements/1/status"));
        assertFalse(event.getFormattedMessage().contains(rawSecretInException),
                "Log must not expose raw exception messages");
    }

    @Test
    @DisplayName("GlobalExceptionHandler should log 409 conflict at WARN level without echoing sensitive student email/roll in exception message")
    void handleConflict_ShouldLogWarningWithoutExposingDuplicateValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");

        String sensitiveEmail = "existing.user@secret-domain.com";
        DuplicateStudentException conflictException = new DuplicateStudentException(
                "An account with email " + sensitiveEmail + " already exists"
        );

        exceptionHandler.handleConflict(conflictException, request);

        List<ILoggingEvent> logEvents = exceptionHandlerAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected a log event for conflict");

        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("/api/auth/register"));
        assertFalse(event.getFormattedMessage().contains(sensitiveEmail),
                "Conflict log must NEVER echo sensitive email address from exception message!");
    }

    @Test
    @DisplayName("GlobalExceptionHandler should log malformed JSON at DEBUG level without echoing raw payload snippet")
    void handleHttpMessageNotReadable_ShouldLogDebugWithoutExposingPayloadSnippet() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");

        String sensitivePayloadSnippet = "password=SuperSecretPassword123";
        HttpMessageNotReadableException notReadableException = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character in " + sensitivePayloadSnippet,
                new MockHttpInputMessage(new byte[0])
        );

        exceptionHandler.handleHttpMessageNotReadable(notReadableException, request);

        List<ILoggingEvent> logEvents = exceptionHandlerAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected a log event for unreadable HTTP message");

        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.DEBUG, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("/api/auth/login"));
        assertFalse(event.getFormattedMessage().contains(sensitivePayloadSnippet),
                "Malformed JSON log must NEVER echo payload snippets containing passwords or user input!");
    }

    @Test
    @DisplayName("GlobalExceptionHandler should log 500 error at ERROR level with full exception object attached")
    void handleUnexpectedException_ShouldLogErrorWithExceptionStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/jobs/trigger-crash");

        RuntimeException runtimeException = new RuntimeException("Database connection timeout simulation");

        exceptionHandler.handleUnexpectedException(runtimeException, request);

        List<ILoggingEvent> logEvents = exceptionHandlerAppender.list;
        assertFalse(logEvents.isEmpty(), "Expected a log event for unexpected exception");

        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("/api/jobs/trigger-crash"));
        assertTrue(event.getFormattedMessage().contains("Database connection timeout simulation"));
        assertNotNull(event.getThrowableProxy(), "Expected throwable to be attached to error log event");
    }
}

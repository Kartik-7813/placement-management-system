package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.AuditAction;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
    }

    /**
     * Registers a new student account and profile.
     * Enforces duplicate checks for email and roll number, hashes the password via BCrypt,
     * restricts the role strictly to ROLE_STUDENT, and issues a JWT upon completion.
     */
    @Transactional
    public AuthResponse registerStudent(RegisterRequest request) {
        // Duplicate account and profile checks
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateStudentException("An account with email " + request.getEmail() + " already exists");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateStudentException("A student with email " + request.getEmail() + " already exists");
        }
        if (studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateStudentException("A student with roll number " + request.getRollNumber() + " already exists");
        }

        // 1. Create User account with BCrypt-hashed password and hardcoded ROLE_STUDENT
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT
        );
        userRepository.save(user);

        // 2. Create Student profile
        Student student = new Student();
        student.setRollNumber(request.getRollNumber());
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDob(request.getDob());
        student.setGender(request.getGender());
        student.setDepartment(request.getDepartment());
        student.setCgpa(request.getCgpa());
        student.setPassingYear(request.getPassingYear());
        student.setAddress(request.getAddress());
        student.setStatus(StudentStatus.ACTIVE);
        studentRepository.save(student);

        // 3. Generate token for new user
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        log.info("Student registered successfully");

        // 4. Record business audit trail event
        auditService.recordAudit(
                user.getUserId(),
                AuditAction.STUDENT_REGISTERED,
                "STUDENT",
                student.getStudentId(),
                "Student self-registration completed"
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                jwtService.getJwtExpirationMs()
        );
    }

    /**
     * Authenticates an existing user and returns an AuthResponse containing a JWT.
     * Throws BadCredentialsException on invalid credentials without revealing whether the email exists.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for login attempt");
            // Throw generic BadCredentialsException so clients cannot distinguish between missing email vs incorrect password
            throw new BadCredentialsException("Invalid email or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(Role.ROLE_STUDENT.name());

        log.info("User logged in successfully");

        return new AuthResponse(
                token,
                userDetails.getUsername(),
                role,
                jwtService.getJwtExpirationMs()
        );
    }
}

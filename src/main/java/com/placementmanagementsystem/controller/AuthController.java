package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for user registration and JWT authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @Operation(summary = "Register a new student", description = "Public endpoint to register a new student account and receive a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "409", description = "Email or roll number already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerStudent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @SecurityRequirements
    @Operation(summary = "Login user", description = "Public endpoint to authenticate with email and password and receive a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

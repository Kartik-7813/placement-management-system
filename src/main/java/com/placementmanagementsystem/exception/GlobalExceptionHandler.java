package com.placementmanagementsystem.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.placementmanagementsystem.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================
    // 404 - NOT FOUND
    // =========================================================

    @ExceptionHandler({
            StudentNotFoundException.class,
            CompanyNotFoundException.class,
            JobNotFoundException.class,
            ApplicationNotFoundException.class,
            InterviewNotFoundException.class,
            PlacementNotFoundException.class,
            SkillNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    // =========================================================
    // 401 - UNAUTHORIZED / AUTHENTICATION FAILURE
    // =========================================================

    @ExceptionHandler({
            org.springframework.security.authentication.BadCredentialsException.class,
            org.springframework.security.core.AuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                request
        );
    }

    // =========================================================
    // 403 - FORBIDDEN / ACCESS DENIED
    // =========================================================

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Access is denied. You do not have permission to access this resource.",
                request
        );
    }

    // =========================================================
    // 409 - CONFLICT
    // =========================================================

    @ExceptionHandler({
            DuplicateStudentException.class,
            ApplicationAlreadyExistsException.class,
            InterviewAlreadyExistsException.class,
            PlacementAlreadyExistsException.class,
            SkillAlreadyAssignedException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    // =========================================================
    // 400 - BUSINESS VALIDATION
    // =========================================================

    @ExceptionHandler({
            StudentNotEligibleException.class,
            JobNotAcceptingApplicationsException.class,
            ApplicationDeadlinePassedException.class,
            InactiveStudentException.class,
            InactiveCompanyException.class,
            ApplicationNotSelectedException.class,
            PlacementStudentMismatchException.class,
            IllegalStateException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            RuntimeException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    // =========================================================
    // 400 - BEAN VALIDATION
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields",
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // =========================================================
    // 400 - MALFORMED JSON / INVALID FIELD OR ENUM VALUES
    // =========================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        log.debug("Malformed JSON or unreadable request body at path [{}]: {}", request.getRequestURI(), exception.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request or invalid field value/format",
                request
        );
    }

    // =========================================================
    // 400 - PATH VARIABLE / PARAMETER TYPE MISMATCH
    // =========================================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String paramName = exception.getName();
        String expectedType = exception.getRequiredType() != null 
                ? exception.getRequiredType().getSimpleName() 
                : "valid type";

        String message = String.format("Parameter '%s' should be of type %s", paramName, expectedType);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // 400 - MISSING REQUIRED REQUEST PARAMETER
    // =========================================================

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        String message = String.format("Required request parameter '%s' is missing", exception.getParameterName());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    // =========================================================
    // 409 - DATABASE DATA INTEGRITY VIOLATION
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        log.warn("Database integrity violation at path [{}]: {}", request.getRequestURI(), exception.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Database constraint violation: duplicate record or invalid reference data",
                request
        );
    }

    // =========================================================
    // 500 - UNEXPECTED INTERNAL SERVER ERROR (FALLBACK)
    // =========================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unhandled exception processing request to [{}]: {}", request.getRequestURI(), exception.getMessage(), exception);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact support.",
                request
        );
    }

    // =========================================================
    // COMMON ERROR RESPONSE BUILDER
    // =========================================================

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }
}
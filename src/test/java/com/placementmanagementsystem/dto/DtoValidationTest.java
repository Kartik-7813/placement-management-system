package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("JobRequest should fail validation when deadline is in the past")
    void testJobRequestPastDeadlineFails() {
        JobRequest request = new JobRequest(
                1L,
                "Backend Engineer",
                "Description",
                "Eligibility",
                "Java",
                new BigDecimal("7.50"),
                new BigDecimal("1200000.00"),
                "Full-Time",
                LocalDate.now().minusDays(1), // Past deadline
                JobStatus.ACTIVE
        );

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("deadline")));
    }

    @Test
    @DisplayName("JobRequest should fail validation when title exceeds 150 characters")
    void testJobRequestTitleExceedsLengthFails() {
        String longTitle = "A".repeat(151);
        JobRequest request = new JobRequest(
                1L,
                longTitle,
                "Description",
                "Eligibility",
                "Java",
                new BigDecimal("7.50"),
                new BigDecimal("1200000.00"),
                "Full-Time",
                LocalDate.now().plusDays(10),
                JobStatus.ACTIVE
        );

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("CompanyRequest should fail validation when name or phone exceeds max length")
    void testCompanyRequestLengthConstraints() {
        CompanyRequest request = new CompanyRequest(
                "C".repeat(151), // Exceeds 150
                "https://example.com",
                "Contact Person",
                "company@example.com",
                "1234567890123456", // Exceeds 15
                "Address",
                CompanyStatus.ACTIVE
        );

        Set<ConstraintViolation<CompanyRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
    }

    @Test
    @DisplayName("ApplicationRequest and ApplicationUpdateRequest should fail when resumeUrl exceeds 255 chars")
    void testApplicationResumeUrlLength() {
        String longUrl = "https://example.com/" + "u".repeat(250); // Total > 255
        ApplicationRequest req = new ApplicationRequest(1L, 2L, longUrl, "Remarks");
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("resumeUrl")));

        ApplicationUpdateRequest updateReq = new ApplicationUpdateRequest(longUrl, "Remarks");
        Set<ConstraintViolation<ApplicationUpdateRequest>> updateViolations = validator.validate(updateReq);
        assertFalse(updateViolations.isEmpty());
        assertTrue(updateViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("resumeUrl")));
    }

    @Test
    @DisplayName("InterviewRequest should fail when roundType or interviewer exceeds max length")
    void testInterviewRequestLengthConstraints() {
        InterviewRequest request = new InterviewRequest(
                1L,
                1,
                "R".repeat(51), // Exceeds 50
                LocalDateTime.now().plusDays(1),
                "I".repeat(151), // Exceeds 150
                "Feedback",
                InterviewResult.PENDING
        );

        Set<ConstraintViolation<InterviewRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roundType")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("interviewer")));
    }

    @Test
    @DisplayName("PlacementRequest should fail when jobRole exceeds 150 chars or package has invalid digits")
    void testPlacementRequestConstraints() {
        PlacementRequest request = new PlacementRequest(
                1L,
                1L,
                "J".repeat(151), // Exceeds 150
                new BigDecimal("1000000000000.00"), // 13 integer digits (exceeds 10)
                LocalDate.now().plusMonths(1),
                PlacementStatus.CONFIRMED
        );

        Set<ConstraintViolation<PlacementRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("jobRole")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("packageAmount")));
    }

    @Test
    @DisplayName("SkillRequest should fail when name exceeds 100 characters")
    void testSkillRequestLengthConstraint() {
        SkillRequest request = new SkillRequest("S".repeat(101));
        Set<ConstraintViolation<SkillRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
}

package com.placementmanagementsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagementsystem.dto.ApplicationRequest;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.ApplicationUpdateRequest;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.service.ApplicationService;

import jakarta.validation.Valid;

@Tag(name = "Applications", description = "Endpoints for student job applications and status workflows")
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Submit job application", description = "Submit a job application for the authenticated student. Requires STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Student ineligible, deadline passed, or job closed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot apply on behalf of another student"),
            @ApiResponse(responseCode = "404", description = "Student or job not found"),
            @ApiResponse(responseCode = "409", description = "Student has already applied to this job")
    })
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all applications", description = "Retrieve paginated list of applications filtered by role permissions and query parameters. Requires ADMIN, COMPANY, or STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping
    public ResponseEntity<Page<ApplicationResponse>> getAllApplications(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 10, sort = "applicationId", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ApplicationResponse> response = applicationService.getAllApplications(
                studentId, jobId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get application by ID", description = "Retrieve application details by ID. Requires ADMIN, company owner, or applicant STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's application"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update application", description = "Update resume URL or remarks for an application. Requires ADMIN or applicant STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot update another user's application"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
        @PathVariable Long id,
        @Valid @RequestBody ApplicationUpdateRequest request) {

        ApplicationResponse response = applicationService.updateApplication(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update application status", description = "Change application status (e.g. SHORTLISTED, SELECTED, REJECTED). Requires ADMIN or company owner role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application status updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another company's application"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long id, @RequestParam ApplicationStatus status) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, status);
        return ResponseEntity.ok(response);
    }
}

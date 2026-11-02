package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Jobs", description = "Endpoints for posting, searching, and managing job openings")
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @Operation(summary = "Create job", description = "Create a new job posting. Requires ADMIN or COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or deadline in the past"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot create job for another company"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.createJob(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @SecurityRequirements
    @Operation(summary = "Get all jobs", description = "Public endpoint to retrieve paginated job listings with optional filtering by title, company, status, and minCgpa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch")
    })
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) BigDecimal minCgpa,
            @PageableDefault(size = 10, sort = "jobId", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<JobResponse> response = jobService.getAllJobs(title, companyId, status, minCgpa, pageable);
        return ResponseEntity.ok(response);
    }

    @SecurityRequirements
    @Operation(summary = "Get job by ID", description = "Public endpoint to retrieve details of a specific job.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job found"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update job", description = "Update an existing job posting. Requires ADMIN or owning COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid deadline change"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot update another company's job"),
            @ApiResponse(responseCode = "404", description = "Job or company not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.updateJob(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Close job", description = "Close a job posting so it no longer accepts applications. Requires ADMIN or owning COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job closed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot close another company's job"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> closeJob(@PathVariable Long id) {
        jobService.closeJob(id);
        return ResponseEntity.noContent().build();
    }
}

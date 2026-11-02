package com.placementmanagementsystem.controller;

import java.util.List;

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

import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.service.InterviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Interviews", description = "Endpoints for scheduling, updating, and tracking interview rounds")
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // =========================================================
    // CREATE / SCHEDULE INTERVIEW
    // POST /api/interviews
    // =========================================================

    @Operation(summary = "Schedule interview", description = "Schedule an interview round for an application. Requires ADMIN or owning COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Interview scheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot schedule for another company's application"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Interview round already scheduled")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PostMapping
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @Valid @RequestBody InterviewRequest request) {

        InterviewResponse response =
                interviewService.scheduleInterview(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================
    // GET ALL INTERVIEWS
    // GET /api/interviews
    // =========================================================

    @Operation(summary = "Get all interviews", description = "Retrieve paginated list of interviews with optional filters. Requires ADMIN or COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interviews retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or COMPANY role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @GetMapping
    public ResponseEntity<Page<InterviewResponse>> getAllInterviews(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) InterviewResult result,
            @PageableDefault(
                    size = 10,
                    sort = "interviewId",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        Page<InterviewResponse> response =
                interviewService.getAllInterviews(
                        applicationId,
                        result,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET INTERVIEW BY ID
    // GET /api/interviews/{id}
    // =========================================================

    @Operation(summary = "Get interview by ID", description = "Retrieve interview details by ID. Requires ADMIN, owning COMPANY, or applicant STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's interview"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterviewById(
            @PathVariable Long id) {

        InterviewResponse response =
                interviewService.getInterviewById(id);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET INTERVIEWS FOR APPLICATION
    // GET /api/interviews/application/{applicationId}
    // =========================================================

    @Operation(summary = "Get interviews for application", description = "List all interview rounds for a specific application. Requires ADMIN, owning COMPANY, or applicant STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interviews retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's interviews"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewResponse>> getInterviewsByApplication(
            @PathVariable Long applicationId) {

        List<InterviewResponse> response =
                interviewService.getInterviewsByApplication(applicationId);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE INTERVIEW
    // PUT /api/interviews/{id}
    // =========================================================

    @Operation(summary = "Update interview", description = "Update interview schedule, feedback, or result. Requires ADMIN or owning COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interview updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot update another company's interview"),
            @ApiResponse(responseCode = "404", description = "Interview not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate interview round number")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(
            @PathVariable Long id,
            @Valid @RequestBody InterviewRequest request) {

        InterviewResponse response =
                interviewService.updateInterview(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // DELETE INTERVIEW
    // DELETE /api/interviews/{id}
    // =========================================================

    @Operation(summary = "Delete interview", description = "Delete an interview record. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Interview deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Interview not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long id) {

        interviewService.deleteInterview(id);

        return ResponseEntity.noContent().build();
    }
}
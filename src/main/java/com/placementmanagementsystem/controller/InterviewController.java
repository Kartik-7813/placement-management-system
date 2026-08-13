package com.placementmanagementsystem.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long id) {

        interviewService.deleteInterview(id);

        return ResponseEntity.noContent().build();
    }
}
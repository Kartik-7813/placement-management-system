package com.placementmanagementsystem.controller;

import java.util.List;

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

import com.placementmanagementsystem.dto.PlacementRequest;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.service.PlacementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Placements", description = "Endpoints for managing job placements, offers, and student placement records")
@RestController
@RequestMapping("/api/placements")
public class PlacementController {

    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    // =========================================================
    // CREATE PLACEMENT
    // =========================================================

    @Operation(summary = "Create placement record", description = "Record a confirmed placement for a selected student application. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Placement created successfully"),
            @ApiResponse(responseCode = "400", description = "Application not selected or student mismatch"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Student or application not found"),
            @ApiResponse(responseCode = "409", description = "Placement already exists for application or student already placed")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlacementResponse> createPlacement(
            @Valid @RequestBody PlacementRequest request) {

        PlacementResponse response =
                placementService.createPlacement(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    // =========================================================
    // GET ALL PLACEMENTS
    // =========================================================

    @Operation(summary = "Get all placements", description = "Retrieve paginated list of placements with optional status filter. Requires ADMIN or COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placements retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or COMPANY role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @GetMapping
    public ResponseEntity<Page<PlacementResponse>> getAllPlacements(
            @RequestParam(required = false) PlacementStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "placementId",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        Page<PlacementResponse> response =
                placementService.getAllPlacements(
                        status,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET PLACEMENT BY ID
    // =========================================================

    @Operation(summary = "Get placement by ID", description = "Retrieve placement details by ID. Requires ADMIN, company owner, or placed STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placement found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another user's placement"),
            @ApiResponse(responseCode = "404", description = "Placement not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<PlacementResponse> getPlacementById(
            @PathVariable Long id) {

        PlacementResponse response =
                placementService.getPlacementById(id);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET PLACEMENT BY STUDENT
    // =========================================================

    @Operation(summary = "Get placement by student ID", description = "Retrieve confirmed placement for a student. Requires ADMIN, COMPANY, or placed STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placement found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another student's placement"),
            @ApiResponse(responseCode = "404", description = "Placement or student not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<PlacementResponse> getPlacementByStudent(
            @PathVariable Long studentId) {

        PlacementResponse response =
                placementService.getPlacementByStudent(studentId);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET PLACEMENT HISTORY BY STUDENT
    // =========================================================

    @Operation(summary = "Get placement history for student", description = "Retrieve placement history for a student. Requires ADMIN, COMPANY, or placed STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placement history retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another student's history"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/student/{studentId}/history")
    public ResponseEntity<List<PlacementResponse>> getPlacementHistoryByStudent(
            @PathVariable Long studentId) {

        List<PlacementResponse> response =
                placementService.getPlacementHistoryByStudent(studentId);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE PLACEMENT DETAILS
    // =========================================================

    @Operation(summary = "Update placement details", description = "Update role, package, or joining date of a placement. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placement updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Placement not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PlacementResponse> updatePlacement(
            @PathVariable Long id,
            @Valid @RequestBody PlacementRequest request) {

        PlacementResponse response =
                placementService.updatePlacement(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE PLACEMENT STATUS
    // =========================================================

    @Operation(summary = "Update placement status", description = "Update placement status (e.g. CONFIRMED, JOINED, WITHDRAWN). Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Placement status updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Placement not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<PlacementResponse> updatePlacementStatus(
            @PathVariable Long id,
            @RequestParam PlacementStatus status) {

        PlacementResponse response =
                placementService.updatePlacementStatus(
                        id,
                        status
                );

        return ResponseEntity.ok(response);
    }
}
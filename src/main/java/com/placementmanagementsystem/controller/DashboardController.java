package com.placementmanagementsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.placementmanagementsystem.dto.DashboardResponse;
import com.placementmanagementsystem.service.DashboardService;

@Tag(name = "Dashboard", description = "Endpoints for placement analytics and administrative reporting")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get placement dashboard summary", description = "Retrieve aggregated placement statistics including total students, placed students, total jobs, and top packages. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> getDashboardSummary() {

        DashboardResponse response =
                dashboardService.getDashboardSummary();

        return ResponseEntity.ok(response);
    }
}
package com.placementmanagementsystem.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

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

    @GetMapping("/student/{studentId}")
    public ResponseEntity<PlacementResponse> getPlacementByStudent(
            @PathVariable Long studentId) {

        PlacementResponse response =
                placementService.getPlacementByStudent(studentId);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE PLACEMENT DETAILS
    // =========================================================

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
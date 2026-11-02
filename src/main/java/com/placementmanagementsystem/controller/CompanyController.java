package com.placementmanagementsystem.controller;

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

import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.service.CompanyService;

@Tag(name = "Companies", description = "Endpoints for managing registered companies and recruiting organizations")
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Create company", description = "Register a new recruiting company. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Company created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Company with email already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyRequest request) {
        CompanyResponse response = companyService.createCompany(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @SecurityRequirements
    @Operation(summary = "Get all companies", description = "Public endpoint to retrieve paginated list of companies with optional name and status filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch")
    })
    @GetMapping
    public ResponseEntity<Page<CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CompanyStatus status,
            @PageableDefault(size = 10, sort = "companyId", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CompanyResponse> response = companyService.getAllCompanies(name, status, pageable);
        return ResponseEntity.ok(response);
    }

    @SecurityRequirements
    @Operation(summary = "Get company by ID", description = "Public endpoint to retrieve company details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company found"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable Long id) {
        CompanyResponse response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update company", description = "Update company profile. Requires ADMIN or owning COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot update another company"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        CompanyResponse response = companyService.updateCompany(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate company", description = "Deactivate a company. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Company deactivated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCompany(@PathVariable Long id) {
        companyService.deactivateCompany(id);
        return ResponseEntity.noContent().build();
    }
}

package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.PlacementStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PlacementRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    private String jobRole;

    @PositiveOrZero(message = "Package must be non-negative")
    private BigDecimal packageAmount;

    private LocalDate joiningDate;
    private PlacementStatus status;

    public PlacementRequest() {
    }

    public PlacementRequest(Long studentId, Long applicationId, String jobRole, BigDecimal packageAmount, LocalDate joiningDate, PlacementStatus status) {
        this.studentId = studentId;
        this.applicationId = applicationId;
        this.jobRole = jobRole;
        this.packageAmount = packageAmount;
        this.joiningDate = joiningDate;
        this.status = status;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public BigDecimal getPackageAmount() {
        return packageAmount;
    }

    public void setPackageAmount(BigDecimal packageAmount) {
        this.packageAmount = packageAmount;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public PlacementStatus getStatus() {
        return status;
    }

    public void setStatus(PlacementStatus status) {
        this.status = status;
    }
}

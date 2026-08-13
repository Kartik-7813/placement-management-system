package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.PlacementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PlacementResponse {

    private Long placementId;
    private Long studentId;
    private String studentName;
    private Long applicationId;
    private String jobRole;
    private BigDecimal packageAmount;
    private LocalDate joiningDate;
    private PlacementStatus status;
    private LocalDateTime placedOn;

    public PlacementResponse() {
    }

    public PlacementResponse(Long placementId, Long studentId, String studentName, Long applicationId, String jobRole, BigDecimal packageAmount, LocalDate joiningDate, PlacementStatus status, LocalDateTime placedOn) {
        this.placementId = placementId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.applicationId = applicationId;
        this.jobRole = jobRole;
        this.packageAmount = packageAmount;
        this.joiningDate = joiningDate;
        this.status = status;
        this.placedOn = placedOn;
    }

    public Long getPlacementId() {
        return placementId;
    }

    public void setPlacementId(Long placementId) {
        this.placementId = placementId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public LocalDateTime getPlacedOn() {
        return placedOn;
    }

    public void setPlacedOn(LocalDateTime placedOn) {
        this.placedOn = placedOn;
    }
}

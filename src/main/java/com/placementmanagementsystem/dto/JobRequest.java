package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.JobStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class JobRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Job title is required")
    private String title;

    private String description;
    private String eligibilityCriteria;
    private String requiredSkills;

    @DecimalMin(value = "0.00", message = "Minimum CGPA must be at least 0.0")
    @DecimalMax(value = "10.00", message = "Minimum CGPA cannot exceed 10.0")
    private BigDecimal minCgpa;

    @PositiveOrZero(message = "Package must be non-negative")
    private BigDecimal packageAmount;

    private String jobType;
    private LocalDate deadline;
    private JobStatus status;

    public JobRequest() {
    }

    public JobRequest(Long companyId, String title, String description, String eligibilityCriteria, String requiredSkills, BigDecimal minCgpa, BigDecimal packageAmount, String jobType, LocalDate deadline, JobStatus status) {
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.eligibilityCriteria = eligibilityCriteria;
        this.requiredSkills = requiredSkills;
        this.minCgpa = minCgpa;
        this.packageAmount = packageAmount;
        this.jobType = jobType;
        this.deadline = deadline;
        this.status = status;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEligibilityCriteria() {
        return eligibilityCriteria;
    }

    public void setEligibilityCriteria(String eligibilityCriteria) {
        this.eligibilityCriteria = eligibilityCriteria;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public BigDecimal getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(BigDecimal minCgpa) {
        this.minCgpa = minCgpa;
    }

    public BigDecimal getPackageAmount() {
        return packageAmount;
    }

    public void setPackageAmount(BigDecimal packageAmount) {
        this.packageAmount = packageAmount;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }
}

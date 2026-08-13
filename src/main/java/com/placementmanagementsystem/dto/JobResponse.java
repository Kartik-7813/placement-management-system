package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class JobResponse {

    private Long jobId;
    private Long companyId;
    private String companyName;
    private String title;
    private String description;
    private String eligibilityCriteria;
    private String requiredSkills;
    private BigDecimal minCgpa;
    private BigDecimal packageAmount;
    private String jobType;
    private LocalDate deadline;
    private JobStatus status;

    public JobResponse() {
    }

    public JobResponse(Long jobId, Long companyId, String companyName, String title, String description, String eligibilityCriteria, String requiredSkills, BigDecimal minCgpa, BigDecimal packageAmount, String jobType, LocalDate deadline, JobStatus status) {
        this.jobId = jobId;
        this.companyId = companyId;
        this.companyName = companyName;
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

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

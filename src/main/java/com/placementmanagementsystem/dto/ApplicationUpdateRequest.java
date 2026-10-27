package com.placementmanagementsystem.dto;

import jakarta.validation.constraints.Size;

public class ApplicationUpdateRequest {

    @Size(max = 255, message = "Resume URL cannot exceed 255 characters")
    private String resumeUrl;

    private String remarks;

    public ApplicationUpdateRequest() {
    }

    public ApplicationUpdateRequest(String resumeUrl, String remarks) {
        this.resumeUrl = resumeUrl;
        this.remarks = remarks;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

package com.placementmanagementsystem.dto;

import java.time.LocalDateTime;

import com.placementmanagementsystem.enums.InterviewResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class InterviewRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;


    @NotNull(message = "Round number is required")
    @Positive(message = "Round number must be positive")
    private Integer roundNumber;

    @NotBlank(message = "Round type is required")
    private String roundType;

    @NotNull(message = "Scheduled date and time are required")
    private LocalDateTime scheduledOn;
    
   
    private String interviewer;
    
    private String feedback;
    
    private InterviewResult result;

    public InterviewRequest() {
    }

    public InterviewRequest(Long applicationId, Integer roundNumber, String roundType, LocalDateTime scheduledOn, String interviewer, String feedback, InterviewResult result) {
        this.applicationId = applicationId;
        this.roundNumber = roundNumber;
        this.roundType = roundType;
        this.scheduledOn = scheduledOn;
        this.interviewer = interviewer;
        this.feedback = feedback;
        this.result = result;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundType() {
        return roundType;
    }

    public void setRoundType(String roundType) {
        this.roundType = roundType;
    }

    public LocalDateTime getScheduledOn() {
        return scheduledOn;
    }

    public void setScheduledOn(LocalDateTime scheduledOn) {
        this.scheduledOn = scheduledOn;
    }

    public String getInterviewer() {
        return interviewer;
    }

    public void setInterviewer(String interviewer) {
        this.interviewer = interviewer;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public InterviewResult getResult() {
        return result;
    }

    public void setResult(InterviewResult result) {
        this.result = result;
    }
}

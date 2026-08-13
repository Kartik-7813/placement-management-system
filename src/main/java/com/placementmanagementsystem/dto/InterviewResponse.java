package com.placementmanagementsystem.dto;

import com.placementmanagementsystem.enums.InterviewResult;

import java.time.LocalDateTime;

public class InterviewResponse {

    private Long interviewId;
    private Long applicationId;
    private Integer roundNumber;
    private String roundType;
    private LocalDateTime scheduledOn;
    private String interviewer;
    private String feedback;
    private InterviewResult result;
    private LocalDateTime createdAt;

    public InterviewResponse() {
    }

    public InterviewResponse(Long interviewId, Long applicationId, Integer roundNumber, String roundType, LocalDateTime scheduledOn, String interviewer, String feedback, InterviewResult result, LocalDateTime createdAt) {
        this.interviewId = interviewId;
        this.applicationId = applicationId;
        this.roundNumber = roundNumber;
        this.roundType = roundType;
        this.scheduledOn = scheduledOn;
        this.interviewer = interviewer;
        this.feedback = feedback;
        this.result = result;
        this.createdAt = createdAt;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

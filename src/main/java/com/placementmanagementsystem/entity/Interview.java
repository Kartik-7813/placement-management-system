package com.placementmanagementsystem.entity;

import java.time.LocalDateTime;

import com.placementmanagementsystem.enums.InterviewResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_id")
    private Long interviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "round_type", length = 50)
    private String roundType;

    @Column(name = "scheduled_on")
    private LocalDateTime scheduledOn;

    @Column(name = "interviewer", length = 150)
    private String interviewer;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 30)
    private InterviewResult result;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Interview() {
    }

    public Interview(Long interviewId, Application application, Integer roundNumber, String roundType, LocalDateTime scheduledOn, String interviewer, String feedback, InterviewResult result, LocalDateTime createdAt) {
        this.interviewId = interviewId;
        this.application = application;
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

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
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

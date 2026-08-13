package com.placementmanagementsystem.dto;

public class DashboardResponse {

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;

    private long totalCompanies;
    private long activeCompanies;
    private long inactiveCompanies;

    private long totalJobs;
    private long activeJobs;
    private long closedJobs;

    private long totalApplications;
    private long appliedApplications;
    private long shortlistedApplications;
    private long interviewingApplications;
    private long selectedApplications;
    private long rejectedApplications;
    private long withdrawnApplications;

    private long totalPlacements;
    private long confirmedPlacements;
    private long joinedPlacements;
    private long cancelledPlacements;

    public DashboardResponse() {
    }

    public DashboardResponse(
            long totalStudents,
            long activeStudents,
            long inactiveStudents,
            long totalCompanies,
            long activeCompanies,
            long inactiveCompanies,
            long totalJobs,
            long activeJobs,
            long closedJobs,
            long totalApplications,
            long appliedApplications,
            long shortlistedApplications,
            long interviewingApplications,
            long selectedApplications,
            long rejectedApplications,
            long withdrawnApplications,
            long totalPlacements,
            long confirmedPlacements,
            long joinedPlacements,
            long cancelledPlacements) {

        this.totalStudents = totalStudents;
        this.activeStudents = activeStudents;
        this.inactiveStudents = inactiveStudents;

        this.totalCompanies = totalCompanies;
        this.activeCompanies = activeCompanies;
        this.inactiveCompanies = inactiveCompanies;

        this.totalJobs = totalJobs;
        this.activeJobs = activeJobs;
        this.closedJobs = closedJobs;

        this.totalApplications = totalApplications;
        this.appliedApplications = appliedApplications;
        this.shortlistedApplications = shortlistedApplications;
        this.interviewingApplications = interviewingApplications;
        this.selectedApplications = selectedApplications;
        this.rejectedApplications = rejectedApplications;
        this.withdrawnApplications = withdrawnApplications;

        this.totalPlacements = totalPlacements;
        this.confirmedPlacements = confirmedPlacements;
        this.joinedPlacements = joinedPlacements;
        this.cancelledPlacements = cancelledPlacements;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getActiveStudents() {
        return activeStudents;
    }

    public void setActiveStudents(long activeStudents) {
        this.activeStudents = activeStudents;
    }

    public long getInactiveStudents() {
        return inactiveStudents;
    }

    public void setInactiveStudents(long inactiveStudents) {
        this.inactiveStudents = inactiveStudents;
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public long getActiveCompanies() {
        return activeCompanies;
    }

    public void setActiveCompanies(long activeCompanies) {
        this.activeCompanies = activeCompanies;
    }

    public long getInactiveCompanies() {
        return inactiveCompanies;
    }

    public void setInactiveCompanies(long inactiveCompanies) {
        this.inactiveCompanies = inactiveCompanies;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(long activeJobs) {
        this.activeJobs = activeJobs;
    }

    public long getClosedJobs() {
        return closedJobs;
    }

    public void setClosedJobs(long closedJobs) {
        this.closedJobs = closedJobs;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getAppliedApplications() {
        return appliedApplications;
    }

    public void setAppliedApplications(long appliedApplications) {
        this.appliedApplications = appliedApplications;
    }

    public long getShortlistedApplications() {
        return shortlistedApplications;
    }

    public void setShortlistedApplications(long shortlistedApplications) {
        this.shortlistedApplications = shortlistedApplications;
    }

    public long getInterviewingApplications() {
        return interviewingApplications;
    }

    public void setInterviewingApplications(long interviewingApplications) {
        this.interviewingApplications = interviewingApplications;
    }

    public long getSelectedApplications() {
        return selectedApplications;
    }

    public void setSelectedApplications(long selectedApplications) {
        this.selectedApplications = selectedApplications;
    }

    public long getRejectedApplications() {
        return rejectedApplications;
    }

    public void setRejectedApplications(long rejectedApplications) {
        this.rejectedApplications = rejectedApplications;
    }

    public long getWithdrawnApplications() {
        return withdrawnApplications;
    }

    public void setWithdrawnApplications(long withdrawnApplications) {
        this.withdrawnApplications = withdrawnApplications;
    }

    public long getTotalPlacements() {
        return totalPlacements;
    }

    public void setTotalPlacements(long totalPlacements) {
        this.totalPlacements = totalPlacements;
    }

    public long getConfirmedPlacements() {
        return confirmedPlacements;
    }

    public void setConfirmedPlacements(long confirmedPlacements) {
        this.confirmedPlacements = confirmedPlacements;
    }

    public long getJoinedPlacements() {
        return joinedPlacements;
    }

    public void setJoinedPlacements(long joinedPlacements) {
        this.joinedPlacements = joinedPlacements;
    }

    public long getCancelledPlacements() {
        return cancelledPlacements;
    }

    public void setCancelledPlacements(long cancelledPlacements) {
        this.cancelledPlacements = cancelledPlacements;
    }
}
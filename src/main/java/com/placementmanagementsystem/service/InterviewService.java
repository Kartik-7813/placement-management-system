package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.InterviewAlreadyExistsException;
import com.placementmanagementsystem.exception.InterviewNotFoundException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.InterviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    public InterviewService(
            InterviewRepository interviewRepository,
            ApplicationRepository applicationRepository) {

        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    // =========================================================
    // CREATE / SCHEDULE INTERVIEW
    // =========================================================

    public InterviewResponse scheduleInterview(InterviewRequest request) {

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + request.getApplicationId()));

        // Interview can only be scheduled for shortlisted/interviewing applications
        if (application.getStatus() != ApplicationStatus.SHORTLISTED
                && application.getStatus() != ApplicationStatus.INTERVIEWING) {

            throw new IllegalStateException(
                    "Interview cannot be scheduled for application with status: "
                            + application.getStatus());
        }

        // Prevent duplicate round
        if (interviewRepository.existsByApplication_ApplicationIdAndRoundNumber(
                request.getApplicationId(),
                request.getRoundNumber())) {

            throw new InterviewAlreadyExistsException(
                    "Interview round " + request.getRoundNumber()
                            + " already exists for application id: "
                            + request.getApplicationId());
        }

        Interview interview = new Interview();

        interview.setApplication(application);
        interview.setRoundNumber(request.getRoundNumber());
        interview.setRoundType(request.getRoundType());
        interview.setScheduledOn(request.getScheduledOn());
        interview.setInterviewer(request.getInterviewer());
        interview.setFeedback(request.getFeedback());

        // New interviews are PENDING by default
        interview.setResult(
                request.getResult() != null
                        ? request.getResult()
                        : InterviewResult.PENDING
        );

        interview.setCreatedAt(LocalDateTime.now());

        // Once the first interview is scheduled,
        // application enters INTERVIEWING.
        if (application.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.setStatus(ApplicationStatus.INTERVIEWING);
            applicationRepository.save(application);
        }

        Interview savedInterview = interviewRepository.save(interview);

        return toResponse(savedInterview);
    }

    // =========================================================
    // GET ALL INTERVIEWS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<InterviewResponse> getAllInterviews(
            Long applicationId,
            InterviewResult result,
            Pageable pageable) {

        Page<Interview> interviews =
                interviewRepository.searchInterviews(
                        applicationId,
                        result,
                        pageable
                );

        return interviews.map(this::toResponse);
    }

    // =========================================================
    // GET INTERVIEW BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long id) {

        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new InterviewNotFoundException(
                        "Interview not found with id: " + id));

        return toResponse(interview);
    }

    // =========================================================
    // GET INTERVIEWS FOR APPLICATION
    // =========================================================

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(
            Long applicationId) {

        // First verify application exists
        applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + applicationId));

        List<Interview> interviews =
                interviewRepository
                        .findByApplication_ApplicationIdOrderByRoundNumberAsc(
                                applicationId
                        );

        return interviews.stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // UPDATE INTERVIEW
    // =========================================================

    public InterviewResponse updateInterview(
            Long id,
            InterviewRequest request) {

        Interview existingInterview = interviewRepository.findById(id)
                .orElseThrow(() -> new InterviewNotFoundException(
                        "Interview not found with id: " + id));

        // Do not allow changing application relationship
        // through the update request.

        existingInterview.setRoundNumber(request.getRoundNumber());
        existingInterview.setRoundType(request.getRoundType());
        existingInterview.setScheduledOn(request.getScheduledOn());
        existingInterview.setInterviewer(request.getInterviewer());
        existingInterview.setFeedback(request.getFeedback());

        if (request.getResult() != null) {
            existingInterview.setResult(request.getResult());
        }

        Interview updatedInterview =
                interviewRepository.save(existingInterview);

        return toResponse(updatedInterview);
    }

    // =========================================================
    // DELETE INTERVIEW
    // =========================================================

    public void deleteInterview(Long id) {

        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new InterviewNotFoundException(
                        "Interview not found with id: " + id));

        interviewRepository.delete(interview);
    }

    // =========================================================
    // ENTITY → DTO
    // =========================================================

    private InterviewResponse toResponse(Interview interview) {

        return new InterviewResponse(
                interview.getInterviewId(),
                interview.getApplication().getApplicationId(),
                interview.getRoundNumber(),
                interview.getRoundType(),
                interview.getScheduledOn(),
                interview.getInterviewer(),
                interview.getFeedback(),
                interview.getResult(),
                interview.getCreatedAt()
        );
    }
}
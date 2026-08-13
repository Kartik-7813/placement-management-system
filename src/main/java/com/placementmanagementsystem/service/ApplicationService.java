package com.placementmanagementsystem.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagementsystem.dto.ApplicationRequest;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.ApplicationUpdateRequest;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.ApplicationAlreadyExistsException;
import com.placementmanagementsystem.exception.ApplicationDeadlinePassedException;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.InactiveStudentException;
import com.placementmanagementsystem.exception.JobNotAcceptingApplicationsException;
import com.placementmanagementsystem.exception.JobNotFoundException;
import com.placementmanagementsystem.exception.StudentNotEligibleException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.StudentRepository;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              StudentRepository studentRepository,
                              JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.jobRepository = jobRepository;
    }

    // ========== CREATE APPLICATION ==========

    public ApplicationResponse createApplication(ApplicationRequest request) {

        // 1. Student must exist
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student not found with id: " + request.getStudentId()));

        // 2. Student must be active
        if (student.getStatus() == StudentStatus.INACTIVE) {
            throw new InactiveStudentException(
                    "Inactive student cannot submit applications (student id: " + request.getStudentId() + ")");
        }

        // 3. Job must exist
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new JobNotFoundException(
                        "Job not found with id: " + request.getJobId()));

        // 4. Job must be ACTIVE
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new JobNotAcceptingApplicationsException(
                    "Job is not accepting applications (job id: " + request.getJobId()
                            + ", status: " + job.getStatus() + ")");
        }

        // 5. Check application deadline
        if (job.getDeadline() != null && LocalDate.now().isAfter(job.getDeadline())) {
            throw new ApplicationDeadlinePassedException(
                    "Application deadline has passed for this job (deadline: " + job.getDeadline() + ")");
        }

        // 6. Check CGPA eligibility
        if (job.getMinCgpa() != null) {
            if (student.getCgpa() == null) {
                throw new StudentNotEligibleException(
                        "Student CGPA is not available, but this job requires a minimum CGPA of "
                                + job.getMinCgpa());
            }
            if (student.getCgpa().compareTo(job.getMinCgpa()) < 0) {
                throw new StudentNotEligibleException(
                        "Student CGPA (" + student.getCgpa()
                                + ") is below the minimum required CGPA (" + job.getMinCgpa()
                                + ") for this job");
            }
        }

        // 7. Check duplicate application
        if (applicationRepository.existsByStudent_StudentIdAndJob_JobId(
                request.getStudentId(), request.getJobId())) {
            throw new ApplicationAlreadyExistsException(
                    "Student (id: " + request.getStudentId()
                            + ") has already applied to this job (id: " + request.getJobId() + ")");
        }

        // 8. Create Application entity
        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setAppliedOn(LocalDateTime.now());
        application.setStatus(ApplicationStatus.APPLIED);
        application.setResumeUrl(request.getResumeUrl());
        application.setRemarks(request.getRemarks());

        // 9. Save
        Application savedApplication = applicationRepository.save(application);

        // 10. Convert to response
        return toResponse(savedApplication);
    }

    // ========== GET ALL APPLICATIONS ==========

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getAllApplications(Long studentId, Long jobId,
                                                        ApplicationStatus status, Pageable pageable) {
        Page<Application> applications = applicationRepository.searchApplications(
                studentId, jobId, status, pageable);
        return applications.map(this::toResponse);
    }

    // ========== GET APPLICATION BY ID ==========

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));
        return toResponse(application);
    }

    // ========== UPDATE APPLICATION ==========

    public ApplicationResponse updateApplication(Long id, ApplicationUpdateRequest request) {
        Application existingApplication = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));

        // Only allow updating resumeUrl and remarks
        existingApplication.setResumeUrl(request.getResumeUrl());
        existingApplication.setRemarks(request.getRemarks());

        Application updatedApplication = applicationRepository.save(existingApplication);
        return toResponse(updatedApplication);
    }

    // ========== UPDATE APPLICATION STATUS (controlled workflow) ==========

    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus newStatus) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + id));

        validateStatusTransition(application.getStatus(), newStatus);

        application.setStatus(newStatus);
        Application updatedApplication = applicationRepository.save(application);
        return toResponse(updatedApplication);
    }

    // ========== STATUS TRANSITION VALIDATION ==========

    private void validateStatusTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        boolean valid = false;

        switch (currentStatus) {
            
            case APPLIED -> // APPLIED -> SHORTLISTED, REJECTED, WITHDRAWN
                valid = (newStatus == ApplicationStatus.SHORTLISTED
                        || newStatus == ApplicationStatus.REJECTED
                        || newStatus == ApplicationStatus.WITHDRAWN);

            case SHORTLISTED -> // SHORTLISTED -> INTERVIEWING, REJECTED, WITHDRAWN
                valid = (newStatus == ApplicationStatus.INTERVIEWING
                        || newStatus == ApplicationStatus.REJECTED
                        || newStatus == ApplicationStatus.WITHDRAWN);

            case INTERVIEWING -> // INTERVIEWING -> REJECTED only (SELECTED is handled by later workflows)
                valid = (newStatus == ApplicationStatus.SELECTED || newStatus == ApplicationStatus.REJECTED);

            case SELECTED, REJECTED, WITHDRAWN -> // Terminal states — no further transitions allowed
                valid = false;
        }

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition: " + currentStatus + " → " + newStatus);
        }
    }

    // ========== ENTITY → DTO MAPPING ==========

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getApplicationId(),
                application.getStudent().getStudentId(),
                application.getStudent().getName(),
                application.getJob().getJobId(),
                application.getJob().getTitle(),
                application.getJob().getCompany().getName(),
                application.getAppliedOn(),
                application.getStatus(),
                application.getResumeUrl(),
                application.getRemarks()
        );
    }
}

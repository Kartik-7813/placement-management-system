package com.placementmanagementsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagementsystem.dto.DashboardResponse;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.PlacementRepository;
import com.placementmanagementsystem.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final PlacementRepository placementRepository;

    public DashboardService(
            StudentRepository studentRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            PlacementRepository placementRepository) {

        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.placementRepository = placementRepository;
    }

    public DashboardResponse getDashboardSummary() {

        // =========================
        // STUDENTS
        // =========================

        long totalStudents = studentRepository.count();

        long activeStudents =
                studentRepository.countByStatus(StudentStatus.ACTIVE);

        long inactiveStudents =
                studentRepository.countByStatus(StudentStatus.INACTIVE);

        // =========================
        // COMPANIES
        // =========================

        long totalCompanies = companyRepository.count();

        long activeCompanies =
                companyRepository.countByStatus(CompanyStatus.ACTIVE);

        long inactiveCompanies =
                companyRepository.countByStatus(CompanyStatus.INACTIVE);

        // =========================
        // JOBS
        // =========================

        long totalJobs = jobRepository.count();

        long activeJobs =
                jobRepository.countByStatus(JobStatus.ACTIVE);

        long closedJobs =
                jobRepository.countByStatus(JobStatus.CLOSED);

        // =========================
        // APPLICATIONS
        // =========================

        long totalApplications = applicationRepository.count();

        long appliedApplications =
                applicationRepository.countByStatus(ApplicationStatus.APPLIED);

        long shortlistedApplications =
                applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED);

        long interviewingApplications =
                applicationRepository.countByStatus(ApplicationStatus.INTERVIEWING);

        long selectedApplications =
                applicationRepository.countByStatus(ApplicationStatus.SELECTED);

        long rejectedApplications =
                applicationRepository.countByStatus(ApplicationStatus.REJECTED);

        long withdrawnApplications =
                applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN);

        // =========================
        // PLACEMENTS
        // =========================

        long totalPlacements = placementRepository.count();

        long confirmedPlacements =
                placementRepository.countByStatus(PlacementStatus.CONFIRMED);

        long joinedPlacements =
                placementRepository.countByStatus(PlacementStatus.JOINED);

        long cancelledPlacements =
                placementRepository.countByStatus(PlacementStatus.CANCELLED);

        // =========================
        // RESPONSE
        // =========================

        return new DashboardResponse(
                totalStudents,
                activeStudents,
                inactiveStudents,

                totalCompanies,
                activeCompanies,
                inactiveCompanies,

                totalJobs,
                activeJobs,
                closedJobs,

                totalApplications,
                appliedApplications,
                shortlistedApplications,
                interviewingApplications,
                selectedApplications,
                rejectedApplications,
                withdrawnApplications,

                totalPlacements,
                confirmedPlacements,
                joinedPlacements,
                cancelledPlacements
        );
    }
}
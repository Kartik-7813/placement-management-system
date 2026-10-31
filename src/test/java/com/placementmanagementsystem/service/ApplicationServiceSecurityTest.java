package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.ApplicationRequest;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.ApplicationUpdateRequest;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.InactiveCompanyException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService Security & Ownership Unit Tests")
class ApplicationServiceSecurityTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private ApplicationService applicationService;

    private Student mockStudent;
    private Job mockJob;
    private Application mockApplication;

    @BeforeEach
    void setUp() {
        mockStudent = new Student();
        mockStudent.setStudentId(1L);
        mockStudent.setName("Alice");
        mockStudent.setEmail("alice@example.com");
        mockStudent.setStatus(StudentStatus.ACTIVE);

        Company company = new Company();
        company.setCompanyId(10L);
        company.setName("TechCorp");

        mockJob = new Job();
        mockJob.setJobId(100L);
        mockJob.setTitle("Backend Engineer");
        mockJob.setStatus(JobStatus.ACTIVE);
        mockJob.setCompany(company);

        mockApplication = new Application();
        mockApplication.setApplicationId(50L);
        mockApplication.setStudent(mockStudent);
        mockApplication.setJob(mockJob);
        mockApplication.setAppliedOn(LocalDateTime.now());
        mockApplication.setStatus(ApplicationStatus.APPLIED);
        mockApplication.setResumeUrl("https://example.com/resume.pdf");
    }

    @Test
    @DisplayName("getAllApplications forces studentId to authenticated student when caller has ROLE_STUDENT")
    void getAllApplications_WhenRoleStudent_ForcesOwnStudentId() {
        when(securityHelper.hasAuthority("ROLE_STUDENT")).thenReturn(true);
        when(securityHelper.getCurrentStudent()).thenReturn(mockStudent);
        when(applicationRepository.searchApplications(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockApplication)));

        var result = applicationService.getAllApplications(null, null, null, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(applicationRepository).searchApplications(eq(1L), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("getAllApplications throws AccessDeniedException when student attempts to query another student's applications")
    void getAllApplications_WhenStudentQueriesOtherStudent_ThrowsAccessDenied() {
        when(securityHelper.hasAuthority("ROLE_STUDENT")).thenReturn(true);
        when(securityHelper.getCurrentStudent()).thenReturn(mockStudent);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                applicationService.getAllApplications(2L, null, null, Pageable.unpaged()));

        assertEquals("You do not have permission to view another student's applications", ex.getMessage());
    }

    @Test
    @DisplayName("getAllApplications allows admin to query applications without filtering by student")
    void getAllApplications_WhenRoleAdmin_AllowsQuery() {
        when(securityHelper.hasAuthority("ROLE_STUDENT")).thenReturn(false);
        when(applicationRepository.searchApplications(eq(null), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockApplication)));

        var result = applicationService.getAllApplications(null, null, null, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getApplicationById delegates to enforceStudentReadAccess")
    void getApplicationById_EnforcesReadAccess() {
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(mockApplication));

        var response = applicationService.getApplicationById(50L);

        assertNotNull(response);
        assertEquals(50L, response.getApplicationId());
        verify(securityHelper).enforceStudentReadAccess(1L);
    }

    @Test
    @DisplayName("updateApplication throws AccessDeniedException when ownership check fails")
    void updateApplication_WhenOwnershipCheckFails_ThrowsAccessDenied() {
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(mockApplication));
        doThrow(new AccessDeniedException("You do not have permission to access or modify another student's resource"))
                .when(securityHelper).enforceStudentOwnership(1L);

        ApplicationUpdateRequest request = new ApplicationUpdateRequest("https://example.com/new.pdf", "remarks");

        assertThrows(AccessDeniedException.class, () ->
                applicationService.updateApplication(50L, request));
    }

    @Test
    @DisplayName("updateApplication throws IllegalStateException when application is in terminal SELECTED status")
    void updateApplication_WhenTerminalStatus_ThrowsIllegalStateException() {
        mockApplication.setStatus(ApplicationStatus.SELECTED);
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(mockApplication));

        ApplicationUpdateRequest request = new ApplicationUpdateRequest("https://example.com/new.pdf", "remarks");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                applicationService.updateApplication(50L, request));

        assertEquals("Cannot update application in terminal status: SELECTED", ex.getMessage());
    }

    @Test
    @DisplayName("updateApplication succeeds when application is in APPLIED status and owned by student")
    void updateApplication_WhenValid_Succeeds() {
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(mockApplication));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationUpdateRequest request = new ApplicationUpdateRequest("https://example.com/new.pdf", "updated");

        var response = applicationService.updateApplication(50L, request);

        assertNotNull(response);
        assertEquals("https://example.com/new.pdf", response.getResumeUrl());
        assertEquals("updated", response.getRemarks());
    }

    @Test
    @DisplayName("updateApplicationStatus throws AccessDeniedException when caller is a student")
    void updateApplicationStatus_WhenCallerIsStudent_ThrowsAccessDenied() {
        when(securityHelper.hasAuthority("ROLE_STUDENT")).thenReturn(true);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                applicationService.updateApplicationStatus(50L, ApplicationStatus.SHORTLISTED));

        assertEquals("Students are not permitted to change application status", ex.getMessage());
    }

    @Test
    @DisplayName("createApplication throws InactiveCompanyException when job belongs to an INACTIVE company")
    void createApplication_WhenCompanyIsInactive_ThrowsInactiveCompanyException() {
        Company inactiveCompany = new Company();
        inactiveCompany.setCompanyId(10L);
        inactiveCompany.setName("Inactive Corp");
        inactiveCompany.setStatus(CompanyStatus.INACTIVE);

        mockJob.setCompany(inactiveCompany);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(jobRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockJob));

        ApplicationRequest request = new ApplicationRequest(1L, 100L, "https://resume.pdf", "Applying");

        InactiveCompanyException ex = assertThrows(InactiveCompanyException.class,
                () -> applicationService.createApplication(request));

        assertEquals("Cannot apply to a job belonging to an inactive company (company id: 10)", ex.getMessage());
        verify(applicationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("createApplication succeeds when job belongs to an ACTIVE company")
    void createApplication_WhenCompanyIsActive_Succeeds() {
        Company activeCompany = new Company();
        activeCompany.setCompanyId(10L);
        activeCompany.setName("Active Corp");
        activeCompany.setStatus(CompanyStatus.ACTIVE);

        mockJob.setCompany(activeCompany);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(jobRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockJob));
        when(applicationRepository.existsByStudent_StudentIdAndJob_JobId(1L, 100L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application app = i.getArgument(0);
            app.setApplicationId(77L);
            return app;
        });

        ApplicationRequest request = new ApplicationRequest(1L, 100L, "https://resume.pdf", "Applying");

        ApplicationResponse response = applicationService.createApplication(request);

        assertNotNull(response);
        assertEquals(77L, response.getApplicationId());
        assertEquals(ApplicationStatus.APPLIED, response.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }
}

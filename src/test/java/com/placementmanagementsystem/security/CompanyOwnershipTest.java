package com.placementmanagementsystem.security;

import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.InterviewRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.service.ApplicationService;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.InterviewService;
import com.placementmanagementsystem.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Company Account Linkage & Ownership Enforcement Tests")
class CompanyOwnershipTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityHelper securityHelper;

    private JobService jobService;
    private CompanyService companyService;
    private ApplicationService applicationService;
    private InterviewService interviewService;

    private Company companyA;
    private Company companyB;
    private Job jobA;
    private Job jobB;
    private Application appA;
    private Application appB;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, companyRepository, securityHelper);
        companyService = new CompanyService(companyRepository, securityHelper);
        applicationService = new ApplicationService(applicationRepository, studentRepository, jobRepository, securityHelper);
        interviewService = new InterviewService(interviewRepository, applicationRepository, securityHelper);

        companyA = new Company(1L, "Company A", "https://companya.com", "Alice HR", "hr@companya.com", "1234567890", "Tech Park", CompanyStatus.ACTIVE);
        companyB = new Company(2L, "Company B", "https://companyb.com", "Bob HR", "hr@companyb.com", "0987654321", "City Center", CompanyStatus.ACTIVE);

        jobA = new Job();
        jobA.setJobId(101L);
        jobA.setTitle("Dev A");
        jobA.setCompany(companyA);
        jobA.setStatus(JobStatus.ACTIVE);

        jobB = new Job();
        jobB.setJobId(102L);
        jobB.setTitle("Dev B");
        jobB.setCompany(companyB);
        jobB.setStatus(JobStatus.ACTIVE);

        Student student = new Student();
        student.setStudentId(10L);

        appA = new Application();
        appA.setApplicationId(201L);
        appA.setStudent(student);
        appA.setJob(jobA);
        appA.setStatus(ApplicationStatus.APPLIED);

        appB = new Application();
        appB.setApplicationId(202L);
        appB.setStudent(student);
        appB.setJob(jobB);
        appB.setStatus(ApplicationStatus.APPLIED);
    }

    // 1. A company user can create a job for their associated company.
    @Test
    @DisplayName("Company user can create a job for their associated company")
    void companyUser_CreateJobForOwnCompany_Succeeds() {
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(true);
        when(securityHelper.getCurrentCompany()).thenReturn(companyA);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(companyA));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job j = i.getArgument(0);
            j.setJobId(500L);
            return j;
        });

        JobRequest request = new JobRequest(1L, "Software Engineer", "Desc", "Eligible", "Java", new BigDecimal("7.0"), new BigDecimal("1000000"), "Full-Time", LocalDate.now().plusMonths(1), JobStatus.ACTIVE);
        JobResponse response = jobService.createJob(request);

        assertNotNull(response);
        assertEquals(1L, response.getCompanyId());
    }

    // 2. A conflicting company ID supplied in the request cannot impersonate another company.
    @Test
    @DisplayName("Conflicting company ID in request cannot impersonate another company")
    void companyUser_ConflictingCompanyId_ThrowsAccessDenied() {
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(true);
        when(securityHelper.getCurrentCompany()).thenReturn(companyA);

        // User belongs to Company A (id=1), but request supplies Company B (id=2)
        JobRequest request = new JobRequest(2L, "Malicious Job", "Desc", "Eligible", "Java", new BigDecimal("7.0"), new BigDecimal("1000000"), "Full-Time", LocalDate.now().plusMonths(1), JobStatus.ACTIVE);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                jobService.createJob(request));
        assertEquals("Company users cannot create jobs for another company", ex.getMessage());
    }

    // 3. A company user cannot update another company's job.
    @Test
    @DisplayName("Company user cannot update another company's job")
    void companyUser_UpdateOtherCompanyJob_ThrowsAccessDenied() {
        when(jobRepository.findById(102L)).thenReturn(Optional.of(jobB));
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to access or modify another company's resource"))
                .when(securityHelper).enforceCompanyOwnership(2L);

        JobRequest request = new JobRequest(2L, "Updated Title", "Desc", "Eligible", "Java", new BigDecimal("7.0"), new BigDecimal("1000000"), "Full-Time", LocalDate.now().plusMonths(1), JobStatus.ACTIVE);

        assertThrows(AccessDeniedException.class, () ->
                jobService.updateJob(102L, request));
    }

    // 4. A company user cannot close another company's job.
    @Test
    @DisplayName("Company user cannot close another company's job")
    void companyUser_CloseOtherCompanyJob_ThrowsAccessDenied() {
        when(jobRepository.findById(102L)).thenReturn(Optional.of(jobB));
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to access or modify another company's resource"))
                .when(securityHelper).enforceCompanyOwnership(2L);

        assertThrows(AccessDeniedException.class, () ->
                jobService.closeJob(102L));
    }

    // 5. A company user cannot modify another company's profile.
    @Test
    @DisplayName("Company user cannot modify another company's profile")
    void companyUser_UpdateOtherCompanyProfile_ThrowsAccessDenied() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to access or modify another company's resource"))
                .when(securityHelper).enforceCompanyOwnership(2L);

        CompanyRequest request = new CompanyRequest("Comp B", "https://b.com", "Contact", "email@b.com", "123", "Addr", CompanyStatus.ACTIVE);

        assertThrows(AccessDeniedException.class, () ->
                companyService.updateCompany(2L, request));
    }

    // 6. A company user cannot update recruitment records (applications/interviews) of another company.
    @Test
    @DisplayName("Company user cannot update application status for another company's job")
    void companyUser_UpdateStatusForOtherCompanyJob_ThrowsAccessDenied() {
        when(securityHelper.hasAuthority("ROLE_STUDENT")).thenReturn(false);
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(true);
        when(applicationRepository.findById(202L)).thenReturn(Optional.of(appB));
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to access or modify another company's resource"))
                .when(securityHelper).enforceCompanyOwnership(2L);

        assertThrows(AccessDeniedException.class, () ->
                applicationService.updateApplicationStatus(202L, ApplicationStatus.SHORTLISTED));
    }

    @Test
    @DisplayName("Company user cannot schedule interview for another company's job application")
    void companyUser_ScheduleInterviewForOtherCompanyJob_ThrowsAccessDenied() {
        when(applicationRepository.findById(202L)).thenReturn(Optional.of(appB));
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(true);
        org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to access or modify another company's resource"))
                .when(securityHelper).enforceCompanyOwnership(2L);

        InterviewRequest request = new InterviewRequest(202L, 1, "Technical", LocalDateTime.now().plusDays(2), "Interviewer", "Notes", InterviewResult.PENDING);

        assertThrows(AccessDeniedException.class, () ->
                interviewService.scheduleInterview(request));
    }

    // 7. An unlinked company account is denied company-specific operations.
    @Test
    @DisplayName("Unlinked company account is denied company operations")
    void unlinkedCompanyAccount_ThrowsAccessDenied() {
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(true);
        when(securityHelper.getCurrentCompany()).thenThrow(new AccessDeniedException("Company account is not associated with any company profile"));

        JobRequest request = new JobRequest(1L, "Software Engineer", "Desc", "Eligible", "Java", new BigDecimal("7.0"), new BigDecimal("1000000"), "Full-Time", LocalDate.now().plusMonths(1), JobStatus.ACTIVE);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                jobService.createJob(request));
        assertEquals("Company account is not associated with any company profile", ex.getMessage());
    }

    // 8. An administrator retains authorized management access across any company.
    @Test
    @DisplayName("Administrator can create job for any company")
    void admin_CanCreateJobForAnyCompany() {
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(false);
        when(securityHelper.hasAuthority("ROLE_ADMIN")).thenReturn(true);
        when(companyRepository.findById(2L)).thenReturn(Optional.of(companyB));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job j = i.getArgument(0);
            j.setJobId(700L);
            return j;
        });

        JobRequest request = new JobRequest(2L, "Admin Posted Job", "Desc", "Eligible", "Java", new BigDecimal("7.0"), new BigDecimal("1000000"), "Full-Time", LocalDate.now().plusMonths(1), JobStatus.ACTIVE);
        JobResponse response = jobService.createJob(request);

        assertNotNull(response);
        assertEquals(2L, response.getCompanyId());
    }

    // 9. Entity relationship validation: User has nullable ManyToOne Company without cascading delete
    @Test
    @DisplayName("User entity maintains nullable ManyToOne relationship with Company")
    void user_EntityCompanyRelationship_MaintainsIntegrity() {
        User studentUser = new User("student@example.com", "pass", Role.ROLE_STUDENT);
        assertEquals(null, studentUser.getCompany(), "Student user should have null company");

        User companyUser = new User("hr@companya.com", "pass", Role.ROLE_COMPANY, companyA);
        assertNotNull(companyUser.getCompany(), "Company user should have linked company");
        assertEquals(1L, companyUser.getCompany().getCompanyId());
    }
}

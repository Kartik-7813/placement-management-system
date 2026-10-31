package com.placementmanagementsystem.transaction;

import com.placementmanagementsystem.dto.AuthResponse;
import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.InterviewRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.service.AuthService;
import com.placementmanagementsystem.service.InterviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Integration test verifying real Spring transaction rollback behavior.
 * Note: No @Transactional is placed on this test class so that each service call
 * controls its own database transaction boundaries, allowing post-failure assertions
 * to inspect the actual persisted database state.
 */
@SpringBootTest
@DisplayName("Transactional Rollback Integration Tests")
public class TransactionalRollbackIntegrationTest {

    private static final String ROLLBACK_USER_EMAIL = "rollback.test.user@example.com";
    private static final String ROLLBACK_STUDENT_ROLL = "ROLLBACK_2026_999";
    private static final String ROLLBACK_COMPANY_EMAIL = "rollback.company@example.com";
    private static final String ROLLBACK_EXISTING_STUDENT_EMAIL = "existing.rollback.student@example.com";
    private static final String ROLLBACK_EXISTING_STUDENT_ROLL = "EXISTING_ROLLBACK_001";

    @Autowired
    private AuthService authService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private StudentRepository studentRepository;

    @MockitoSpyBean
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Long createdCompanyId;
    private Long createdJobId;
    private Long createdStudentId;
    private Long createdApplicationId;

    @AfterEach
    void tearDown() {
        // Reset spy behavior so future operations behave normally
        reset(studentRepository);
        reset(interviewRepository);

        // Clean up test data in reverse foreign key order
        if (createdApplicationId != null) {
            interviewRepository.findByApplication_ApplicationId(createdApplicationId)
                    .forEach(i -> interviewRepository.delete(i));
            applicationRepository.deleteById(createdApplicationId);
            createdApplicationId = null;
        }

        if (createdJobId != null) {
            jobRepository.deleteById(createdJobId);
            createdJobId = null;
        }

        if (createdCompanyId != null) {
            companyRepository.deleteById(createdCompanyId);
            createdCompanyId = null;
        }

        if (createdStudentId != null) {
            studentRepository.deleteById(createdStudentId);
            createdStudentId = null;
        }

        userRepository.findByEmail(ROLLBACK_USER_EMAIL).ifPresent(userRepository::delete);
        studentRepository.findByRollNumber(ROLLBACK_STUDENT_ROLL).ifPresent(studentRepository::delete);
        studentRepository.findByRollNumber(ROLLBACK_EXISTING_STUDENT_ROLL).ifPresent(studentRepository::delete);
    }

    @Test
    @DisplayName("registerStudent rollback: User save must roll back when Student profile creation fails")
    void testRegisterStudentRollback_WhenStudentSaveFails_UserIsRolledBack() {
        // 1. Prepare registration request
        RegisterRequest request = new RegisterRequest(
                ROLLBACK_USER_EMAIL,
                "ValidPassword123!",
                ROLLBACK_STUDENT_ROLL,
                "Rollback Test Candidate"
        );

        // Verify precondition: neither User nor Student exists before test
        assertFalse(userRepository.existsByEmail(ROLLBACK_USER_EMAIL), "User must not exist before test");
        assertFalse(studentRepository.existsByRollNumber(ROLLBACK_STUDENT_ROLL), "Student must not exist before test");

        // 2. Inject controlled failure: when studentRepository.save is called (after userRepository.save has succeeded), throw RuntimeException
        doThrow(new RuntimeException("Simulated failure during Student profile persistence"))
                .when(studentRepository).save(any(Student.class));

        // 3. Execute service method and expect failure
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            authService.registerStudent(request);
        });

        assertEquals("Simulated failure during Student profile persistence", thrownException.getMessage());

        // 4. Verify post-condition: transaction rolled back, so NO User or Student record was committed
        Optional<User> persistedUser = userRepository.findByEmail(ROLLBACK_USER_EMAIL);
        Optional<Student> persistedStudent = studentRepository.findByRollNumber(ROLLBACK_STUDENT_ROLL);

        assertTrue(persistedUser.isEmpty(), "Persisted database state must NOT contain the User row after rollback");
        assertTrue(persistedStudent.isEmpty(), "Persisted database state must NOT contain the Student row after rollback");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("scheduleInterview rollback: Application status change must roll back when Interview save fails")
    void testScheduleInterviewRollback_WhenInterviewSaveFails_ApplicationStatusIsReverted() {
        // 1. Seed Company, Job, Student, and Application in SHORTLISTED status
        Company company = new Company();
        company.setName("Rollback Test Corp");
        company.setEmail(ROLLBACK_COMPANY_EMAIL);
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        createdCompanyId = company.getCompanyId();

        Job job = new Job();
        job.setTitle("Rollback Test Job");
        job.setCompany(company);
        job.setStatus(JobStatus.ACTIVE);
        job.setDeadline(LocalDate.now().plusMonths(1));
        job = jobRepository.save(job);
        createdJobId = job.getJobId();

        Student student = new Student();
        student.setRollNumber(ROLLBACK_EXISTING_STUDENT_ROLL);
        student.setName("Existing Candidate");
        student.setEmail(ROLLBACK_EXISTING_STUDENT_EMAIL);
        student.setStatus(StudentStatus.ACTIVE);
        student = studentRepository.save(student);
        createdStudentId = student.getStudentId();

        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setAppliedOn(LocalDateTime.now().minusDays(1));
        application = applicationRepository.save(application);
        createdApplicationId = application.getApplicationId();

        // Verify initial persisted application status
        Application initialApp = applicationRepository.findById(createdApplicationId).orElseThrow();
        assertEquals(ApplicationStatus.SHORTLISTED, initialApp.getStatus(), "Application must start in SHORTLISTED status");

        // 2. Inject controlled failure on interviewRepository.save (after application status was set to INTERVIEWING)
        doThrow(new RuntimeException("Simulated failure during Interview persistence"))
                .when(interviewRepository).save(any());

        InterviewRequest interviewRequest = new InterviewRequest(
                createdApplicationId,
                1,
                "Technical Round 1",
                LocalDateTime.now().plusDays(2),
                "Senior Interviewer",
                "Technical evaluation",
                InterviewResult.PENDING
        );

        // 3. Execute interview scheduling and expect failure
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            interviewService.scheduleInterview(interviewRequest);
        });

        assertEquals("Simulated failure during Interview persistence", thrownException.getMessage());

        // 4. Verify post-condition: transaction rolled back, so Application status is reverted to SHORTLISTED and no interview exists
        Application freshApplication = applicationRepository.findById(createdApplicationId).orElseThrow();
        assertEquals(ApplicationStatus.SHORTLISTED, freshApplication.getStatus(),
                "Application status in database must roll back to SHORTLISTED after interview failure");

        var interviews = interviewRepository.findByApplication_ApplicationId(createdApplicationId);
        assertTrue(interviews.isEmpty(), "No Interview row should exist in database for this application after rollback");
    }
}

package com.placementmanagementsystem.concurrency;

import com.placementmanagementsystem.dto.ApplicationRequest;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.dto.PlacementRequest;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Placement;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.entity.User;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.enums.Role;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.ApplicationAlreadyExistsException;
import com.placementmanagementsystem.exception.InactiveCompanyException;
import com.placementmanagementsystem.exception.InterviewAlreadyExistsException;
import com.placementmanagementsystem.exception.JobNotAcceptingApplicationsException;
import com.placementmanagementsystem.exception.PlacementAlreadyExistsException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.InterviewRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.repository.PlacementRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.repository.UserRepository;
import com.placementmanagementsystem.service.ApplicationService;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.InterviewService;
import com.placementmanagementsystem.service.PlacementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real database-backed concurrency tests verifying multi-threaded safety,
 * unique constraints, pessimistic locking, and race-condition resistance.
 * Note: No @Transactional on class level ensures real database commits and locks.
 */
@SpringBootTest
@DisplayName("Concurrency & Race Condition Database Integration Tests")
public class ConcurrencyIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private PlacementService placementService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<Long> cleanupCompanyIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> cleanupJobIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> cleanupStudentIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> cleanupApplicationIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> cleanupPlacementIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> cleanupUserEmails = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        // 1. Clean placements
        for (Long placementId : cleanupPlacementIds) {
            placementRepository.findById(placementId).ifPresent(placementRepository::delete);
        }
        cleanupPlacementIds.clear();

        // 2. Clean interviews for registered applications
        for (Long appId : cleanupApplicationIds) {
            interviewRepository.findByApplication_ApplicationId(appId)
                    .forEach(interviewRepository::delete);
            applicationRepository.findById(appId).ifPresent(applicationRepository::delete);
        }
        cleanupApplicationIds.clear();

        // 3. Clean jobs
        for (Long jobId : cleanupJobIds) {
            jobRepository.findById(jobId).ifPresent(jobRepository::delete);
        }
        cleanupJobIds.clear();

        // 4. Clean students and associated users
        for (Long studentId : cleanupStudentIds) {
            studentRepository.findById(studentId).ifPresent(student -> {
                userRepository.findByEmail(student.getEmail()).ifPresent(userRepository::delete);
                studentRepository.delete(student);
            });
        }
        cleanupStudentIds.clear();

        // 5. Clean users
        for (String email : cleanupUserEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
        cleanupUserEmails.clear();

        // 6. Clean companies
        for (Long companyId : cleanupCompanyIds) {
            companyRepository.findById(companyId).ifPresent(companyRepository::delete);
        }
        cleanupCompanyIds.clear();
    }

    private Company createTestCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setEmail(name.toLowerCase().replaceAll("\\s+", "") + "@example.com");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        cleanupCompanyIds.add(company.getCompanyId());
        return company;
    }

    private Job createTestJob(Company company, String title) {
        Job job = new Job();
        job.setTitle(title);
        job.setCompany(company);
        job.setStatus(JobStatus.ACTIVE);
        job.setDeadline(LocalDate.now().plusMonths(1));
        job = jobRepository.save(job);
        cleanupJobIds.add(job.getJobId());
        return job;
    }

    private Student createTestStudent(String rollNumber, String email) {
        User user = new User(email, "hashedPassword123", Role.ROLE_STUDENT);
        userRepository.save(user);
        cleanupUserEmails.add(email);

        Student student = new Student();
        student.setRollNumber(rollNumber);
        student.setName("Student " + rollNumber);
        student.setEmail(email);
        student.setStatus(StudentStatus.ACTIVE);
        student = studentRepository.save(student);
        cleanupStudentIds.add(student.getStudentId());
        return student;
    }

    private Application createTestApplication(Student student, Job job, ApplicationStatus status) {
        Application application = new Application();
        application.setStudent(student);
        application.setJob(job);
        application.setStatus(status);
        application.setAppliedOn(LocalDateTime.now().minusDays(1));
        application = applicationRepository.save(application);
        cleanupApplicationIds.add(application.getApplicationId());
        return application;
    }

    // =========================================================================
    // TEST A: Concurrent Duplicate Application Submission
    // =========================================================================

    @Test
    @DisplayName("Test A: Concurrent duplicate application submissions - exactly one succeeds, one receives conflict")
    void testConcurrentDuplicateApplicationSubmission() throws Exception {
        Company company = createTestCompany("ConcurApp Corp");
        Job job = createTestJob(company, "ConcurApp Software Engineer");
        Student student = createTestStudent("ROLL_CONCUR_APP_01", "student.app.concur@example.com");

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executor.submit(() -> {
                // Authenticate thread as the student
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(student.getEmail(), "password",
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                );

                readyLatch.countDown();
                try {
                    startLatch.await();
                    ApplicationRequest request = new ApplicationRequest(
                            student.getStudentId(),
                            job.getJobId(),
                            "https://resume.example.com/test.pdf",
                            "Concurrent application attempt"
                    );
                    ApplicationResponse response = applicationService.createApplication(request);
                    if (response != null) {
                        successCount.incrementAndGet();
                        cleanupApplicationIds.add(response.getApplicationId());
                    }
                } catch (ApplicationAlreadyExistsException | DataIntegrityViolationException | ConcurrencyFailureException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    Throwable cause = ex;
                    boolean isLegitimateConflict = false;
                    while (cause != null) {
                        if (cause instanceof ApplicationAlreadyExistsException
                                || cause instanceof DataIntegrityViolationException
                                || cause instanceof ConcurrencyFailureException) {
                            isLegitimateConflict = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (isLegitimateConflict) {
                        conflictCount.incrementAndGet();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one application submission must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one application submission must be rejected with conflict");

        List<Application> persistedApplications = applicationRepository.findByStudent_StudentId(student.getStudentId());
        assertEquals(1, persistedApplications.size(), "Database must contain exactly 1 application record");
        assertEquals(ApplicationStatus.APPLIED, persistedApplications.get(0).getStatus());
    }

    // =========================================================================
    // TEST B: Concurrent Duplicate Interview Round Scheduling
    // =========================================================================

    @Test
    @DisplayName("Test B: Concurrent duplicate interview scheduling - exactly one succeeds, one receives conflict")
    void testConcurrentDuplicateInterviewScheduling() throws Exception {
        Company company = createTestCompany("ConcurInterview Corp");
        Job job = createTestJob(company, "ConcurInterview Backend Engineer");
        Student student = createTestStudent("ROLL_CONCUR_INT_01", "student.int.concur@example.com");
        Application application = createTestApplication(student, job, ApplicationStatus.SHORTLISTED);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executor.submit(() -> {
                // Authenticate thread as Admin
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("admin@example.com", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                );

                readyLatch.countDown();
                try {
                    startLatch.await();
                    InterviewRequest request = new InterviewRequest(
                            application.getApplicationId(),
                            1, // Same round number
                            "Technical Round 1",
                            LocalDateTime.now().plusDays(2),
                            "Panel Lead",
                            "Concurrent interview test",
                            InterviewResult.PENDING
                    );
                    InterviewResponse response = interviewService.scheduleInterview(request);
                    if (response != null) {
                        successCount.incrementAndGet();
                    }
                } catch (InterviewAlreadyExistsException | DataIntegrityViolationException | ConcurrencyFailureException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    Throwable cause = ex;
                    boolean isLegitimateConflict = false;
                    while (cause != null) {
                        if (cause instanceof InterviewAlreadyExistsException
                                || cause instanceof DataIntegrityViolationException
                                || cause instanceof ConcurrencyFailureException) {
                            isLegitimateConflict = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (isLegitimateConflict) {
                        conflictCount.incrementAndGet();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one interview round scheduling must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one interview round scheduling must be rejected with conflict");

        List<Interview> persistedInterviews = interviewRepository.findByApplication_ApplicationId(application.getApplicationId());
        assertEquals(1, persistedInterviews.size(), "Database must contain exactly 1 interview record");

        Application freshApp = applicationRepository.findById(application.getApplicationId()).orElseThrow();
        assertEquals(ApplicationStatus.INTERVIEWING, freshApp.getStatus(), "Application status must be INTERVIEWING");
    }

    // =========================================================================
    // TEST C: Concurrent Company Deactivation vs Application Submission
    // =========================================================================

    @Test
    @DisplayName("Test C: Concurrent company deactivation and application submission - no application accepted for closed job")
    void testConcurrentCompanyDeactivationVsApplicationSubmission() throws Exception {
        Company company = createTestCompany("ConcurDeact Corp");
        Job job = createTestJob(company, "ConcurDeact Cloud Engineer");
        Student student = createTestStudent("ROLL_CONCUR_DEACT_01", "student.deact.concur@example.com");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger deactivationSuccess = new AtomicInteger(0);
        AtomicInteger applicationSuccess = new AtomicInteger(0);
        AtomicInteger applicationRejected = new AtomicInteger(0);

        // Thread 1: Deactivate company
        Future<?> deactFuture = executor.submit(() -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin@example.com", "password",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );
            readyLatch.countDown();
            try {
                startLatch.await();
                companyService.deactivateCompany(company.getCompanyId());
                deactivationSuccess.incrementAndGet();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Thread 2: Submit application to the company's job
        Future<?> appFuture = executor.submit(() -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(student.getEmail(), "password",
                            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
            );
            readyLatch.countDown();
            try {
                startLatch.await();
                ApplicationRequest request = new ApplicationRequest(
                        student.getStudentId(),
                        job.getJobId(),
                        "https://resume.example.com/test.pdf",
                        "Application during deactivation"
                );
                ApplicationResponse response = applicationService.createApplication(request);
                if (response != null) {
                    applicationSuccess.incrementAndGet();
                    cleanupApplicationIds.add(response.getApplicationId());
                }
            } catch (JobNotAcceptingApplicationsException | InactiveCompanyException ex) {
                applicationRejected.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        deactFuture.get(10, TimeUnit.SECONDS);
        appFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verification of database state post-execution
        assertEquals(1, deactivationSuccess.get(), "Company deactivation must succeed");

        Company updatedCompany = companyRepository.findById(company.getCompanyId()).orElseThrow();
        assertEquals(CompanyStatus.INACTIVE, updatedCompany.getStatus(), "Company must be INACTIVE");

        Job updatedJob = jobRepository.findById(job.getJobId()).orElseThrow();
        assertEquals(JobStatus.CLOSED, updatedJob.getStatus(), "Company active jobs must be CLOSED");

        // Verify ordering guarantee: If application was submitted, either:
        // 1. It completed before deactivation locked the job.
        // 2. Or it locked the job after deactivation committed and was properly rejected.
        assertTrue(applicationSuccess.get() + applicationRejected.get() == 1,
                "Application must either cleanly succeed (if acquired lock first) or be rejected (if acquired lock after)");

        // Now verify that any subsequent application submission after deactivation has committed is GUARANTEED to be rejected
        Student secondStudent = createTestStudent("ROLL_CONCUR_DEACT_02", "student.deact.second@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(secondStudent.getEmail(), "password",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
        );
        ApplicationRequest postDeactRequest = new ApplicationRequest(
                secondStudent.getStudentId(),
                job.getJobId(),
                "https://resume.example.com/test2.pdf",
                "Post deactivation attempt"
        );

        assertThrows(JobNotAcceptingApplicationsException.class, () -> {
            applicationService.createApplication(postDeactRequest);
        }, "Application after deactivation commit must be rejected with JobNotAcceptingApplicationsException");

        List<Application> postDeactApplications = applicationRepository.findByStudent_StudentId(secondStudent.getStudentId());
        assertTrue(postDeactApplications.isEmpty(), "No application record should be created after company deactivation");
    }

    // =========================================================================
    // TEST D: Concurrent Placement Creation
    // =========================================================================

    @Test
    @DisplayName("Test D: Concurrent placement creation - PESSIMISTIC_WRITE lock serializes and ensures only 1 active placement")
    void testConcurrentPlacementCreation() throws Exception {
        Company company1 = createTestCompany("Placement Corp 1");
        Job job1 = createTestJob(company1, "Placement Job 1");

        Company company2 = createTestCompany("Placement Corp 2");
        Job job2 = createTestJob(company2, "Placement Job 2");

        Student student = createTestStudent("ROLL_CONCUR_PLACE_01", "student.place.concur@example.com");

        Application app1 = createTestApplication(student, job1, ApplicationStatus.SELECTED);
        Application app2 = createTestApplication(student, job2, ApplicationStatus.SELECTED);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // Thread 1: Create placement for Application 1
        futures.add(executor.submit(() -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin@example.com", "password",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );
            readyLatch.countDown();
            try {
                startLatch.await();
                PlacementRequest request = new PlacementRequest(
                        student.getStudentId(),
                        app1.getApplicationId(),
                        "Software Engineer",
                        new BigDecimal("1200000.00"),
                        LocalDate.now().plusMonths(1),
                        PlacementStatus.CONFIRMED
                );
                PlacementResponse response = placementService.createPlacement(request);
                if (response != null) {
                    successCount.incrementAndGet();
                    cleanupPlacementIds.add(response.getPlacementId());
                }
            } catch (PlacementAlreadyExistsException | DataIntegrityViolationException | ConcurrencyFailureException ex) {
                conflictCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }));

        // Thread 2: Create placement for Application 2
        futures.add(executor.submit(() -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin@example.com", "password",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );
            readyLatch.countDown();
            try {
                startLatch.await();
                PlacementRequest request = new PlacementRequest(
                        student.getStudentId(),
                        app2.getApplicationId(),
                        "Member Technical Staff",
                        new BigDecimal("1500000.00"),
                        LocalDate.now().plusMonths(1),
                        PlacementStatus.CONFIRMED
                );
                PlacementResponse response = placementService.createPlacement(request);
                if (response != null) {
                    successCount.incrementAndGet();
                    cleanupPlacementIds.add(response.getPlacementId());
                }
            } catch (PlacementAlreadyExistsException | DataIntegrityViolationException | ConcurrencyFailureException ex) {
                conflictCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }));

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one placement creation must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one placement creation must be rejected as student already has an active placement");

        List<Placement> activePlacements = placementRepository.findByStudent_StudentIdOrderByPlacedOnDesc(student.getStudentId());
        assertEquals(1, activePlacements.size(), "Database must contain exactly 1 placement record for the student");
        assertEquals(PlacementStatus.CONFIRMED, activePlacements.get(0).getStatus());
    }
}

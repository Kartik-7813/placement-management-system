package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private JobService jobService;

    private Company company;
    private Job job;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(1L);
        company.setName("Tech Corp");
        company.setStatus(CompanyStatus.ACTIVE);

        job = new Job();
        job.setJobId(10L);
        job.setCompany(company);
        job.setTitle("Original Title");
        job.setDescription("Original Description");
        job.setMinCgpa(new BigDecimal("7.00"));
        job.setPackageAmount(new BigDecimal("900000.00"));
        job.setJobType("Full-Time");
        job.setDeadline(LocalDate.of(2023, 12, 31)); // Existing expired deadline
        job.setStatus(JobStatus.ACTIVE);
    }

    // 1. Creating a new job with a past deadline must be rejected
    @Test
    @DisplayName("createJob should reject past deadline with IllegalArgumentException")
    void testCreateJobRejectsPastDeadline() {
        JobRequest createRequest = new JobRequest(
                1L,
                "New Job",
                "Description",
                "Criteria",
                "Java",
                new BigDecimal("7.00"),
                new BigDecimal("800000.00"),
                "Full-Time",
                LocalDate.now().minusDays(1), // Past deadline
                JobStatus.ACTIVE
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                jobService.createJob(createRequest));

        assertEquals("Job deadline must be today or in the future", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    // 2. Updating an expired job without changing its deadline (omitted/null) must succeed
    @Test
    @DisplayName("updateJob should preserve existing expired deadline when request deadline is null")
    void testUpdateJobPreservesExistingDeadlineWhenRequestDeadlineIsNull() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobRequest updateRequest = new JobRequest(
                1L,
                "Updated Title",
                "Updated Description",
                "Updated Criteria",
                "Updated Skills",
                new BigDecimal("7.50"),
                new BigDecimal("1000000.00"),
                "Full-Time",
                null, // deadline omitted/null to preserve existing
                JobStatus.ACTIVE
        );

        JobResponse response = jobService.updateJob(10L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        // Verify existing expired deadline was preserved and not overwritten with null
        assertEquals(LocalDate.of(2023, 12, 31), response.getDeadline());
        verify(jobRepository).save(job);
    }

    // 3. Updating an expired job while explicitly providing the same existing deadline must succeed
    @Test
    @DisplayName("updateJob should succeed when explicitly providing the same existing expired deadline")
    void testUpdateJobSucceedsWhenResubmittingSameExpiredDeadline() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate unchangedExpiredDeadline = LocalDate.of(2023, 12, 31);
        JobRequest updateRequest = new JobRequest(
                1L,
                "Updated Title",
                "Updated Description",
                "Updated Criteria",
                "Updated Skills",
                new BigDecimal("7.50"),
                new BigDecimal("1000000.00"),
                "Full-Time",
                unchangedExpiredDeadline, // Explicitly sending same existing expired deadline
                JobStatus.ACTIVE
        );

        JobResponse response = jobService.updateJob(10L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        assertEquals(unchangedExpiredDeadline, response.getDeadline());
        verify(jobRepository).save(job);
    }

    // 4. Changing an existing deadline to a new past date must be rejected
    @Test
    @DisplayName("updateJob should reject changing deadline to a new past date")
    void testUpdateJobRejectsChangingDeadlineToNewPastDate() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        LocalDate newPastDeadline = LocalDate.now().minusDays(5);
        JobRequest updateRequest = new JobRequest(
                1L,
                "Updated Title",
                "Updated Description",
                "Updated Criteria",
                "Updated Skills",
                new BigDecimal("7.50"),
                new BigDecimal("1000000.00"),
                "Full-Time",
                newPastDeadline, // New past date
                JobStatus.ACTIVE
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                jobService.updateJob(10L, updateRequest));

        assertEquals("Job deadline must be today or in the future", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    // 5. Changing the deadline to a valid future date must succeed
    @Test
    @DisplayName("updateJob should update deadline when a valid future deadline is provided")
    void testUpdateJobUpdatesDeadlineWhenValidFutureDateProvided() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate newDeadline = LocalDate.now().plusMonths(2);
        JobRequest updateRequest = new JobRequest(
                1L,
                "Updated Title",
                "Updated Description",
                "Updated Criteria",
                "Updated Skills",
                new BigDecimal("7.50"),
                new BigDecimal("1000000.00"),
                "Full-Time",
                newDeadline, // Valid future deadline
                JobStatus.ACTIVE
        );

        JobResponse response = jobService.updateJob(10L, updateRequest);

        assertNotNull(response);
        assertEquals(newDeadline, response.getDeadline());
        verify(jobRepository).save(job);
    }

    // 6. Existing job status and ownership restrictions must remain unchanged
    @Test
    @DisplayName("createJob should set deadline when creating a job with valid future date")
    void testCreateJobSetsDeadline() {
        when(securityHelper.hasAuthority("ROLE_COMPANY")).thenReturn(false);
        when(securityHelper.hasAuthority("ROLE_ADMIN")).thenReturn(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job saved = invocation.getArgument(0);
            saved.setJobId(20L);
            return saved;
        });

        LocalDate deadline = LocalDate.now().plusDays(30);
        JobRequest createRequest = new JobRequest(
                1L,
                "New Job",
                "New Description",
                "New Criteria",
                "Java",
                new BigDecimal("7.00"),
                new BigDecimal("800000.00"),
                "Full-Time",
                deadline,
                JobStatus.ACTIVE
        );

        JobResponse response = jobService.createJob(createRequest);

        assertNotNull(response);
        assertEquals(deadline, response.getDeadline());
        verify(jobRepository).save(any(Job.class));
    }
}

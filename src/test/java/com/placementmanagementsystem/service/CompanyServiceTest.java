package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Unit Tests")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private CompanyService companyService;

    private Company testCompany;
    private Job activeJob1;
    private Job activeJob2;
    private Job closedJob;

    @BeforeEach
    void setUp() {
        testCompany = new Company(
                1L,
                "Acme Corp",
                "https://acme.com",
                "John HR",
                "hr@acme.com",
                "1234567890",
                "Tech Valley",
                CompanyStatus.ACTIVE
        );

        activeJob1 = new Job();
        activeJob1.setJobId(101L);
        activeJob1.setCompany(testCompany);
        activeJob1.setTitle("Backend Engineer");
        activeJob1.setStatus(JobStatus.ACTIVE);

        activeJob2 = new Job();
        activeJob2.setJobId(102L);
        activeJob2.setCompany(testCompany);
        activeJob2.setTitle("Frontend Engineer");
        activeJob2.setStatus(JobStatus.ACTIVE);

        closedJob = new Job();
        closedJob.setJobId(103L);
        closedJob.setCompany(testCompany);
        closedJob.setTitle("DevOps Engineer");
        closedJob.setStatus(JobStatus.CLOSED);
    }

    @Test
    @DisplayName("deactivateCompany should set company to INACTIVE and close all currently ACTIVE jobs in the same transaction")
    void deactivateCompany_ShouldCloseActiveJobsAndDeactivateCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        List<Job> activeJobs = new ArrayList<>(List.of(activeJob1, activeJob2));
        when(jobRepository.findByCompany_CompanyIdAndStatus(1L, JobStatus.ACTIVE)).thenReturn(activeJobs);

        companyService.deactivateCompany(1L);

        // Verify company is marked INACTIVE
        assertEquals(CompanyStatus.INACTIVE, testCompany.getStatus());
        verify(companyRepository).save(testCompany);

        // Verify all active jobs are closed
        assertEquals(JobStatus.CLOSED, activeJob1.getStatus());
        assertEquals(JobStatus.CLOSED, activeJob2.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Job>> jobsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository).saveAll(jobsCaptor.capture());
        List<Job> savedJobs = jobsCaptor.getValue();
        assertEquals(2, savedJobs.size());
        assertTrue(savedJobs.stream().allMatch(j -> j.getStatus() == JobStatus.CLOSED));

        // Verify already closed jobs remain untouched
        assertEquals(JobStatus.CLOSED, closedJob.getStatus());
    }

    @Test
    @DisplayName("deactivateCompany should throw CompanyNotFoundException when company does not exist")
    void deactivateCompany_CompanyNotFound_ShouldThrowException() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.deactivateCompany(999L));

        verify(companyRepository, never()).save(any());
        verify(jobRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createCompany should default to ACTIVE status and persist company")
    void createCompany_Success() {
        CompanyRequest request = new CompanyRequest(
                "Beta Solutions",
                "https://beta.com",
                "Bob HR",
                "hr@beta.com",
                "9876543210",
                "City Center",
                null
        );

        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setCompanyId(2L);
            return c;
        });

        CompanyResponse response = companyService.createCompany(request);

        assertNotNull(response);
        assertEquals(2L, response.getCompanyId());
        assertEquals(CompanyStatus.ACTIVE, response.getStatus());
        assertEquals("Beta Solutions", response.getName());
    }
}

package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.exception.InactiveCompanyException;
import com.placementmanagementsystem.exception.JobNotFoundException;
import com.placementmanagementsystem.repository.CompanyRepository;
import com.placementmanagementsystem.repository.JobRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final SecurityHelper securityHelper;

    public JobService(JobRepository jobRepository,
                      CompanyRepository companyRepository,
                      SecurityHelper securityHelper) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.securityHelper = securityHelper;
    }

    public JobResponse createJob(JobRequest request) {
        Long targetCompanyId;
        if (securityHelper.hasAuthority("ROLE_COMPANY")) {
            Company authenticatedCompany = securityHelper.getCurrentCompany();
            // Prevent company user from creating a job for another company
            if (request.getCompanyId() != null && !request.getCompanyId().equals(authenticatedCompany.getCompanyId())) {
                throw new AccessDeniedException("Company users cannot create jobs for another company");
            }
            targetCompanyId = authenticatedCompany.getCompanyId();
        } else if (securityHelper.hasAuthority("ROLE_ADMIN")) {
            targetCompanyId = request.getCompanyId();
        } else {
            throw new AccessDeniedException("Access denied");
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + targetCompanyId));

        if (company.getStatus() == CompanyStatus.INACTIVE) {
            throw new InactiveCompanyException("Cannot create a job for an inactive company (company id: " + targetCompanyId + ")");
        }

        Job job = toEntity(request);
        job.setCompany(company);
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.ACTIVE);
        }

        Job savedJob = jobRepository.save(job);
        return toResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(String title, Long companyId, JobStatus status, BigDecimal minCgpa, Pageable pageable) {
        String searchTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;

        Page<Job> jobs = jobRepository.searchJobs(searchTitle, companyId, status, minCgpa, pageable);
        return jobs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
        return toResponse(job);
    }

    public JobResponse updateJob(Long id, JobRequest request) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));

        // Enforce company ownership: company users can only update jobs belonging to their company
        securityHelper.enforceCompanyOwnership(existingJob.getCompany().getCompanyId());

        Long targetCompanyId;
        if (securityHelper.hasAuthority("ROLE_COMPANY")) {
            Company authenticatedCompany = securityHelper.getCurrentCompany();
            if (request.getCompanyId() != null && !request.getCompanyId().equals(authenticatedCompany.getCompanyId())) {
                throw new AccessDeniedException("Company users cannot transfer jobs to another company");
            }
            targetCompanyId = authenticatedCompany.getCompanyId();
        } else {
            targetCompanyId = (request.getCompanyId() != null) ? request.getCompanyId() : existingJob.getCompany().getCompanyId();
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + targetCompanyId));

        if (company.getStatus() == CompanyStatus.INACTIVE &&
                (request.getStatus() == null || request.getStatus() == JobStatus.ACTIVE)) {
            throw new InactiveCompanyException("Cannot assign active job status for an inactive company (company id: " + targetCompanyId + ")");
        }

        existingJob.setCompany(company);
        existingJob.setTitle(request.getTitle());
        existingJob.setDescription(request.getDescription());
        existingJob.setEligibilityCriteria(request.getEligibilityCriteria());
        existingJob.setRequiredSkills(request.getRequiredSkills());
        existingJob.setMinCgpa(request.getMinCgpa());
        existingJob.setPackageAmount(request.getPackageAmount());
        existingJob.setJobType(request.getJobType());
        existingJob.setDeadline(request.getDeadline());
        if (request.getStatus() != null) {
            existingJob.setStatus(request.getStatus());
        }

        Job updatedJob = jobRepository.save(existingJob);
        return toResponse(updatedJob);
    }

    public void closeJob(Long id) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));

        // Enforce company ownership: company users can only close jobs belonging to their company
        securityHelper.enforceCompanyOwnership(existingJob.getCompany().getCompanyId());

        existingJob.setStatus(JobStatus.CLOSED);
        jobRepository.save(existingJob);
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getJobId(),
                job.getCompany().getCompanyId(),
                job.getCompany().getName(),
                job.getTitle(),
                job.getDescription(),
                job.getEligibilityCriteria(),
                job.getRequiredSkills(),
                job.getMinCgpa(),
                job.getPackageAmount(),
                job.getJobType(),
                job.getDeadline(),
                job.getStatus()
        );
    }

    private Job toEntity(JobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setEligibilityCriteria(request.getEligibilityCriteria());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setMinCgpa(request.getMinCgpa());
        job.setPackageAmount(request.getPackageAmount());
        job.setJobType(request.getJobType());
        job.setDeadline(request.getDeadline());
        job.setStatus(request.getStatus());
        return job;
    }
}

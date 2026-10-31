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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final SecurityHelper securityHelper;

    public CompanyService(CompanyRepository companyRepository,
                          JobRepository jobRepository,
                          SecurityHelper securityHelper) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.securityHelper = securityHelper;
    }

    public CompanyResponse createCompany(CompanyRequest request) {
        Company company = toEntity(request);
        if (company.getStatus() == null) {
            company.setStatus(CompanyStatus.ACTIVE);
        }

        Company savedCompany = companyRepository.save(company);
        return toResponse(savedCompany);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAllCompanies(String name, CompanyStatus status, Pageable pageable) {
        String searchName = (name != null && !name.trim().isEmpty()) ? name.trim() : null;

        Page<Company> companies = companyRepository.searchCompanies(searchName, status, pageable);
        return companies.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + id));
        return toResponse(company);
    }

    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        // Enforce company ownership: company users can update only their own company profile
        securityHelper.enforceCompanyOwnership(id);

        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + id));

        existingCompany.setName(request.getName());
        existingCompany.setWebsite(request.getWebsite());
        existingCompany.setContactPerson(request.getContactPerson());
        existingCompany.setEmail(request.getEmail());
        existingCompany.setPhone(request.getPhone());
        existingCompany.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            existingCompany.setStatus(request.getStatus());
        }

        Company updatedCompany = companyRepository.save(existingCompany);
        return toResponse(updatedCompany);
    }

    public void deactivateCompany(Long id) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + id));

        existingCompany.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(existingCompany);

        // Close all currently active jobs belonging to this company within the same transaction
        List<Job> activeJobs = jobRepository.findByCompany_CompanyIdAndStatus(id, JobStatus.ACTIVE);
        for (Job job : activeJobs) {
            job.setStatus(JobStatus.CLOSED);
        }
        jobRepository.saveAll(activeJobs);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getCompanyId(),
                company.getName(),
                company.getWebsite(),
                company.getContactPerson(),
                company.getEmail(),
                company.getPhone(),
                company.getAddress(),
                company.getStatus()
        );
    }

    private Company toEntity(CompanyRequest request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setWebsite(request.getWebsite());
        company.setContactPerson(request.getContactPerson());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setAddress(request.getAddress());
        company.setStatus(request.getStatus());
        return company;
    }
}

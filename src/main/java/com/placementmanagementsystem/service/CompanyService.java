package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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

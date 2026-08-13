package com.placementmanagementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.placementmanagementsystem.entity.Company;
import com.placementmanagementsystem.enums.CompanyStatus;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByNameContainingIgnoreCase(String name);

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Company> findByStatus(CompanyStatus status);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    long countByStatus(CompanyStatus status);

    @Query("SELECT c FROM Company c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Company> searchCompanies(
            @Param("name") String name,
            @Param("status") CompanyStatus status,
            Pageable pageable
    );
}

package com.placementmanagementsystem.repository;

import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT j FROM Job j WHERE j.jobId = :id")
    Optional<Job> findByIdWithLock(@Param("id") Long id);

    List<Job> findByCompany_CompanyId(Long companyId);

    Page<Job> findByCompany_CompanyId(Long companyId, Pageable pageable);

    List<Job> findByCompany_CompanyIdAndStatus(Long companyId, JobStatus status);

    List<Job> findByStatus(JobStatus status);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByCompany_CompanyIdAndStatus(Long companyId, JobStatus status, Pageable pageable);

    Page<Job> findByMinCgpaLessThanEqual(BigDecimal cgpa, Pageable pageable);

    Page<Job> findByMinCgpaLessThanEqualAndStatus(BigDecimal cgpa, JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:companyId IS NULL OR j.company.companyId = :companyId) AND " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:minCgpa IS NULL OR j.minCgpa <= :minCgpa)")
    Page<Job> searchJobs(
            @Param("title") String title,
            @Param("companyId") Long companyId,
            @Param("status") JobStatus status,
            @Param("minCgpa") BigDecimal minCgpa,
            Pageable pageable
    );
}

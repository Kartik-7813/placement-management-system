package com.placementmanagementsystem.repository;

import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByStudent_StudentIdAndJob_JobId(Long studentId, Long jobId);

    Optional<Application> findByStudent_StudentIdAndJob_JobId(Long studentId, Long jobId);

    List<Application> findByStudent_StudentId(Long studentId);

    Page<Application> findByStudent_StudentId(Long studentId, Pageable pageable);

    List<Application> findByJob_JobId(Long jobId);

    Page<Application> findByJob_JobId(Long jobId, Pageable pageable);

    List<Application> findByStatus(ApplicationStatus status);

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE " +
           "(:studentId IS NULL OR a.student.studentId = :studentId) AND " +
           "(:jobId IS NULL OR a.job.jobId = :jobId) AND " +
           "(:status IS NULL OR a.status = :status)")
    Page<Application> searchApplications(
            @Param("studentId") Long studentId,
            @Param("jobId") Long jobId,
            @Param("status") ApplicationStatus status,
            Pageable pageable
    );
}

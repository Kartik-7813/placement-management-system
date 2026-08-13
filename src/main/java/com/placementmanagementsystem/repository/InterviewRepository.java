package com.placementmanagementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.enums.InterviewResult;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplication_ApplicationId(Long applicationId);

    Page<Interview> findByApplication_ApplicationId(
            Long applicationId,
            Pageable pageable);

    List<Interview> findByApplication_ApplicationIdOrderByRoundNumberAsc(
            Long applicationId);

    List<Interview> findByResult(InterviewResult result);

    Page<Interview> findByResult(
            InterviewResult result,
            Pageable pageable);

    boolean existsByApplication_ApplicationIdAndRoundNumber(
            Long applicationId,
            Integer roundNumber);

    @Query("""
            SELECT i FROM Interview i
            WHERE (:applicationId IS NULL
                   OR i.application.applicationId = :applicationId)
            AND (:result IS NULL
                   OR i.result = :result)
            """)
    Page<Interview> searchInterviews(
            @Param("applicationId") Long applicationId,
            @Param("result") InterviewResult result,
            Pageable pageable);
}
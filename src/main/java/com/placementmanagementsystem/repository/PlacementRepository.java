package com.placementmanagementsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.placementmanagementsystem.entity.Placement;
import com.placementmanagementsystem.enums.PlacementStatus;

@Repository
public interface PlacementRepository extends JpaRepository<Placement, Long> {

    Optional<Placement> findByStudent_StudentId(Long studentId);

    Optional<Placement> findByApplication_ApplicationId(Long applicationId);

    boolean existsByStudent_StudentId(Long studentId);

    boolean existsByApplication_ApplicationId(Long applicationId);

    List<Placement> findByStatus(PlacementStatus status);

    Page<Placement> findByStatus(PlacementStatus status, Pageable pageable);

    long countByStatus(PlacementStatus status);
}

package com.placementmanagementsystem.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagementsystem.dto.PlacementRequest;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Placement;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.ApplicationNotSelectedException;
import com.placementmanagementsystem.exception.PlacementAlreadyExistsException;
import com.placementmanagementsystem.exception.PlacementNotFoundException;
import com.placementmanagementsystem.exception.PlacementStudentMismatchException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.PlacementRepository;
import com.placementmanagementsystem.repository.StudentRepository;

@Service
@Transactional
public class PlacementService {

    private final PlacementRepository placementRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;

    public PlacementService(
            PlacementRepository placementRepository,
            StudentRepository studentRepository,
            ApplicationRepository applicationRepository) {

        this.placementRepository = placementRepository;
        this.studentRepository = studentRepository;
        this.applicationRepository = applicationRepository;
    }

    // =========================================================
    // CREATE PLACEMENT
    // =========================================================

    public PlacementResponse createPlacement(PlacementRequest request) {

        // 1. Student must exist
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student not found with id: " + request.getStudentId()));

        // 2. Application must exist
        Application application = applicationRepository.findById(request.getApplicationId())
        .orElseThrow(() -> new ApplicationNotFoundException(
                "Application not found with id: " + request.getApplicationId()));

        // 3. Application must belong to the supplied student
        if (!application.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new PlacementStudentMismatchException(
                    "Application " + request.getApplicationId()
                            + " does not belong to student " + request.getStudentId());
        }

        // 4. Application must be SELECTED
        if (application.getStatus() != ApplicationStatus.SELECTED) {
            throw new ApplicationNotSelectedException(
                    "Placement can only be created for a SELECTED application. "
                            + "Current status: " + application.getStatus());
        }

        // 5. Student cannot already have a placement
        if (placementRepository.existsByStudent_StudentId(student.getStudentId())) {
            throw new PlacementAlreadyExistsException(
                    "Student already has a placement: " + student.getStudentId());
        }

        // 6. Application cannot already have a placement
        if (placementRepository.existsByApplication_ApplicationId(application.getApplicationId())) {
            throw new PlacementAlreadyExistsException(
                    "Application already has a placement: " + application.getApplicationId());
        }

        // 7. Create placement
        Placement placement = new Placement();

        placement.setStudent(student);
        placement.setApplication(application);

        placement.setJobRole(request.getJobRole());
        placement.setPackageAmount(request.getPackageAmount());
        placement.setJoiningDate(request.getJoiningDate());

        // Initial placement status is controlled by the service
        placement.setStatus(PlacementStatus.CONFIRMED);

        placement.setPlacedOn(LocalDateTime.now());

        Placement savedPlacement = placementRepository.save(placement);

        return toResponse(savedPlacement);
    }

    // =========================================================
    // GET ALL PLACEMENTS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<PlacementResponse> getAllPlacements(
            PlacementStatus status,
            Pageable pageable) {

        Page<Placement> placements;

        if (status != null) {
            placements = placementRepository.findByStatus(status, pageable);
        } else {
            placements = placementRepository.findAll(pageable);
        }

        return placements.map(this::toResponse);
    }

    // =========================================================
    // GET PLACEMENT BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public PlacementResponse getPlacementById(Long id) {

        Placement placement = placementRepository.findById(id)
                .orElseThrow(() -> new PlacementNotFoundException(
                        "Placement not found with id: " + id));

        return toResponse(placement);
    }

    // =========================================================
    // GET PLACEMENT BY STUDENT
    // =========================================================

    @Transactional(readOnly = true)
    public PlacementResponse getPlacementByStudent(Long studentId) {

        Placement placement = placementRepository
                .findByStudent_StudentId(studentId)
                .orElseThrow(() -> new PlacementNotFoundException(
                        "Placement not found for student id: " + studentId));

        return toResponse(placement);
    }

    // =========================================================
    // UPDATE PLACEMENT DETAILS
    // =========================================================

    public PlacementResponse updatePlacement(
            Long id,
            PlacementRequest request) {

        Placement existingPlacement = placementRepository.findById(id)
                .orElseThrow(() -> new PlacementNotFoundException(
                        "Placement not found with id: " + id));

        /*
         * Student and application relationships are immutable.
         * Only placement details can be updated.
         */

        existingPlacement.setJobRole(request.getJobRole());
        existingPlacement.setPackageAmount(request.getPackageAmount());
        existingPlacement.setJoiningDate(request.getJoiningDate());

        Placement updatedPlacement =
                placementRepository.save(existingPlacement);

        return toResponse(updatedPlacement);
    }

    // =========================================================
    // UPDATE PLACEMENT STATUS
    // =========================================================

    public PlacementResponse updatePlacementStatus(
            Long id,
            PlacementStatus newStatus) {

        Placement placement = placementRepository.findById(id)
                .orElseThrow(() -> new PlacementNotFoundException(
                        "Placement not found with id: " + id));

        validateStatusTransition(
                placement.getStatus(),
                newStatus
        );

        placement.setStatus(newStatus);

        Placement updatedPlacement =
                placementRepository.save(placement);

        return toResponse(updatedPlacement);
    }

    // =========================================================
    // STATUS TRANSITION VALIDATION
    // =========================================================

    private void validateStatusTransition(
            PlacementStatus currentStatus,
            PlacementStatus newStatus) {

        boolean valid = false;

        switch (currentStatus) {

            case CONFIRMED -> // CONFIRMED → JOINED or CANCELLED
                valid = newStatus == PlacementStatus.JOINED
                        || newStatus == PlacementStatus.CANCELLED;

            case JOINED, CANCELLED -> // Terminal states
                valid = false;
        }

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid placement status transition: "
                            + currentStatus + " → " + newStatus);
        }
    }

    // =========================================================
    // ENTITY → DTO
    // =========================================================

    private PlacementResponse toResponse(Placement placement) {

        return new PlacementResponse(
                placement.getPlacementId(),
                placement.getStudent().getStudentId(),
                placement.getStudent().getName(),
                placement.getApplication().getApplicationId(),
                placement.getJobRole(),
                placement.getPackageAmount(),
                placement.getJoiningDate(),
                placement.getStatus(),
                placement.getPlacedOn()
        );
    }
}
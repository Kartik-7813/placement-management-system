package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.PlacementRequest;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Job;
import com.placementmanagementsystem.entity.Placement;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.exception.PlacementAlreadyExistsException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.PlacementRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {

    @Mock
    private PlacementRepository placementRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private com.placementmanagementsystem.security.SecurityHelper securityHelper;

    @InjectMocks
    private PlacementService placementService;

    private Student testStudent;
    private Application testApplication1;
    private Application testApplication2;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setStudentId(1L);
        testStudent.setName("Jane Doe");
        testStudent.setRollNumber("CS2026100");

        Job job1 = new Job();
        job1.setJobId(10L);
        job1.setTitle("Software Engineer");

        Job job2 = new Job();
        job2.setJobId(20L);
        job2.setTitle("Systems Engineer");

        testApplication1 = new Application();
        testApplication1.setApplicationId(101L);
        testApplication1.setStudent(testStudent);
        testApplication1.setJob(job1);
        testApplication1.setStatus(ApplicationStatus.SELECTED);

        testApplication2 = new Application();
        testApplication2.setApplicationId(102L);
        testApplication2.setStudent(testStudent);
        testApplication2.setJob(job2);
        testApplication2.setStatus(ApplicationStatus.SELECTED);
    }

    @Test
    @DisplayName("First placement creation succeeds with CONFIRMED status and acquires pessimistic lock on student")
    void testCreateFirstPlacement_success() {
        PlacementRequest request = new PlacementRequest(
                1L,
                101L,
                "Software Engineer",
                new BigDecimal("1200000.00"),
                LocalDate.now().plusMonths(1),
                PlacementStatus.CONFIRMED
        );

        when(studentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testStudent));
        when(applicationRepository.findById(101L)).thenReturn(Optional.of(testApplication1));
        when(placementRepository.existsByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES))).thenReturn(false);
        when(placementRepository.existsByApplication_ApplicationId(101L)).thenReturn(false);
        when(placementRepository.save(any(Placement.class))).thenAnswer(invocation -> {
            Placement p = invocation.getArgument(0);
            p.setPlacementId(501L);
            return p;
        });

        PlacementResponse response = placementService.createPlacement(request);

        assertNotNull(response);
        assertEquals(501L, response.getPlacementId());
        assertEquals(PlacementStatus.CONFIRMED, response.getStatus());
        assertEquals(1L, response.getStudentId());
        assertEquals(101L, response.getApplicationId());

        verify(studentRepository).findByIdWithLock(1L);
        verify(placementRepository).save(any(Placement.class));
    }

    @Test
    @DisplayName("Rejection of a second placement when student already has a CONFIRMED placement")
    void testCreateSecondPlacement_whenAlreadyConfirmed_throwsException() {
        PlacementRequest request = new PlacementRequest(
                1L,
                102L,
                "Systems Engineer",
                new BigDecimal("1400000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );

        when(studentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testStudent));
        when(applicationRepository.findById(102L)).thenReturn(Optional.of(testApplication2));
        when(placementRepository.existsByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES))).thenReturn(true);

        PlacementAlreadyExistsException exception = assertThrows(
                PlacementAlreadyExistsException.class,
                () -> placementService.createPlacement(request)
        );

        assertEquals("Student already has an active placement: 1", exception.getMessage());
        verify(placementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejection of a second placement when student already has a JOINED placement")
    void testCreateSecondPlacement_whenAlreadyJoined_throwsException() {
        PlacementRequest request = new PlacementRequest(
                1L,
                102L,
                "Systems Engineer",
                new BigDecimal("1400000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );

        when(studentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testStudent));
        when(applicationRepository.findById(102L)).thenReturn(Optional.of(testApplication2));
        when(placementRepository.existsByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES))).thenReturn(true);

        PlacementAlreadyExistsException exception = assertThrows(
                PlacementAlreadyExistsException.class,
                () -> placementService.createPlacement(request)
        );

        assertEquals("Student already has an active placement: 1", exception.getMessage());
        verify(placementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancellation followed by a new placement succeeds for a student with previous CANCELLED records")
    void testCancellationFollowedByNewPlacement_success() {
        PlacementRequest request = new PlacementRequest(
                1L,
                102L,
                "Systems Engineer",
                new BigDecimal("1500000.00"),
                LocalDate.now().plusMonths(1),
                PlacementStatus.CONFIRMED
        );

        when(studentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testStudent));
        when(applicationRepository.findById(102L)).thenReturn(Optional.of(testApplication2));
        // Active check returns false because previous placement was CANCELLED
        when(placementRepository.existsByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES))).thenReturn(false);
        when(placementRepository.existsByApplication_ApplicationId(102L)).thenReturn(false);
        when(placementRepository.save(any(Placement.class))).thenAnswer(invocation -> {
            Placement p = invocation.getArgument(0);
            p.setPlacementId(502L);
            return p;
        });

        PlacementResponse response = placementService.createPlacement(request);

        assertNotNull(response);
        assertEquals(502L, response.getPlacementId());
        assertEquals(PlacementStatus.CONFIRMED, response.getStatus());
        verify(placementRepository).save(any(Placement.class));
    }

    @Test
    @DisplayName("Retention of historical cancelled records: history contains both cancelled and confirmed placements")
    void testRetentionOfHistoricalCancelledRecords() {
        Placement cancelledPlacement = new Placement(
                500L,
                testStudent,
                testApplication1,
                "Initial Offer",
                new BigDecimal("1000000.00"),
                LocalDate.now(),
                PlacementStatus.CANCELLED,
                LocalDateTime.now().minusMonths(2)
        );

        Placement confirmedPlacement = new Placement(
                501L,
                testStudent,
                testApplication2,
                "New Offer",
                new BigDecimal("1300000.00"),
                LocalDate.now().plusMonths(1),
                PlacementStatus.CONFIRMED,
                LocalDateTime.now().minusWeeks(1)
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(placementRepository.findByStudent_StudentIdOrderByPlacedOnDesc(1L))
                .thenReturn(List.of(confirmedPlacement, cancelledPlacement));
        when(placementRepository.findFirstByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES)))
                .thenReturn(Optional.of(confirmedPlacement));

        // 1. Verify history returns all records including CANCELLED
        List<PlacementResponse> history = placementService.getPlacementHistoryByStudent(1L);
        assertEquals(2, history.size());
        assertEquals(PlacementStatus.CONFIRMED, history.get(0).getStatus());
        assertEquals(PlacementStatus.CANCELLED, history.get(1).getStatus());

        // 2. Verify single placement endpoint prioritizes the active CONFIRMED placement
        PlacementResponse active = placementService.getPlacementByStudent(1L);
        assertEquals(501L, active.getPlacementId());
        assertEquals(PlacementStatus.CONFIRMED, active.getStatus());
    }

    @Test
    @DisplayName("Concurrent placement attempt serialized: second request finds active placement and is rejected")
    void testConcurrentPlacementAttempts_serializedByLock() {
        PlacementRequest concurrentRequest = new PlacementRequest(
                1L,
                102L,
                "Systems Engineer",
                new BigDecimal("1400000.00"),
                LocalDate.now().plusMonths(1),
                PlacementStatus.CONFIRMED
        );

        // When Thread 2 unblocks from lock, Thread 1 has already committed an active placement
        when(studentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testStudent));
        when(applicationRepository.findById(102L)).thenReturn(Optional.of(testApplication2));
        when(placementRepository.existsByStudent_StudentIdAndStatusIn(eq(1L), eq(PlacementService.ACTIVE_STATUSES)))
                .thenReturn(true);

        assertThrows(
                PlacementAlreadyExistsException.class,
                () -> placementService.createPlacement(concurrentRequest)
        );

        verify(placementRepository, never()).save(any());
    }
}

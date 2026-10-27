package com.placementmanagementsystem.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.entity.Application;
import com.placementmanagementsystem.entity.Interview;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.exception.InterviewAlreadyExistsException;
import com.placementmanagementsystem.repository.ApplicationRepository;
import com.placementmanagementsystem.repository.InterviewRepository;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private InterviewService interviewService;

    private Application testApplication;
    private Interview testInterview;

    @BeforeEach
    void setUp() {
        testApplication = new Application();
        testApplication.setApplicationId(10L);
        testApplication.setStatus(ApplicationStatus.SHORTLISTED);

        testInterview = new Interview();
        testInterview.setInterviewId(100L);
        testInterview.setApplication(testApplication);
        testInterview.setRoundNumber(1);
        testInterview.setRoundType("Technical");
        testInterview.setScheduledOn(LocalDateTime.now().plusDays(2));
        testInterview.setInterviewer("John Doe");
        testInterview.setResult(InterviewResult.PENDING);
    }

    @Test
    @DisplayName("Scheduling interview with duplicate round for same application throws InterviewAlreadyExistsException")
    void testScheduleInterview_duplicateRoundThrowsException() {
        InterviewRequest request = new InterviewRequest(
                10L,
                1,
                "Technical",
                LocalDateTime.now().plusDays(2),
                "John Doe",
                "Initial round",
                InterviewResult.PENDING
        );

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.existsByApplication_ApplicationIdAndRoundNumber(10L, 1)).thenReturn(true);

        InterviewAlreadyExistsException exception = assertThrows(
                InterviewAlreadyExistsException.class,
                () -> interviewService.scheduleInterview(request)
        );

        assertEquals("Interview round 1 already exists for application id: 10", exception.getMessage());
        verify(interviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating interview to a round number that already exists throws InterviewAlreadyExistsException")
    void testUpdateInterview_changingToExistingRoundThrowsException() {
        InterviewRequest request = new InterviewRequest(
                10L,
                2, // changing from round 1 to round 2
                "HR Round",
                LocalDateTime.now().plusDays(3),
                "Jane Smith",
                "HR interview",
                InterviewResult.PENDING
        );

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(interviewRepository.existsByApplication_ApplicationIdAndRoundNumber(10L, 2)).thenReturn(true);

        InterviewAlreadyExistsException exception = assertThrows(
                InterviewAlreadyExistsException.class,
                () -> interviewService.updateInterview(100L, request)
        );

        assertEquals("Interview round 2 already exists for application id: 10", exception.getMessage());
        verify(interviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating interview when round number is unchanged succeeds without false duplicate detection")
    void testUpdateInterview_roundUnchangedSucceeds() {
        InterviewRequest request = new InterviewRequest(
                10L,
                1, // same round 1
                "Technical Deep Dive",
                LocalDateTime.now().plusDays(4),
                "Senior Architect",
                "Detailed feedback",
                InterviewResult.PASSED
        );

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewResponse response = interviewService.updateInterview(100L, request);

        assertNotNull(response);
        assertEquals(1, response.getRoundNumber());
        assertEquals("Technical Deep Dive", response.getRoundType());
        assertEquals("Senior Architect", response.getInterviewer());
        assertEquals(InterviewResult.PASSED, response.getResult());
        verify(interviewRepository, never()).existsByApplication_ApplicationIdAndRoundNumber(any(), any());
    }

    @Test
    @DisplayName("Updating interview changing application to an application with existing round throws InterviewAlreadyExistsException")
    void testUpdateInterview_changingApplicationWithConflictingRoundThrowsException() {
        InterviewRequest request = new InterviewRequest(
                20L, // changing application from 10 to 20
                1,
                "Technical",
                LocalDateTime.now().plusDays(2),
                "John Doe",
                null,
                InterviewResult.PENDING
        );

        when(interviewRepository.findById(100L)).thenReturn(Optional.of(testInterview));
        when(interviewRepository.existsByApplication_ApplicationIdAndRoundNumber(20L, 1)).thenReturn(true);

        InterviewAlreadyExistsException exception = assertThrows(
                InterviewAlreadyExistsException.class,
                () -> interviewService.updateInterview(100L, request)
        );

        assertEquals("Interview round 1 already exists for application id: 20", exception.getMessage());
    }

    @Test
    @DisplayName("Simulating concurrent duplicate insert: repository save throwing DataIntegrityViolationException")
    void testConcurrentDuplicateInsert_databaseConstraintViolationThrows() {
        InterviewRequest request = new InterviewRequest(
                10L,
                2,
                "Technical 2",
                LocalDateTime.now().plusDays(2),
                "John Doe",
                null,
                InterviewResult.PENDING
        );

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(testApplication));
        when(interviewRepository.existsByApplication_ApplicationIdAndRoundNumber(10L, 2)).thenReturn(false);
        when(interviewRepository.save(any(Interview.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '10-2' for key 'uk_application_round'"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> interviewService.scheduleInterview(request)
        );
    }
}

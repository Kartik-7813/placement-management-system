package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.repository.SkillRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private StudentService studentService;

    private Student testStudent;
    private StudentRequest validRequest;

    @BeforeEach
    void setUp() {
        testStudent = new Student(
                1L,
                "CS2026001",
                "Alice Smith",
                "alice@example.com",
                "9876543210",
                LocalDate.of(2003, 1, 15),
                "Female",
                "Computer Science",
                new BigDecimal("8.50"),
                2026,
                "Campus Hostel",
                StudentStatus.ACTIVE
        );

        validRequest = new StudentRequest(
                "CS2026001",
                "Alice Smith Updated",
                "alice@example.com",
                "9876543211",
                LocalDate.of(2003, 1, 15),
                "Female",
                "Computer Science",
                new BigDecimal("8.75"),
                2026,
                "New Address",
                StudentStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("updateStudent should reject request when email differs from existing student login email")
    void updateStudent_DifferentEmail_ShouldThrowIllegalArgumentException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        StudentRequest requestWithDifferentEmail = new StudentRequest(
                "CS2026001",
                "Alice Smith",
                "changed.email@example.com",
                "9876543210",
                LocalDate.of(2003, 1, 15),
                "Female",
                "Computer Science",
                new BigDecimal("8.50"),
                2026,
                "Campus Hostel",
                StudentStatus.ACTIVE
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> studentService.updateStudent(1L, requestWithDifferentEmail));

        assertEquals("Student email cannot be updated. Login email must remain unchanged.", ex.getMessage());
        verify(studentRepository, never()).save(any());
        assertEquals("alice@example.com", testStudent.getEmail());
    }

    @Test
    @DisplayName("updateStudent should succeed when email matches existing student email")
    void updateStudent_SameEmail_ShouldSucceed() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.updateStudent(1L, validRequest);

        assertNotNull(response);
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice Smith Updated", response.getName());
        assertEquals(new BigDecimal("8.75"), response.getCgpa());
        assertEquals("9876543211", response.getPhone());
        verify(studentRepository).save(testStudent);
    }

    @Test
    @DisplayName("updateStudent should reject request when roll number is changed to an already existing roll number")
    void updateStudent_DuplicateRollNumber_ShouldThrowDuplicateStudentException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(studentRepository.existsByRollNumber("CS2026999")).thenReturn(true);

        StudentRequest requestWithDuplicateRollNumber = new StudentRequest(
                "CS2026999",
                "Alice Smith",
                "alice@example.com",
                "9876543210",
                LocalDate.of(2003, 1, 15),
                "Female",
                "Computer Science",
                new BigDecimal("8.50"),
                2026,
                "Campus Hostel",
                StudentStatus.ACTIVE
        );

        DuplicateStudentException ex = assertThrows(DuplicateStudentException.class,
                () -> studentService.updateStudent(1L, requestWithDuplicateRollNumber));

        assertTrue(ex.getMessage().contains("CS2026999 already exists"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStudent should throw StudentNotFoundException when student does not exist")
    void updateStudent_StudentNotFound_ShouldThrowException() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> studentService.updateStudent(999L, validRequest));

        verify(studentRepository, never()).save(any());
    }
}

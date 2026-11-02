package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.dto.SkillResponse;
import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Students", description = "Endpoints for managing student profiles and skills")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(summary = "Create student", description = "Create a new student profile. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Student with email or roll number already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all students", description = "Get paginated students with optional filters for department, status, and name. Requires ADMIN or COMPANY role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property or parameter mismatch"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or COMPANY role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY')")
    @GetMapping
    public ResponseEntity<Page<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = "studentId", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<StudentResponse> response = studentService.getAllStudents(department, status, name, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get student by ID", description = "Retrieve a student profile. Requires ADMIN, COMPANY, or profile-owner STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot view another student's profile"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        StudentResponse response = studentService.getStudentById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update student", description = "Update an existing student profile. Requires ADMIN or profile-owner STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request fields"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot update another student's profile"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "409", description = "Roll number conflict")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate student", description = "Deactivate a student profile. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Student deactivated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get student skills", description = "List all skills assigned to a student. Requires ADMIN, COMPANY, or profile-owner STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another student's skills"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{studentId}/skills")
    public ResponseEntity<List<SkillResponse>> getStudentSkills(@PathVariable Long studentId) {
        List<SkillResponse> skills = studentService.getStudentSkills(studentId);
        return ResponseEntity.ok(skills);
    }

    @Operation(summary = "Assign skill to student", description = "Associate a skill with a student. Requires ADMIN or profile-owner STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skill assigned successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another student's skills"),
            @ApiResponse(responseCode = "404", description = "Student or skill not found"),
            @ApiResponse(responseCode = "409", description = "Skill already assigned to student")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @PostMapping("/{studentId}/skills/{skillId}")
    public ResponseEntity<List<SkillResponse>> assignSkillToStudent(
            @PathVariable Long studentId,
            @PathVariable Long skillId) {
        List<SkillResponse> skills = studentService.assignSkillToStudent(studentId, skillId);
        return ResponseEntity.ok(skills);
    }

    @Operation(summary = "Remove skill from student", description = "Remove an assigned skill from a student. Requires ADMIN or profile-owner STUDENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Skill removed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another student's skills"),
            @ApiResponse(responseCode = "404", description = "Student or skill assignment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @DeleteMapping("/{studentId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long skillId) {
        studentService.removeSkillFromStudent(studentId, skillId);
        return ResponseEntity.noContent().build();
    }
}

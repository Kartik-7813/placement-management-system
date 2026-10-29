package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.dto.SkillResponse;
import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.service.StudentService;
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

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

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

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        StudentResponse response = studentService.getStudentById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY', 'STUDENT')")
    @GetMapping("/{studentId}/skills")
    public ResponseEntity<List<SkillResponse>> getStudentSkills(@PathVariable Long studentId) {
        List<SkillResponse> skills = studentService.getStudentSkills(studentId);
        return ResponseEntity.ok(skills);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @PostMapping("/{studentId}/skills/{skillId}")
    public ResponseEntity<List<SkillResponse>> assignSkillToStudent(
            @PathVariable Long studentId,
            @PathVariable Long skillId) {
        List<SkillResponse> skills = studentService.assignSkillToStudent(studentId, skillId);
        return ResponseEntity.ok(skills);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @DeleteMapping("/{studentId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long skillId) {
        studentService.removeSkillFromStudent(studentId, skillId);
        return ResponseEntity.noContent().build();
    }
}

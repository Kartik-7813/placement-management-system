package com.placementmanagementsystem.service;

import com.placementmanagementsystem.dto.SkillResponse;
import com.placementmanagementsystem.dto.StudentRequest;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.entity.Skill;
import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.StudentStatus;
import com.placementmanagementsystem.exception.DuplicateStudentException;
import com.placementmanagementsystem.exception.SkillAlreadyAssignedException;
import com.placementmanagementsystem.exception.SkillNotFoundException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.repository.SkillRepository;
import com.placementmanagementsystem.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;

    public StudentService(StudentRepository studentRepository, SkillRepository skillRepository) {
        this.studentRepository = studentRepository;
        this.skillRepository = skillRepository;
    }

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateStudentException("Student with roll number " + request.getRollNumber() + " already exists");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateStudentException("Student with email " + request.getEmail() + " already exists");
        }

        Student student = toEntity(request);
        if (student.getStatus() == null) {
            student.setStatus(StudentStatus.ACTIVE);
        }

        Student savedStudent = studentRepository.save(student);
        return toResponse(savedStudent);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(String department, StudentStatus status, String name, Pageable pageable) {
        String searchName = (name != null && !name.trim().isEmpty()) ? name.trim() : null;
        String searchDept = (department != null && !department.trim().isEmpty()) ? department.trim() : null;

        Page<Student> students = studentRepository.searchStudents(searchName, searchDept, status, pageable);
        return students.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
        return toResponse(student);
    }

    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));

        if (!existingStudent.getRollNumber().equals(request.getRollNumber()) &&
                studentRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateStudentException("Student with roll number " + request.getRollNumber() + " already exists");
        }

        if (!existingStudent.getEmail().equals(request.getEmail()) &&
                studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateStudentException("Student with email " + request.getEmail() + " already exists");
        }

        existingStudent.setRollNumber(request.getRollNumber());
        existingStudent.setName(request.getName());
        existingStudent.setEmail(request.getEmail());
        existingStudent.setPhone(request.getPhone());
        existingStudent.setDob(request.getDob());
        existingStudent.setGender(request.getGender());
        existingStudent.setDepartment(request.getDepartment());
        existingStudent.setCgpa(request.getCgpa());
        existingStudent.setPassingYear(request.getPassingYear());
        existingStudent.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            existingStudent.setStatus(request.getStatus());
        }

        Student updatedStudent = studentRepository.save(existingStudent);
        return toResponse(updatedStudent);
    }

    public void deactivateStudent(Long id) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
        existingStudent.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(existingStudent);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getStudentSkills(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        return student.getSkills().stream()
                .map(this::toSkillResponse)
                .collect(Collectors.toList());
    }

    public List<SkillResponse> assignSkillToStudent(Long studentId, Long skillId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillNotFoundException("Skill not found with id: " + skillId));

        boolean alreadyAssigned = student.getSkills().stream()
                .anyMatch(s -> s.getSkillId().equals(skillId));
        if (alreadyAssigned) {
            throw new SkillAlreadyAssignedException("Skill '" + skill.getName() + "' is already assigned to student with id: " + studentId);
        }

        student.getSkills().add(skill);
        Student savedStudent = studentRepository.save(student);

        return savedStudent.getSkills().stream()
                .map(this::toSkillResponse)
                .collect(Collectors.toList());
    }

    public void removeSkillFromStudent(Long studentId, Long skillId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillNotFoundException("Skill not found with id: " + skillId));

        student.getSkills().remove(skill);
        studentRepository.save(student);
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getStudentId(),
                student.getRollNumber(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getDob(),
                student.getGender(),
                student.getDepartment(),
                student.getCgpa(),
                student.getPassingYear(),
                student.getAddress(),
                student.getStatus()
        );
    }

    private Student toEntity(StudentRequest request) {
        Student student = new Student();
        student.setRollNumber(request.getRollNumber());
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDob(request.getDob());
        student.setGender(request.getGender());
        student.setDepartment(request.getDepartment());
        student.setCgpa(request.getCgpa());
        student.setPassingYear(request.getPassingYear());
        student.setAddress(request.getAddress());
        student.setStatus(request.getStatus());
        return student;
    }

    private SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(skill.getSkillId(), skill.getName());
    }
}

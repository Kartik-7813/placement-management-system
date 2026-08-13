package com.placementmanagementsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.enums.StudentStatus;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNumber(String rollNumber);

    Optional<Student> findByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    boolean existsByEmail(String email);

    List<Student> findByDepartment(String department);

    Page<Student> findByDepartment(String department, Pageable pageable);

    List<Student> findByStatus(StudentStatus status);

    Page<Student> findByStatus(StudentStatus status, Pageable pageable);

    long countByStatus(StudentStatus status);

    Page<Student> findByDepartmentAndStatus(String department, StudentStatus status, Pageable pageable);

    Page<Student> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:department IS NULL OR s.department = :department) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<Student> searchStudents(
            @Param("name") String name,
            @Param("department") String department,
            @Param("status") StudentStatus status,
            Pageable pageable
    );
}

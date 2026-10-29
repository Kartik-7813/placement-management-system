package com.placementmanagementsystem.security;

import com.placementmanagementsystem.entity.Student;
import com.placementmanagementsystem.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper component for extracting authenticated user details from the SecurityContext
 * and performing student ownership verification across read and write boundaries.
 */
@Component
public class SecurityHelper {

    private final StudentRepository studentRepository;

    public SecurityHelper(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Retrieves the email/username of the currently authenticated user.
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return auth.getName();
    }

    /**
     * Checks if the currently authenticated user has the specified authority.
     */
    public boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the Student entity corresponding to the currently authenticated user.
     */
    public Student getCurrentStudent() {
        String email = getCurrentUserEmail();
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("No student profile linked to authenticated account"));
    }

    /**
     * Enforces read access on student-associated resources:
     * - ROLE_ADMIN and ROLE_COMPANY can read candidate profiles, applications, interviews, and placements.
     * - ROLE_STUDENT can only read their own resource matching targetStudentId.
     * - Other roles or unauthenticated users are denied.
     */
    public void enforceStudentReadAccess(Long targetStudentId) {
        if (hasAuthority("ROLE_ADMIN") || hasAuthority("ROLE_COMPANY")) {
            return;
        }
        if (hasAuthority("ROLE_STUDENT")) {
            if (targetStudentId == null) {
                throw new AccessDeniedException("Invalid target student identifier");
            }
            Student currentStudent = getCurrentStudent();
            if (!currentStudent.getStudentId().equals(targetStudentId)) {
                throw new AccessDeniedException("You do not have permission to access another student's resource");
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    /**
     * Enforces strict student ownership on mutation operations (profile updates, skill additions, application submissions):
     * - ROLE_ADMIN can perform management actions.
     * - ROLE_STUDENT can only mutate their own resource matching targetStudentId.
     * - ROLE_COMPANY cannot mutate student resources.
     */
    public void enforceStudentOwnership(Long targetStudentId) {
        if (hasAuthority("ROLE_ADMIN")) {
            return;
        }
        if (hasAuthority("ROLE_STUDENT")) {
            if (targetStudentId == null) {
                throw new AccessDeniedException("Invalid target student identifier");
            }
            Student currentStudent = getCurrentStudent();
            if (!currentStudent.getStudentId().equals(targetStudentId)) {
                throw new AccessDeniedException("You do not have permission to access or modify another student's resource");
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}

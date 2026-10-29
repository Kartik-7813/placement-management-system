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
 * and performing student ownership verification.
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
     * Enforces that the currently authenticated user is either an ADMIN or the student identified by targetStudentId.
     * Throws AccessDeniedException if the user is a student attempting to access another student's record.
     */
    public void enforceStudentOwnership(Long targetStudentId) {
        if (hasAuthority("ROLE_ADMIN")) {
            return; // Admins bypass student ownership
        }
        if (hasAuthority("ROLE_STUDENT")) {
            Student currentStudent = getCurrentStudent();
            if (!currentStudent.getStudentId().equals(targetStudentId)) {
                throw new AccessDeniedException("You do not have permission to access or modify another student's resource");
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}

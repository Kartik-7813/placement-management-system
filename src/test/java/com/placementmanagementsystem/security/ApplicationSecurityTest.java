package com.placementmanagementsystem.security;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.controller.ApplicationController;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.ApplicationUpdateRequest;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ApplicationController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("Application Privacy & Security Tests")
class ApplicationSecurityTest {

    private static final String STUDENT_TOKEN = "student-token";
    private static final String COMPANY_TOKEN = "company-token";
    private static final String ADMIN_TOKEN = "admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        mockUserWithToken(STUDENT_TOKEN, "student@example.com", "ROLE_STUDENT");
        mockUserWithToken(COMPANY_TOKEN, "recruiter@company.com", "ROLE_COMPANY");
        mockUserWithToken(ADMIN_TOKEN, "admin@college.edu", "ROLE_ADMIN");
    }

    private void mockUserWithToken(String token, String email, String role) {
        UserDetails userDetails = new User(
                email,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private ApplicationResponse createSampleResponse(Long appId, Long studentId) {
        return new ApplicationResponse(
                appId,
                studentId,
                "Student Name",
                100L,
                "Software Engineer",
                "Acme Corp",
                LocalDateTime.now(),
                ApplicationStatus.APPLIED,
                "https://storage.example.com/resume.pdf",
                "Excited to apply"
        );
    }

    @Test
    @DisplayName("Anonymous request to /api/applications returns 401 Unauthorized")
    void anonymous_AccessApplications_Returns401() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Student can view their own application list")
    void student_ViewOwnApplications_Returns200() throws Exception {
        when(applicationService.getAllApplications(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createSampleResponse(1L, 1L))));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Student attempting to query another student's applications returns 403 Forbidden")
    void student_QueryOtherStudentApplications_Returns403() throws Exception {
        when(applicationService.getAllApplications(eq(2L), any(), any(), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to view another student's applications"));

        mockMvc.perform(get("/api/applications?studentId=2")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied. You do not have permission to access this resource.")));
    }

    @Test
    @DisplayName("Student can view their own application by ID")
    void student_ViewOwnApplicationById_Returns200() throws Exception {
        when(applicationService.getApplicationById(1L))
                .thenReturn(createSampleResponse(1L, 1L));

        mockMvc.perform(get("/api/applications/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId", is(1)));
    }

    @Test
    @DisplayName("Student accessing another student's application by ID returns 403 Forbidden")
    void student_ViewOtherStudentApplicationById_Returns403() throws Exception {
        when(applicationService.getApplicationById(2L))
                .thenThrow(new AccessDeniedException("You do not have permission to access another student's resource"));

        mockMvc.perform(get("/api/applications/2")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("Company user can view candidate application by ID")
    void company_ViewApplicationById_Returns200() throws Exception {
        when(applicationService.getApplicationById(1L))
                .thenReturn(createSampleResponse(1L, 1L));

        mockMvc.perform(get("/api/applications/1")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId", is(1)));
    }

    @Test
    @DisplayName("Student can update their own application resume")
    void student_UpdateOwnApplication_Returns200() throws Exception {
        when(applicationService.updateApplication(eq(1L), any(ApplicationUpdateRequest.class)))
                .thenReturn(createSampleResponse(1L, 1L));

        mockMvc.perform(put("/api/applications/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "resumeUrl": "https://storage.example.com/updated_resume.pdf",
                                    "remarks": "Updated contact details"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Student updating another student's application returns 403 Forbidden")
    void student_UpdateOtherStudentApplication_Returns403() throws Exception {
        when(applicationService.updateApplication(eq(2L), any(ApplicationUpdateRequest.class)))
                .thenThrow(new AccessDeniedException("You do not have permission to access or modify another student's resource"));

        mockMvc.perform(put("/api/applications/2")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "resumeUrl": "https://storage.example.com/malicious_resume.pdf",
                                    "remarks": "Tampered"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("Student cannot change application status (returns 403 Forbidden)")
    void student_ChangeApplicationStatus_Returns403() throws Exception {
        mockMvc.perform(put("/api/applications/1/status?status=SHORTLISTED")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("Company user can update candidate application status")
    void company_ChangeApplicationStatus_Returns200() throws Exception {
        ApplicationResponse updatedResponse = createSampleResponse(1L, 1L);
        when(applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/applications/1/status?status=SHORTLISTED")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company user cannot submit applications (returns 403 Forbidden)")
    void company_CreateApplication_Returns403() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "studentId": 1,
                                    "jobId": 10,
                                    "resumeUrl": "https://storage.example.com/resume.pdf"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }
}

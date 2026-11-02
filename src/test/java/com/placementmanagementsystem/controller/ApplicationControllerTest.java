package com.placementmanagementsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementmanagementsystem.dto.ApplicationRequest;
import com.placementmanagementsystem.dto.ApplicationResponse;
import com.placementmanagementsystem.dto.ApplicationUpdateRequest;
import com.placementmanagementsystem.enums.ApplicationStatus;
import com.placementmanagementsystem.exception.ApplicationAlreadyExistsException;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApplicationController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String STUDENT_TOKEN = "valid.student.jwt";
    private static final String ADMIN_TOKEN = "valid.admin.jwt";

    private void mockToken(String token, String email, String role) {
        UserDetails userDetails = new User(email, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private ApplicationResponse createSampleResponse(Long id) {
        ApplicationResponse response = new ApplicationResponse();
        response.setApplicationId(id);
        response.setStudentId(10L);
        response.setStudentName("Jane Doe");
        response.setJobId(20L);
        response.setJobTitle("Software Engineer");
        response.setCompanyName("Tech Corp");
        response.setAppliedOn(LocalDateTime.now());
        response.setStatus(ApplicationStatus.APPLIED);
        response.setResumeUrl("https://example.com/resume.pdf");
        response.setRemarks("Top candidate");
        return response;
    }

    // =========================================================
    // POST /api/applications (Create Application)
    // =========================================================

    @Test
    @DisplayName("POST /api/applications - 201 Created on valid application request")
    void createApplication_Success_Returns201() throws Exception {
        mockToken(STUDENT_TOKEN, "student@test.com", "ROLE_STUDENT");

        ApplicationRequest request = new ApplicationRequest(10L, 20L, "https://example.com/resume.pdf", "Applying for SWE");
        ApplicationResponse response = createSampleResponse(1L);

        when(applicationService.createApplication(any(ApplicationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(1L))
                .andExpect(jsonPath("$.studentId").value(10L))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        verify(applicationService).createApplication(any(ApplicationRequest.class));
    }

    @Test
    @DisplayName("POST /api/applications - 400 Bad Request on missing required fields")
    void createApplication_MissingRequiredFields_Returns400() throws Exception {
        mockToken(STUDENT_TOKEN, "student@test.com", "ROLE_STUDENT");

        ApplicationRequest request = new ApplicationRequest(); // missing studentId and jobId

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.studentId").exists())
                .andExpect(jsonPath("$.validationErrors.jobId").exists());
    }

    @Test
    @DisplayName("POST /api/applications - 409 Conflict when student already applied to job")
    void createApplication_DuplicateApplication_Returns409() throws Exception {
        mockToken(STUDENT_TOKEN, "student@test.com", "ROLE_STUDENT");

        ApplicationRequest request = new ApplicationRequest(10L, 20L, "https://example.com/resume.pdf", "Applying again");

        when(applicationService.createApplication(any(ApplicationRequest.class)))
                .thenThrow(new ApplicationAlreadyExistsException("Student has already applied to this job"));

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Student has already applied to this job"));
    }

    @Test
    @DisplayName("POST /api/applications - 401 Unauthorized when unauthenticated")
    void createApplication_Unauthenticated_Returns401() throws Exception {
        ApplicationRequest request = new ApplicationRequest(10L, 20L, "https://example.com/resume.pdf", "Applying");

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/applications (Get All Paginated)
    // =========================================================

    @Test
    @DisplayName("GET /api/applications - 200 OK with paginated application list")
    void getAllApplications_Success_Returns200WithPage() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        List<ApplicationResponse> content = List.of(createSampleResponse(1L));
        Page<ApplicationResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(applicationService.getAllApplications(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].applicationId").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================
    // GET /api/applications/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/applications/{id} - 200 OK when application exists")
    void getApplicationById_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        ApplicationResponse response = createSampleResponse(1L);
        when(applicationService.getApplicationById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/applications/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(1L))
                .andExpect(jsonPath("$.studentName").value("Jane Doe"))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    @DisplayName("GET /api/applications/{id} - 404 Not Found when application does not exist")
    void getApplicationById_NotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        when(applicationService.getApplicationById(99L))
                .thenThrow(new ApplicationNotFoundException("Application not found with ID: 99"));

        mockMvc.perform(get("/api/applications/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Application not found with ID: 99"));
    }

    // =========================================================
    // PUT /api/applications/{id} (Update details)
    // =========================================================

    @Test
    @DisplayName("PUT /api/applications/{id} - 200 OK on valid update request")
    void updateApplication_Success_Returns200() throws Exception {
        mockToken(STUDENT_TOKEN, "student@test.com", "ROLE_STUDENT");

        ApplicationUpdateRequest request = new ApplicationUpdateRequest("https://example.com/updated.pdf", "Updated remarks");
        ApplicationResponse response = createSampleResponse(1L);
        response.setResumeUrl("https://example.com/updated.pdf");
        response.setRemarks("Updated remarks");

        when(applicationService.updateApplication(eq(1L), any(ApplicationUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/applications/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(1L))
                .andExpect(jsonPath("$.resumeUrl").value("https://example.com/updated.pdf"))
                .andExpect(jsonPath("$.remarks").value("Updated remarks"));
    }

    @Test
    @DisplayName("PUT /api/applications/{id} - 400 Bad Request when resumeUrl exceeds limit")
    void updateApplication_ValidationError_Returns400() throws Exception {
        mockToken(STUDENT_TOKEN, "student@test.com", "ROLE_STUDENT");

        ApplicationUpdateRequest request = new ApplicationUpdateRequest("https://example.com/" + "a".repeat(250), "Updated remarks");

        mockMvc.perform(put("/api/applications/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.resumeUrl").exists());
    }

    // =========================================================
    // PUT /api/applications/{id}/status (Update status)
    // =========================================================

    @Test
    @DisplayName("PUT /api/applications/{id}/status - 200 OK on valid status update")
    void updateApplicationStatus_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        ApplicationResponse response = createSampleResponse(1L);
        response.setStatus(ApplicationStatus.SHORTLISTED);

        when(applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED))
                .thenReturn(response);

        mockMvc.perform(put("/api/applications/1/status")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("status", "SHORTLISTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(1L))
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}/status - 400 Bad Request when status query param is missing")
    void updateApplicationStatus_MissingParam_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(put("/api/applications/1/status")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

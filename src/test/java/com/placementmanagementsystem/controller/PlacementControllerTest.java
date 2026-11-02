package com.placementmanagementsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.PlacementRequest;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.enums.PlacementStatus;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.exception.PlacementAlreadyExistsException;
import com.placementmanagementsystem.exception.PlacementNotFoundException;
import com.placementmanagementsystem.exception.StudentNotFoundException;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.PlacementService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(controllers = PlacementController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class PlacementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private PlacementService placementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin.jwt.token";

    private void mockToken(String token, String email, String role) {
        UserDetails userDetails = new User(email, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private PlacementResponse createSampleResponse(Long id) {
        PlacementResponse response = new PlacementResponse();
        response.setPlacementId(id);
        response.setStudentId(10L);
        response.setStudentName("John Doe");
        response.setApplicationId(20L);
        response.setJobRole("Software Engineer");
        response.setPackageAmount(new BigDecimal("1200000.00"));
        response.setJoiningDate(LocalDate.now().plusMonths(2));
        response.setStatus(PlacementStatus.CONFIRMED);
        response.setPlacedOn(LocalDateTime.now());
        return response;
    }

    // =========================================================
    // POST /api/placements (Create Placement)
    // =========================================================

    @Test
    @DisplayName("POST /api/placements - 201 Created on valid placement request")
    void createPlacement_Success_Returns201() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(
                10L, 20L, "Software Engineer",
                new BigDecimal("1200000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );
        PlacementResponse response = createSampleResponse(1L);

        when(placementService.createPlacement(any(PlacementRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placementId").value(1L))
                .andExpect(jsonPath("$.studentId").value(10L))
                .andExpect(jsonPath("$.jobRole").value("Software Engineer"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(placementService).createPlacement(any(PlacementRequest.class));
    }

    @Test
    @DisplayName("POST /api/placements - 400 Bad Request on missing required fields")
    void createPlacement_MissingRequiredFields_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(); // missing studentId, applicationId, jobRole

        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.studentId").exists())
                .andExpect(jsonPath("$.validationErrors.applicationId").exists())
                .andExpect(jsonPath("$.validationErrors.jobRole").exists());
    }

    @Test
    @DisplayName("POST /api/placements - 404 Not Found when application does not exist")
    void createPlacement_ApplicationNotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(
                10L, 99L, "Software Engineer",
                new BigDecimal("1200000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );

        when(placementService.createPlacement(any(PlacementRequest.class)))
                .thenThrow(new ApplicationNotFoundException("Application not found with ID: 99"));

        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Application not found with ID: 99"));
    }

    @Test
    @DisplayName("POST /api/placements - 409 Conflict when placement already exists")
    void createPlacement_DuplicatePlacement_Returns409() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(
                10L, 20L, "Software Engineer",
                new BigDecimal("1200000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );

        when(placementService.createPlacement(any(PlacementRequest.class)))
                .thenThrow(new PlacementAlreadyExistsException("Placement already exists for this application"));

        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Placement already exists for this application"));
    }

    @Test
    @DisplayName("POST /api/placements - 401 Unauthorized when unauthenticated")
    void createPlacement_Unauthenticated_Returns401() throws Exception {
        PlacementRequest request = new PlacementRequest(
                10L, 20L, "Software Engineer",
                new BigDecimal("1200000.00"),
                LocalDate.now().plusMonths(2),
                PlacementStatus.CONFIRMED
        );

        mockMvc.perform(post("/api/placements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/placements (Paginated list)
    // =========================================================

    @Test
    @DisplayName("GET /api/placements - 200 OK with paginated placements")
    void getAllPlacements_Success_Returns200WithPage() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        List<PlacementResponse> content = List.of(createSampleResponse(1L));
        Page<PlacementResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(placementService.getAllPlacements(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].placementId").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================
    // GET /api/placements/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/placements/{id} - 200 OK when placement exists")
    void getPlacementById_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementResponse response = createSampleResponse(1L);
        when(placementService.getPlacementById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/placements/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementId").value(1L))
                .andExpect(jsonPath("$.jobRole").value("Software Engineer"));
    }

    @Test
    @DisplayName("GET /api/placements/{id} - 404 Not Found when placement does not exist")
    void getPlacementById_NotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        when(placementService.getPlacementById(99L))
                .thenThrow(new PlacementNotFoundException("Placement not found with ID: 99"));

        mockMvc.perform(get("/api/placements/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Placement not found with ID: 99"));
    }

    // =========================================================
    // GET /api/placements/student/{studentId}
    // =========================================================

    @Test
    @DisplayName("GET /api/placements/student/{studentId} - 200 OK when found")
    void getPlacementByStudent_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementResponse response = createSampleResponse(1L);
        when(placementService.getPlacementByStudent(10L)).thenReturn(response);

        mockMvc.perform(get("/api/placements/student/10")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementId").value(1L))
                .andExpect(jsonPath("$.studentId").value(10L));
    }

    // =========================================================
    // GET /api/placements/student/{studentId}/history
    // =========================================================

    @Test
    @DisplayName("GET /api/placements/student/{studentId}/history - 200 OK returns history list")
    void getPlacementHistoryByStudent_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        List<PlacementResponse> history = List.of(createSampleResponse(1L));
        when(placementService.getPlacementHistoryByStudent(10L)).thenReturn(history);

        mockMvc.perform(get("/api/placements/student/10/history")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].placementId").value(1L));
    }

    @Test
    @DisplayName("GET /api/placements/student/{studentId}/history - 404 Not Found when student does not exist")
    void getPlacementHistoryByStudent_StudentNotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        when(placementService.getPlacementHistoryByStudent(99L))
                .thenThrow(new StudentNotFoundException("Student not found with ID: 99"));

        mockMvc.perform(get("/api/placements/student/99/history")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with ID: 99"));
    }

    // =========================================================
    // PUT /api/placements/{id}
    // =========================================================

    @Test
    @DisplayName("PUT /api/placements/{id} - 200 OK on valid update")
    void updatePlacement_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(
                10L, 20L, "Senior Software Engineer",
                new BigDecimal("1500000.00"),
                LocalDate.now().plusMonths(3),
                PlacementStatus.CONFIRMED
        );
        PlacementResponse response = createSampleResponse(1L);
        response.setJobRole("Senior Software Engineer");
        response.setPackageAmount(new BigDecimal("1500000.00"));

        when(placementService.updatePlacement(eq(1L), any(PlacementRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/placements/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementId").value(1L))
                .andExpect(jsonPath("$.jobRole").value("Senior Software Engineer"));
    }

    @Test
    @DisplayName("PUT /api/placements/{id} - 400 Bad Request when package is negative")
    void updatePlacement_NegativePackage_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementRequest request = new PlacementRequest(
                10L, 20L, "Senior Software Engineer",
                new BigDecimal("-5000.00"),
                LocalDate.now().plusMonths(3),
                PlacementStatus.CONFIRMED
        );

        mockMvc.perform(put("/api/placements/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.packageAmount").exists());
    }

    // =========================================================
    // PUT /api/placements/{id}/status
    // =========================================================

    @Test
    @DisplayName("PUT /api/placements/{id}/status - 200 OK on valid status update")
    void updatePlacementStatus_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        PlacementResponse response = createSampleResponse(1L);
        response.setStatus(PlacementStatus.JOINED);

        when(placementService.updatePlacementStatus(1L, PlacementStatus.JOINED)).thenReturn(response);

        mockMvc.perform(put("/api/placements/1/status")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("status", "JOINED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementId").value(1L))
                .andExpect(jsonPath("$.status").value("JOINED"));
    }

    @Test
    @DisplayName("PUT /api/placements/{id}/status - 400 Bad Request when status param missing")
    void updatePlacementStatus_MissingParam_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        mockMvc.perform(put("/api/placements/1/status")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

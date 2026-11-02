package com.placementmanagementsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.InterviewRequest;
import com.placementmanagementsystem.dto.InterviewResponse;
import com.placementmanagementsystem.enums.InterviewResult;
import com.placementmanagementsystem.exception.ApplicationNotFoundException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.exception.InterviewAlreadyExistsException;
import com.placementmanagementsystem.exception.InterviewNotFoundException;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.InterviewService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InterviewController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin.jwt.token";
    private static final String COMPANY_TOKEN = "company.jwt.token";

    private void mockToken(String token, String email, String role) {
        UserDetails userDetails = new User(email, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private InterviewResponse createSampleResponse(Long id) {
        InterviewResponse response = new InterviewResponse();
        response.setInterviewId(id);
        response.setApplicationId(1L);
        response.setRoundNumber(1);
        response.setRoundType("Technical Round 1");
        response.setScheduledOn(LocalDateTime.now().plusDays(2));
        response.setInterviewer("Tech Lead");
        response.setFeedback("Good problem solving skills");
        response.setResult(InterviewResult.PENDING);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    // =========================================================
    // POST /api/interviews (Schedule Interview)
    // =========================================================

    @Test
    @DisplayName("POST /api/interviews - 201 Created on valid interview schedule request")
    void scheduleInterview_Success_Returns201() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewRequest request = new InterviewRequest(
                1L, 1, "Technical Round 1",
                LocalDateTime.now().plusDays(2),
                "Tech Lead", "Good skills", InterviewResult.PENDING
        );
        InterviewResponse response = createSampleResponse(1L);

        when(interviewService.scheduleInterview(any(InterviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewId").value(1L))
                .andExpect(jsonPath("$.applicationId").value(1L))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.roundType").value("Technical Round 1"))
                .andExpect(jsonPath("$.result").value("PENDING"));

        verify(interviewService).scheduleInterview(any(InterviewRequest.class));
    }

    @Test
    @DisplayName("POST /api/interviews - 400 Bad Request on missing required fields")
    void scheduleInterview_MissingRequiredFields_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewRequest request = new InterviewRequest(); // missing applicationId, roundNumber, roundType, scheduledOn

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.applicationId").exists())
                .andExpect(jsonPath("$.validationErrors.roundNumber").exists())
                .andExpect(jsonPath("$.validationErrors.roundType").exists())
                .andExpect(jsonPath("$.validationErrors.scheduledOn").exists());
    }

    @Test
    @DisplayName("POST /api/interviews - 404 Not Found when application does not exist")
    void scheduleInterview_ApplicationNotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewRequest request = new InterviewRequest(
                99L, 1, "Technical Round 1",
                LocalDateTime.now().plusDays(2),
                "Tech Lead", null, InterviewResult.PENDING
        );

        when(interviewService.scheduleInterview(any(InterviewRequest.class)))
                .thenThrow(new ApplicationNotFoundException("Application not found with ID: 99"));

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Application not found with ID: 99"));
    }

    @Test
    @DisplayName("POST /api/interviews - 409 Conflict when interview round already scheduled")
    void scheduleInterview_DuplicateRound_Returns409() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@techcorp.com", "ROLE_COMPANY");

        InterviewRequest request = new InterviewRequest(
                1L, 1, "Technical Round 1",
                LocalDateTime.now().plusDays(2),
                "Tech Lead", null, InterviewResult.PENDING
        );

        when(interviewService.scheduleInterview(any(InterviewRequest.class)))
                .thenThrow(new InterviewAlreadyExistsException("Interview round 1 is already scheduled for this application"));

        mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Interview round 1 is already scheduled for this application"));
    }

    @Test
    @DisplayName("POST /api/interviews - 401 Unauthorized when unauthenticated")
    void scheduleInterview_Unauthenticated_Returns401() throws Exception {
        InterviewRequest request = new InterviewRequest(
                1L, 1, "Technical Round 1",
                LocalDateTime.now().plusDays(2),
                "Tech Lead", null, InterviewResult.PENDING
        );

        mockMvc.perform(post("/api/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET /api/interviews (Paginated list)
    // =========================================================

    @Test
    @DisplayName("GET /api/interviews - 200 OK with paginated interviews")
    void getAllInterviews_Success_Returns200WithPage() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        List<InterviewResponse> content = List.of(createSampleResponse(1L));
        Page<InterviewResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(interviewService.getAllInterviews(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/interviews")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].interviewId").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================
    // GET /api/interviews/{id}
    // =========================================================

    @Test
    @DisplayName("GET /api/interviews/{id} - 200 OK when interview exists")
    void getInterviewById_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewResponse response = createSampleResponse(1L);
        when(interviewService.getInterviewById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/interviews/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(1L))
                .andExpect(jsonPath("$.roundType").value("Technical Round 1"));
    }

    @Test
    @DisplayName("GET /api/interviews/{id} - 404 Not Found when interview does not exist")
    void getInterviewById_NotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        when(interviewService.getInterviewById(99L))
                .thenThrow(new InterviewNotFoundException("Interview not found with ID: 99"));

        mockMvc.perform(get("/api/interviews/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Interview not found with ID: 99"));
    }

    // =========================================================
    // GET /api/interviews/application/{applicationId}
    // =========================================================

    @Test
    @DisplayName("GET /api/interviews/application/{applicationId} - 200 OK returns interview list")
    void getInterviewsByApplication_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        List<InterviewResponse> list = List.of(createSampleResponse(1L));
        when(interviewService.getInterviewsByApplication(1L)).thenReturn(list);

        mockMvc.perform(get("/api/interviews/application/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].interviewId").value(1L));
    }

    // =========================================================
    // PUT /api/interviews/{id}
    // =========================================================

    @Test
    @DisplayName("PUT /api/interviews/{id} - 200 OK on valid update")
    void updateInterview_Success_Returns200() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewRequest request = new InterviewRequest(
                1L, 1, "Technical Round 1 - Updated",
                LocalDateTime.now().plusDays(3),
                "Principal Architect", "Excellent coding skills", InterviewResult.PASSED
        );
        InterviewResponse response = createSampleResponse(1L);
        response.setRoundType("Technical Round 1 - Updated");
        response.setResult(InterviewResult.PASSED);

        when(interviewService.updateInterview(eq(1L), any(InterviewRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/interviews/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(1L))
                .andExpect(jsonPath("$.roundType").value("Technical Round 1 - Updated"))
                .andExpect(jsonPath("$.result").value("PASSED"));
    }

    @Test
    @DisplayName("PUT /api/interviews/{id} - 400 Bad Request when roundNumber is negative")
    void updateInterview_NegativeRoundNumber_Returns400() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        InterviewRequest request = new InterviewRequest(
                1L, -1, "Technical Round 1",
                LocalDateTime.now().plusDays(3),
                "Interviewer", null, InterviewResult.PENDING
        );

        mockMvc.perform(put("/api/interviews/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.roundNumber").exists());
    }

    // =========================================================
    // DELETE /api/interviews/{id}
    // =========================================================

    @Test
    @DisplayName("DELETE /api/interviews/{id} - 204 No Content on successful deletion")
    void deleteInterview_Success_Returns204() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        doNothing().when(interviewService).deleteInterview(1L);

        mockMvc.perform(delete("/api/interviews/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        verify(interviewService).deleteInterview(1L);
    }

    @Test
    @DisplayName("DELETE /api/interviews/{id} - 404 Not Found when interview does not exist")
    void deleteInterview_NotFound_Returns404() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@test.com", "ROLE_ADMIN");

        doThrow(new InterviewNotFoundException("Interview not found with ID: 99"))
                .when(interviewService).deleteInterview(99L);

        mockMvc.perform(delete("/api/interviews/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Interview not found with ID: 99"));
    }
}

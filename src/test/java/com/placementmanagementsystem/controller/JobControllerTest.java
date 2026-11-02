package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.exception.JobNotFoundException;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JobController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("JobController Unit Tests")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String COMPANY_TOKEN = "company-test-token";

    private void mockToken(String token, String email, String role) {
        UserDetails userDetails = new User(
                email,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    private static final String VALID_JOB_JSON = """
            {
                "companyId": 1,
                "title": "Software Engineer Backend",
                "description": "Design and build microservices",
                "eligibilityCriteria": "B.Tech with >= 7.0 CGPA",
                "requiredSkills": "Java, Spring Boot, MySQL",
                "minCgpa": 7.00,
                "packageAmount": 1200000.00,
                "jobType": "Full-Time",
                "deadline": "2026-12-31",
                "status": "ACTIVE"
            }
            """;

    private static final String INVALID_JOB_JSON = """
            {
                "companyId": null,
                "title": "",
                "minCgpa": 15.00
            }
            """;

    @Test
    @DisplayName("POST /api/jobs should return 201 Created when job is posted")
    void createJob_Success() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");

        JobResponse response = new JobResponse(
                10L, 1L, "Acme Innovations", "Software Engineer Backend",
                "Design and build microservices", "B.Tech with >= 7.0 CGPA",
                "Java, Spring Boot, MySQL", new BigDecimal("7.00"),
                new BigDecimal("1200000.00"), "Full-Time", LocalDate.of(2026, 12, 31), JobStatus.ACTIVE
        );
        when(jobService.createJob(any(JobRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JOB_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId", is(10)))
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.companyName", is("Acme Innovations")))
                .andExpect(jsonPath("$.title", is("Software Engineer Backend")))
                .andExpect(jsonPath("$.packageAmount", is(1200000.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(jobService).createJob(any(JobRequest.class));
    }

    @Test
    @DisplayName("POST /api/jobs with invalid payload should return 400 Bad Request with field errors")
    void createJob_ValidationError() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_JOB_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.companyId", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.title", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.minCgpa", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/jobs when company not found should return 404 Not Found")
    void createJob_CompanyNotFound() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");
        when(jobService.createJob(any(JobRequest.class)))
                .thenThrow(new CompanyNotFoundException("Company not found with id: 1"));

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JOB_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Company not found with id: 1")));
    }

    @Test
    @DisplayName("GET /api/jobs should be publicly accessible and return 200 OK with paginated jobs")
    void getAllJobs_PublicSuccess() throws Exception {
        JobResponse response = new JobResponse(
                10L, 1L, "Acme Innovations", "Software Engineer Backend",
                "Design and build microservices", "B.Tech with >= 7.0 CGPA",
                "Java, Spring Boot, MySQL", new BigDecimal("7.00"),
                new BigDecimal("1200000.00"), "Full-Time", LocalDate.of(2026, 12, 31), JobStatus.ACTIVE
        );
        when(jobService.getAllJobs(eq("Backend"), eq(1L), eq(JobStatus.ACTIVE), eq(new BigDecimal("7.00")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/jobs")
                        .param("title", "Backend")
                        .param("companyId", "1")
                        .param("status", "ACTIVE")
                        .param("minCgpa", "7.00")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].jobId", is(10)))
                .andExpect(jsonPath("$.content[0].title", is("Software Engineer Backend")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(jobService).getAllJobs(eq("Backend"), eq(1L), eq(JobStatus.ACTIVE), eq(new BigDecimal("7.00")), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/jobs/{id} should be publicly accessible and return 200 OK when found")
    void getJobById_PublicSuccess() throws Exception {
        JobResponse response = new JobResponse(
                10L, 1L, "Acme Innovations", "Software Engineer Backend",
                "Design and build microservices", "B.Tech with >= 7.0 CGPA",
                "Java, Spring Boot, MySQL", new BigDecimal("7.00"),
                new BigDecimal("1200000.00"), "Full-Time", LocalDate.of(2026, 12, 31), JobStatus.ACTIVE
        );
        when(jobService.getJobById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/jobs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(10)))
                .andExpect(jsonPath("$.title", is("Software Engineer Backend")));

        verify(jobService).getJobById(10L);
    }

    @Test
    @DisplayName("GET /api/jobs/{id} should return 404 Not Found when job does not exist")
    void getJobById_NotFound() throws Exception {
        when(jobService.getJobById(999L)).thenThrow(new JobNotFoundException("Job not found with id: 999"));

        mockMvc.perform(get("/api/jobs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Job not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /api/jobs/{id} should return 200 OK when job is updated")
    void updateJob_Success() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");

        JobResponse response = new JobResponse(
                10L, 1L, "Acme Innovations", "Lead Backend Engineer",
                "Design and build microservices", "B.Tech with >= 7.0 CGPA",
                "Java, Spring Boot, MySQL", new BigDecimal("7.00"),
                new BigDecimal("1500000.00"), "Full-Time", LocalDate.of(2026, 12, 31), JobStatus.ACTIVE
        );
        when(jobService.updateJob(eq(10L), any(JobRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/jobs/10")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JOB_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(10)))
                .andExpect(jsonPath("$.title", is("Lead Backend Engineer")))
                .andExpect(jsonPath("$.packageAmount", is(1500000.00)));

        verify(jobService).updateJob(eq(10L), any(JobRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/jobs/{id} should return 204 No Content when job is closed")
    void closeJob_Success() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");
        doNothing().when(jobService).closeJob(10L);

        mockMvc.perform(delete("/api/jobs/10")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN))
                .andExpect(status().isNoContent());

        verify(jobService).closeJob(10L);
    }

    @Test
    @DisplayName("DELETE /api/jobs/{id} should return 404 Not Found when job does not exist")
    void closeJob_NotFound() throws Exception {
        mockToken(COMPANY_TOKEN, "recruiter@acme.example.com", "ROLE_COMPANY");
        org.mockito.Mockito.doThrow(new JobNotFoundException("Job not found with id: 999")).when(jobService).closeJob(999L);

        mockMvc.perform(delete("/api/jobs/999")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Job not found with id: 999")));
    }
}

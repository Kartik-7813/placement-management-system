package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.DashboardResponse;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin-test-token";
    private static final String STUDENT_TOKEN = "student-test-token";

    private void mockToken(String token, String email, String role) {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        email,
                        "password",
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))
                );
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/dashboard/summary should return 200 OK with dashboard metrics")
    void getDashboardSummary_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        DashboardResponse response = new DashboardResponse();
        response.setTotalStudents(100L);
        response.setActiveStudents(90L);
        response.setInactiveStudents(10L);
        response.setTotalCompanies(20L);
        response.setActiveCompanies(18L);
        response.setInactiveCompanies(2L);
        response.setTotalJobs(15L);
        response.setActiveJobs(12L);
        response.setClosedJobs(3L);
        response.setTotalApplications(50L);
        response.setTotalPlacements(10L);
        when(dashboardService.getDashboardSummary()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents", is(100)))
                .andExpect(jsonPath("$.activeStudents", is(90)))
                .andExpect(jsonPath("$.inactiveStudents", is(10)))
                .andExpect(jsonPath("$.totalCompanies", is(20)))
                .andExpect(jsonPath("$.activeCompanies", is(18)))
                .andExpect(jsonPath("$.totalJobs", is(15)))
                .andExpect(jsonPath("$.activeJobs", is(12)))
                .andExpect(jsonPath("$.totalApplications", is(50)))
                .andExpect(jsonPath("$.totalPlacements", is(10)));

        verify(dashboardService).getDashboardSummary();
    }

    @Test
    @DisplayName("GET /api/dashboard/summary without authentication should return 401 Unauthorized")
    void getDashboardSummary_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary with non-admin role should return 403 Forbidden")
    void getDashboardSummary_Forbidden() throws Exception {
        mockToken(STUDENT_TOKEN, "student@example.com", "ROLE_STUDENT");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }
}

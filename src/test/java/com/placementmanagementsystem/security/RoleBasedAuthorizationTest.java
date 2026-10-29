package com.placementmanagementsystem.security;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.controller.CompanyController;
import com.placementmanagementsystem.controller.DashboardController;
import com.placementmanagementsystem.controller.JobController;
import com.placementmanagementsystem.controller.PlacementController;
import com.placementmanagementsystem.controller.StudentController;
import com.placementmanagementsystem.dto.DashboardResponse;
import com.placementmanagementsystem.dto.JobResponse;
import com.placementmanagementsystem.dto.PlacementResponse;
import com.placementmanagementsystem.dto.StudentResponse;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.DashboardService;
import com.placementmanagementsystem.service.JobService;
import com.placementmanagementsystem.service.PlacementService;
import com.placementmanagementsystem.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        StudentController.class,
        CompanyController.class,
        JobController.class,
        PlacementController.class,
        DashboardController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("Role-Based Authorization & Resource Ownership Tests")
class RoleBasedAuthorizationTest {

    private static final String ADMIN_TOKEN = "admin-test-token";
    private static final String STUDENT_TOKEN = "student-test-token";
    private static final String COMPANY_TOKEN = "company-test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private PlacementService placementService;

    @MockitoBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        mockUserWithToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        mockUserWithToken(STUDENT_TOKEN, "student@example.com", "ROLE_STUDENT");
        mockUserWithToken(COMPANY_TOKEN, "company@example.com", "ROLE_COMPANY");
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

    @Test
    @DisplayName("ROLE_STUDENT cannot access admin dashboard summary (returns 403 Forbidden)")
    void student_AccessDashboardSummary_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied. You do not have permission to access this resource.")));
    }

    @Test
    @DisplayName("ROLE_ADMIN can access dashboard summary")
    void admin_AccessDashboardSummary_ShouldReturn200() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(new DashboardResponse());

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot create placement (admin-only endpoint, returns 403 Forbidden)")
    void student_CreatePlacement_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "studentId": 1,
                                    "applicationId": 10,
                                    "jobRole": "Software Engineer",
                                    "packageAmount": 850000.00,
                                    "joiningDate": "2026-07-01",
                                    "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("ROLE_ADMIN can create placement")
    void admin_CreatePlacement_ShouldReturn201() throws Exception {
        when(placementService.createPlacement(any())).thenReturn(new PlacementResponse());

        mockMvc.perform(post("/api/placements")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "studentId": 1,
                                    "applicationId": 10,
                                    "jobRole": "Software Engineer",
                                    "packageAmount": 850000.00,
                                    "joiningDate": "2026-07-01",
                                    "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot deactivate a student profile (admin-only endpoint, returns 403 Forbidden)")
    void student_DeactivateStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/students/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("ROLE_STUDENT accessing another student's profile receives 403 Forbidden via ownership check")
    void student_AccessOtherStudent_ShouldReturn403() throws Exception {
        when(studentService.getStudentById(2L))
                .thenThrow(new AccessDeniedException("You do not have permission to access or modify another student's resource"));

        mockMvc.perform(get("/api/students/2")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access is denied. You do not have permission to access this resource.")));
    }

    @Test
    @DisplayName("ROLE_STUDENT accessing their own profile receives 200 OK")
    void student_AccessOwnStudent_ShouldReturn200() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(new StudentResponse());

        mockMvc.perform(get("/api/students/1")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_COMPANY cannot create a student profile (returns 403 Forbidden)")
    void company_CreateStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "rollNumber": "CS2026001",
                                    "name": "John Doe",
                                    "email": "john@example.com"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("ROLE_COMPANY can create a job listing")
    void company_CreateJob_ShouldReturn201() throws Exception {
        when(jobService.createJob(any())).thenReturn(new JobResponse());

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "companyId": 1,
                                    "title": "Software Engineer",
                                    "department": "IT",
                                    "location": "Bangalore"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot create a job listing (returns 403 Forbidden)")
    void student_CreateJob_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "companyId": 1,
                                    "title": "Software Engineer",
                                    "department": "IT",
                                    "location": "Bangalore"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }
}

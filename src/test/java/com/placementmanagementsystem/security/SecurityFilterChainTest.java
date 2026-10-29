package com.placementmanagementsystem.security;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.controller.CompanyController;
import com.placementmanagementsystem.controller.JobController;
import com.placementmanagementsystem.controller.StudentController;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.JobService;
import com.placementmanagementsystem.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {JobController.class, CompanyController.class, StudentController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("SecurityFilterChain Integration Tests")
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private StudentService studentService;

    private UserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(
                "user@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("Anonymous request to protected GET /api/students should return 401 Unauthorized JSON")
    void anonymousRequest_ToProtectedEndpoint_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource.")))
                .andExpect(jsonPath("$.path", is("/api/students")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Anonymous request to public GET /api/jobs should return 200 OK without token")
    void anonymousRequest_ToPublicJobsEndpoint_ShouldReturn200() throws Exception {
        when(jobService.getAllJobs(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous request to public GET /api/companies should return 200 OK without token")
    void anonymousRequest_ToPublicCompaniesEndpoint_ShouldReturn200() throws Exception {
        when(companyService.getAllCompanies(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous POST to protected /api/jobs should return 401 Unauthorized JSON")
    void anonymousRequest_ToProtectedJobCreation_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Request with valid JWT should authenticate and access protected GET /api/students")
    void validJwt_ShouldAuthenticateAndAccessProtectedEndpoint() throws Exception {
        String validToken = "valid.jwt.token";

        when(jwtService.extractUsername(validToken)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUser);
        when(jwtService.isTokenValid(validToken, mockUser)).thenReturn(true);
        when(studentService.getAllStudents(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request with malformed or expired JWT should return 401 Unauthorized JSON")
    void invalidJwt_ShouldReturn401Unauthorized() throws Exception {
        String invalidToken = "invalid.or.expired.token";

        when(jwtService.extractUsername(invalidToken)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUser);
        when(jwtService.isTokenValid(invalidToken, mockUser)).thenReturn(false);

        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}

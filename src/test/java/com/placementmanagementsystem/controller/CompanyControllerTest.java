package com.placementmanagementsystem.controller;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.CompanyResponse;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.exception.CompanyNotFoundException;
import com.placementmanagementsystem.exception.GlobalExceptionHandler;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.service.CompanyService;
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

@WebMvcTest(controllers = CompanyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("CompanyController Unit Tests")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String ADMIN_TOKEN = "admin-test-token";
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

    private static final String VALID_COMPANY_JSON = """
            {
                "name": "Acme Innovations",
                "website": "https://acme.example.com",
                "contactPerson": "Alice Walker",
                "email": "hr@acme.example.com",
                "phone": "9876543210",
                "address": "Bangalore, India",
                "status": "ACTIVE"
            }
            """;

    private static final String INVALID_COMPANY_JSON = """
            {
                "name": "",
                "email": "invalid-email"
            }
            """;

    @Test
    @DisplayName("POST /api/companies should return 201 Created when admin creates company")
    void createCompany_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        CompanyResponse response = new CompanyResponse(
                1L, "Acme Innovations", "https://acme.example.com",
                "Alice Walker", "hr@acme.example.com", "9876543210",
                "Bangalore, India", CompanyStatus.ACTIVE
        );
        when(companyService.createCompany(any(CompanyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COMPANY_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Innovations")))
                .andExpect(jsonPath("$.email", is("hr@acme.example.com")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(companyService).createCompany(any(CompanyRequest.class));
    }

    @Test
    @DisplayName("POST /api/companies with invalid data should return 400 Bad Request with field errors")
    void createCompany_ValidationError() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_COMPANY_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/companies with duplicate name/email should return 409 Conflict")
    void createCompany_DuplicateConflict() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        when(companyService.createCompany(any(CompanyRequest.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry 'hr@acme.example.com' for key 'companies.email'"));

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COMPANY_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("GET /api/companies should be publicly accessible and return 200 OK with paginated companies")
    void getAllCompanies_PublicSuccess() throws Exception {
        CompanyResponse company = new CompanyResponse(
                1L, "Acme Innovations", "https://acme.example.com",
                "Alice Walker", "hr@acme.example.com", "9876543210",
                "Bangalore, India", CompanyStatus.ACTIVE
        );
        when(companyService.getAllCompanies(eq("Acme"), eq(CompanyStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(company), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/companies")
                        .param("name", "Acme")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].companyId", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Acme Innovations")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(companyService).getAllCompanies(eq("Acme"), eq(CompanyStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/companies/{id} should be publicly accessible and return 200 OK when found")
    void getCompanyById_PublicSuccess() throws Exception {
        CompanyResponse company = new CompanyResponse(
                1L, "Acme Innovations", "https://acme.example.com",
                "Alice Walker", "hr@acme.example.com", "9876543210",
                "Bangalore, India", CompanyStatus.ACTIVE
        );
        when(companyService.getCompanyById(1L)).thenReturn(company);

        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Innovations")));

        verify(companyService).getCompanyById(1L);
    }

    @Test
    @DisplayName("GET /api/companies/{id} should return 404 Not Found when company does not exist")
    void getCompanyById_NotFound() throws Exception {
        when(companyService.getCompanyById(99L)).thenThrow(new CompanyNotFoundException("Company not found with ID: 99"));

        mockMvc.perform(get("/api/companies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Company not found with ID: 99")));
    }

    @Test
    @DisplayName("PUT /api/companies/{id} should return 200 OK when company is updated")
    void updateCompany_Success() throws Exception {
        mockToken(COMPANY_TOKEN, "company@example.com", "ROLE_COMPANY");

        CompanyResponse response = new CompanyResponse(
                1L, "Acme Updated", "https://acme.example.com",
                "Alice Walker", "hr@acme.example.com", "9876543210",
                "Hyderabad, India", CompanyStatus.ACTIVE
        );
        when(companyService.updateCompany(eq(1L), any(CompanyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/companies/1")
                        .header("Authorization", "Bearer " + COMPANY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COMPANY_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Updated")));

        verify(companyService).updateCompany(eq(1L), any(CompanyRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/companies/{id} should return 204 No Content when admin deactivates company")
    void deactivateCompany_Success() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        doNothing().when(companyService).deactivateCompany(1L);

        mockMvc.perform(delete("/api/companies/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        verify(companyService).deactivateCompany(1L);
    }

    @Test
    @DisplayName("DELETE /api/companies/{id} should return 404 Not Found when company does not exist")
    void deactivateCompany_NotFound() throws Exception {
        mockToken(ADMIN_TOKEN, "admin@example.com", "ROLE_ADMIN");
        org.mockito.Mockito.doThrow(new CompanyNotFoundException("Company not found with ID: 99")).when(companyService).deactivateCompany(99L);

        mockMvc.perform(delete("/api/companies/99")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Company not found with ID: 99")));
    }
}

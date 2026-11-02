package com.placementmanagementsystem.config;

import com.placementmanagementsystem.controller.AuthController;
import com.placementmanagementsystem.controller.CompanyController;
import com.placementmanagementsystem.controller.JobController;
import com.placementmanagementsystem.controller.StudentController;
import com.placementmanagementsystem.dto.CompanyRequest;
import com.placementmanagementsystem.dto.JobRequest;
import com.placementmanagementsystem.dto.LoginRequest;
import com.placementmanagementsystem.dto.RegisterRequest;
import com.placementmanagementsystem.enums.CompanyStatus;
import com.placementmanagementsystem.enums.JobStatus;
import com.placementmanagementsystem.service.AuthService;
import com.placementmanagementsystem.service.CompanyService;
import com.placementmanagementsystem.service.JobService;
import com.placementmanagementsystem.service.StudentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, JobController.class, CompanyController.class, StudentController.class})
@Import({OpenApiConfig.class, SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SpringDocConfigProperties.class})
@DisplayName("OpenAPI Documentation and Security Requirements Integration Tests")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private StudentService studentService;

    @Test
    @DisplayName("Generated OpenAPI document should configure root bearerAuth and override public endpoints with empty security")
    void testOpenApiDocumentSecurityConfiguration() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Verify root global security scheme and requirement
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())

                // 1. Public Auth operations must override security with empty list []
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").isArray())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security", empty()))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isArray())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security", empty()))

                // 2. Public GET Job operations must override security with empty list []
                .andExpect(jsonPath("$.paths['/api/jobs'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/jobs'].get.security", empty()))
                .andExpect(jsonPath("$.paths['/api/jobs/{id}'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/jobs/{id}'].get.security", empty()))

                // 3. Public GET Company operations must override security with empty list []
                .andExpect(jsonPath("$.paths['/api/companies'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/companies'].get.security", empty()))
                .andExpect(jsonPath("$.paths['/api/companies/{id}'].get.security").isArray())
                .andExpect(jsonPath("$.paths['/api/companies/{id}'].get.security", empty()))

                // 4. Protected operations must NOT have an empty security override (inherits global bearerAuth)
                .andExpect(jsonPath("$.paths['/api/jobs'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/jobs/{id}'].put.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/jobs/{id}'].delete.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/companies'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/companies/{id}'].put.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/companies/{id}'].delete.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/students'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/students'].get.security").doesNotExist());
    }

    @Test
    @DisplayName("Public controller methods should explicitly declare empty @SecurityRequirements annotation")
    void testPublicControllerMethodAnnotations() throws NoSuchMethodException {
        // Auth public operations
        Method registerMethod = AuthController.class.getMethod("register", RegisterRequest.class);
        SecurityRequirements registerSec = registerMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(registerSec, "AuthController.register must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], registerSec.value());

        Method loginMethod = AuthController.class.getMethod("login", LoginRequest.class);
        SecurityRequirements loginSec = loginMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(loginSec, "AuthController.login must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], loginSec.value());

        // Jobs public operations
        Method getAllJobsMethod = JobController.class.getMethod("getAllJobs", String.class, Long.class, JobStatus.class, BigDecimal.class, Pageable.class);
        SecurityRequirements jobsSec = getAllJobsMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(jobsSec, "JobController.getAllJobs must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], jobsSec.value());

        Method getJobByIdMethod = JobController.class.getMethod("getJobById", Long.class);
        SecurityRequirements jobByIdSec = getJobByIdMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(jobByIdSec, "JobController.getJobById must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], jobByIdSec.value());

        // Companies public operations
        Method getAllCompaniesMethod = CompanyController.class.getMethod("getAllCompanies", String.class, CompanyStatus.class, Pageable.class);
        SecurityRequirements companiesSec = getAllCompaniesMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(companiesSec, "CompanyController.getAllCompanies must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], companiesSec.value());

        Method getCompanyByIdMethod = CompanyController.class.getMethod("getCompanyById", Long.class);
        SecurityRequirements companyByIdSec = getCompanyByIdMethod.getAnnotation(SecurityRequirements.class);
        assertNotNull(companyByIdSec, "CompanyController.getCompanyById must be annotated with @SecurityRequirements");
        assertArrayEquals(new io.swagger.v3.oas.annotations.security.SecurityRequirement[0], companyByIdSec.value());
    }

    @Test
    @DisplayName("Protected controller methods should NOT declare empty @SecurityRequirements annotation")
    void testProtectedControllerMethodAnnotations() throws NoSuchMethodException {
        Method createJobMethod = JobController.class.getMethod("createJob", JobRequest.class);
        assertNull(createJobMethod.getAnnotation(SecurityRequirements.class),
                "JobController.createJob must not declare @SecurityRequirements");

        Method createCompanyMethod = CompanyController.class.getMethod("createCompany", CompanyRequest.class);
        assertNull(createCompanyMethod.getAnnotation(SecurityRequirements.class),
                "CompanyController.createCompany must not declare @SecurityRequirements");
    }
}

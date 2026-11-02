package com.placementmanagementsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger configuration for Placement Management System API.
 * Configures API metadata and JWT Bearer authentication scheme.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI placementManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Placement Management System API")
                        .version("1.0.0")
                        .description("RESTful API documentation for the Placement Management System. " +
                                "Supports authentication, student profiles, company management, job postings, " +
                                "applications, interviews, placements, dashboards, and role-based access control."))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token obtained from /api/auth/login (without 'Bearer ' prefix)")));
    }
}

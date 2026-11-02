package com.placementmanagementsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OpenAPI Configuration Unit Tests")
class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    @DisplayName("OpenAPI bean should configure correct API title, version, and description")
    void testOpenApiMetadata() {
        OpenAPI openAPI = openApiConfig.placementManagementOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Placement Management System API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("Placement Management System"));
    }

    @Test
    @DisplayName("OpenAPI bean should configure JWT Bearer security scheme")
    void testOpenApiSecurityScheme() {
        OpenAPI openAPI = openApiConfig.placementManagementOpenAPI();

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);
        assertNotNull(scheme, "bearerAuth security scheme must be present in OpenAPI components");
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    @DisplayName("OpenAPI bean should configure global security requirement with bearerAuth")
    void testOpenApiSecurityRequirement() {
        OpenAPI openAPI = openApiConfig.placementManagementOpenAPI();

        assertNotNull(openAPI.getSecurity());
        boolean hasBearerAuth = openAPI.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey(OpenApiConfig.SECURITY_SCHEME_NAME));
        assertTrue(hasBearerAuth, "OpenAPI security requirements must include bearerAuth");
    }
}

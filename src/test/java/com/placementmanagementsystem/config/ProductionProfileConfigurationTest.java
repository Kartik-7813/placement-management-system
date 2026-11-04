package com.placementmanagementsystem.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Production Profile Configuration Tests (Phase 7 - Step 5C)")
class ProductionProfileConfigurationTest {

    private Properties defaultProperties;
    private Properties prodProperties;

    @BeforeEach
    void setUp() throws IOException {
        defaultProperties = new Properties();
        prodProperties = new Properties();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(is, "application.properties must be present on the classpath");
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] documents = content.split("(?m)^#---.*$");
            assertTrue(documents.length >= 2, "application.properties must contain base and production documents separated by #---");

            defaultProperties.load(new StringReader(documents[0]));
            prodProperties.load(new StringReader(documents[1]));
        }
    }

    @Test
    @DisplayName("Default profile is not prod and activates prod only when spring.config.activate.on-profile=prod")
    void defaultProfile_IsNotProd() {
        assertNull(defaultProperties.getProperty("spring.config.activate.on-profile"),
                "Base document must not have profile activation to serve as the default");
        assertEquals("prod", prodProperties.getProperty("spring.config.activate.on-profile"),
                "Production document must be activated strictly on profile 'prod'");
    }

    @Test
    @DisplayName("Production DDL mode resolves strictly to validate (never modifies database)")
    void production_DdlAutoIsValidate() {
        String prodDdlAuto = prodProperties.getProperty("spring.jpa.hibernate.ddl-auto");
        assertNotNull(prodDdlAuto, "spring.jpa.hibernate.ddl-auto must be defined in prod profile");

        MockEnvironment env = new MockEnvironment();
        assertEquals("validate", env.resolvePlaceholders(prodDdlAuto),
                "Production DDL must be 'validate' to prevent automatic schema modifications");
    }

    @Test
    @DisplayName("Production SQL statement logging is strictly disabled")
    void production_ShowSqlIsDisabled() {
        String prodShowSql = prodProperties.getProperty("spring.jpa.show-sql");
        assertNotNull(prodShowSql, "spring.jpa.show-sql must be defined in prod profile");

        MockEnvironment env = new MockEnvironment();
        assertEquals("false", env.resolvePlaceholders(prodShowSql),
                "Production SQL logging must be disabled to avoid console I/O overhead");
    }

    @Test
    @DisplayName("Production Open Session In View (OSIV) is strictly disabled")
    void production_OpenInViewIsDisabled() {
        String prodOpenInView = prodProperties.getProperty("spring.jpa.open-in-view");
        assertNotNull(prodOpenInView, "spring.jpa.open-in-view must be defined in prod profile");

        MockEnvironment env = new MockEnvironment();
        assertEquals("false", env.resolvePlaceholders(prodOpenInView),
                "Production OSIV must be false for a stateless REST API");
    }

    @Test
    @DisplayName("Production Swagger UI and OpenAPI JSON documentation are disabled by default")
    void production_SwaggerAndOpenApiDisabledByDefault() {
        String swaggerUi = prodProperties.getProperty("springdoc.swagger-ui.enabled");
        String apiDocs = prodProperties.getProperty("springdoc.api-docs.enabled");

        assertNotNull(swaggerUi, "springdoc.swagger-ui.enabled must be defined in prod profile");
        assertNotNull(apiDocs, "springdoc.api-docs.enabled must be defined in prod profile");

        MockEnvironment env = new MockEnvironment();
        assertEquals("false", env.resolvePlaceholders(swaggerUi),
                "Swagger UI must be disabled by default in production");
        assertEquals("false", env.resolvePlaceholders(apiDocs),
                "OpenAPI JSON docs must be disabled by default in production");
    }

    @Test
    @DisplayName("Production Swagger UI and OpenAPI docs can be deliberately enabled via environment variables")
    void production_SwaggerCanBeEnabledViaEnvironmentVariables() {
        String swaggerUi = prodProperties.getProperty("springdoc.swagger-ui.enabled");
        String apiDocs = prodProperties.getProperty("springdoc.api-docs.enabled");

        MockEnvironment env = new MockEnvironment();
        env.setProperty("SWAGGER_ENABLED", "true");
        env.setProperty("API_DOCS_ENABLED", "true");

        assertEquals("true", env.resolvePlaceholders(swaggerUi),
                "SWAGGER_ENABLED=true must enable Swagger UI in production");
        assertEquals("true", env.resolvePlaceholders(apiDocs),
                "API_DOCS_ENABLED=true must enable OpenAPI docs in production");
    }

    @Test
    @DisplayName("Production CORS origins are mandatory and do not silently fall back to localhost")
    void production_CorsOriginsAreMandatoryWithoutLocalhostFallback() {
        String prodCorsOrigins = prodProperties.getProperty("application.cors.allowed-origins");
        assertNotNull(prodCorsOrigins, "application.cors.allowed-origins must be declared in prod profile");

        // Without CORS_ALLOWED_ORIGINS, placeholder resolution must fail or retain unresolvable token
        MockEnvironment unconfiguredEnv = new MockEnvironment();
        assertThrows(IllegalArgumentException.class, () -> unconfiguredEnv.resolveRequiredPlaceholders(prodCorsOrigins),
                "Production CORS must fail fast when CORS_ALLOWED_ORIGINS is not set");

        // When configured, resolves to the production origins
        MockEnvironment configuredEnv = new MockEnvironment();
        configuredEnv.setProperty("CORS_ALLOWED_ORIGINS", "https://placement.example.com,https://admin.placement.example.com");
        assertEquals("https://placement.example.com,https://admin.placement.example.com",
                configuredEnv.resolvePlaceholders(prodCorsOrigins));
    }

    @Test
    @DisplayName("Production database password is mandatory without empty fallback")
    void production_DatasourcePasswordIsMandatoryWithoutFallback() {
        String prodPassword = prodProperties.getProperty("spring.datasource.password");
        assertNotNull(prodPassword, "spring.datasource.password must be defined in prod profile");

        MockEnvironment unconfiguredEnv = new MockEnvironment();
        assertThrows(IllegalArgumentException.class, () -> unconfiguredEnv.resolveRequiredPlaceholders(prodPassword),
                "Production database password must fail fast when SPRING_DATASOURCE_PASSWORD is not set");

        MockEnvironment configuredEnv = new MockEnvironment();
        configuredEnv.setProperty("SPRING_DATASOURCE_PASSWORD", "SuperSecureProdPassword#2026");
        assertEquals("SuperSecureProdPassword#2026", configuredEnv.resolvePlaceholders(prodPassword));
    }

    @Test
    @DisplayName("Production JWT secret is mandatory without empty fallback")
    void production_JwtSecretIsMandatoryWithoutFallback() {
        String prodJwtSecret = prodProperties.getProperty("application.security.jwt.secret");
        assertNotNull(prodJwtSecret, "application.security.jwt.secret must be defined in prod profile");

        MockEnvironment unconfiguredEnv = new MockEnvironment();
        assertThrows(IllegalArgumentException.class, () -> unconfiguredEnv.resolveRequiredPlaceholders(prodJwtSecret),
                "Production JWT secret must fail fast when JWT_SECRET is not set");

        MockEnvironment configuredEnv = new MockEnvironment();
        configuredEnv.setProperty("JWT_SECRET", "dGhpc0lzQVZhbGlkQmFzZTY0RW5jb2RlZFNlY3JldEtleUZvckpXVFNpZ25pbmc=");
        assertEquals("dGhpc0lzQVZhbGlkQmFzZTY0RW5jb2RlZFNlY3JldEtleUZvckpXVFNpZ25pbmc=",
                configuredEnv.resolvePlaceholders(prodJwtSecret));
    }

    @Test
    @DisplayName("HikariCP connection pool settings and Actuator restrictions remain intact from base document")
    void production_InheritsBaseHikariAndActuatorSettings() {
        // Base document properties apply to production unless overridden
        assertEquals("health", defaultProperties.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", defaultProperties.getProperty("management.endpoint.health.show-details"));
        assertEquals("true", defaultProperties.getProperty("management.endpoint.health.probes.enabled"));

        assertNotNull(defaultProperties.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertNotNull(defaultProperties.getProperty("spring.datasource.hikari.minimum-idle"));
        assertNotNull(defaultProperties.getProperty("spring.datasource.hikari.connection-timeout"));
        assertNotNull(defaultProperties.getProperty("spring.datasource.hikari.idle-timeout"));
        assertNotNull(defaultProperties.getProperty("spring.datasource.hikari.max-lifetime"));
    }
}


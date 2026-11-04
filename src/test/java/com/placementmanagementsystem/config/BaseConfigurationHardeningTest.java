package com.placementmanagementsystem.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Base Configuration Hardening Tests (Phase 7 - Step 5B)")
class BaseConfigurationHardeningTest {

    private Properties applicationProperties;

    @BeforeEach
    void setUp() throws IOException {
        applicationProperties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(is, "application.properties must be present on the classpath");
            String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] documents = content.split("(?m)^#---.*$");
            applicationProperties.load(new java.io.StringReader(documents[0]));
        }
    }

    @Test
    @DisplayName("spring.jpa.show-sql defaults to false")
    void showSql_DefaultsToFalse() {
        String rawProperty = applicationProperties.getProperty("spring.jpa.show-sql");
        assertNotNull(rawProperty, "spring.jpa.show-sql must be declared");

        MockEnvironment env = new MockEnvironment();
        String resolvedValue = env.resolvePlaceholders(rawProperty);

        assertEquals("false", resolvedValue, "SQL statement logging must be disabled by default");
    }

    @Test
    @DisplayName("spring.jpa.show-sql can be explicitly enabled via SPRING_JPA_SHOW_SQL")
    void showSql_CanBeEnabledViaEnvironmentVariable() {
        String rawProperty = applicationProperties.getProperty("spring.jpa.show-sql");

        MockEnvironment env = new MockEnvironment();
        env.setProperty("SPRING_JPA_SHOW_SQL", "true");
        String resolvedValue = env.resolvePlaceholders(rawProperty);

        assertEquals("true", resolvedValue, "SPRING_JPA_SHOW_SQL must override show-sql setting");
    }

    @Test
    @DisplayName("spring.jpa.open-in-view is explicitly disabled (false)")
    void openInView_IsExplicitlyFalse() {
        String rawProperty = applicationProperties.getProperty("spring.jpa.open-in-view");
        assertNotNull(rawProperty, "spring.jpa.open-in-view must be declared");

        MockEnvironment env = new MockEnvironment();
        String resolvedValue = env.resolvePlaceholders(rawProperty);

        assertEquals("false", resolvedValue, "Open Session In View must be disabled for stateless REST APIs");
    }

    @Test
    @DisplayName("HikariCP connection pool properties have conservative, sensible defaults")
    void hikariCp_DefaultSettingsAreConfiguredAndSensible() {
        MockEnvironment env = new MockEnvironment();

        int maxPoolSize = Integer.parseInt(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.maximum-pool-size"))
        );
        int minIdle = Integer.parseInt(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.minimum-idle"))
        );
        long connectionTimeout = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.connection-timeout"))
        );
        long idleTimeout = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.idle-timeout"))
        );
        long maxLifetime = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.max-lifetime"))
        );

        assertEquals(10, maxPoolSize, "Default maximum-pool-size must be 10");
        assertEquals(5, minIdle, "Default minimum-idle must be 5");
        assertEquals(30000L, connectionTimeout, "Default connection-timeout must be 30000 ms (30s)");
        assertEquals(600000L, idleTimeout, "Default idle-timeout must be 600000 ms (10 min)");
        assertEquals(1800000L, maxLifetime, "Default max-lifetime must be 1800000 ms (30 min)");

        assertTrue(minIdle <= maxPoolSize, "minimum-idle must not exceed maximum-pool-size");
    }

    @Test
    @DisplayName("HikariCP connection pool properties can be overridden via environment variables")
    void hikariCp_CanBeOverriddenViaEnvironmentVariables() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DB_POOL_MAX_SIZE", "20");
        env.setProperty("DB_POOL_MIN_IDLE", "10");
        env.setProperty("DB_CONNECTION_TIMEOUT_MS", "45000");
        env.setProperty("DB_IDLE_TIMEOUT_MS", "300000");
        env.setProperty("DB_MAX_LIFETIME_MS", "900000");

        int maxPoolSize = Integer.parseInt(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.maximum-pool-size"))
        );
        int minIdle = Integer.parseInt(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.minimum-idle"))
        );
        long connectionTimeout = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.connection-timeout"))
        );
        long idleTimeout = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.idle-timeout"))
        );
        long maxLifetime = Long.parseLong(
                env.resolvePlaceholders(applicationProperties.getProperty("spring.datasource.hikari.max-lifetime"))
        );

        assertEquals(20, maxPoolSize);
        assertEquals(10, minIdle);
        assertEquals(45000L, connectionTimeout);
        assertEquals(300000L, idleTimeout);
        assertEquals(900000L, maxLifetime);
    }
}


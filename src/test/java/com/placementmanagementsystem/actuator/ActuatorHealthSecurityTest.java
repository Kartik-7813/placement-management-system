package com.placementmanagementsystem.actuator;

import com.placementmanagementsystem.config.SecurityConfig;
import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolated Spring Boot Actuator health and security tests.
 * Verifies endpoint access rules, liveness/readiness probes, information leakage protection,
 * and endpoint exposure restrictions without requiring an external MySQL database.
 */
@SpringBootTest(classes = ActuatorHealthSecurityTest.TestActuatorApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=never",
        "management.endpoint.health.probes.enabled=true"
})
@DisplayName("Spring Boot Actuator Health & Security Tests")
class ActuatorHealthSecurityTest {

    @SpringBootApplication(scanBasePackages = "com.placementmanagementsystem.actuator")
    @Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
    static class TestActuatorApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /actuator/health should be accessible anonymously and return status UP")
    void healthEndpoint_ShouldBeAccessibleWithoutAuth_AndReturnStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/liveness should be accessible anonymously and return status UP")
    void livenessProbe_ShouldBeAccessibleWithoutAuth_AndReturnStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness should be accessible anonymously and return status UP")
    void readinessProbe_ShouldBeAccessibleWithoutAuth_AndReturnStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health should never expose components or internal details")
    void healthEndpoint_ShouldNotExposeComponentsOrInternalDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("GET /actuator/env should not be exposed over HTTP and return 404 Not Found")
    void unexposedActuatorEndpoint_Env_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/actuator/env").with(user("test-user").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/students should require authentication and return 401 Unauthorized")
    void protectedBusinessEndpoint_ShouldRequireAuthentication_AndReturn401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}

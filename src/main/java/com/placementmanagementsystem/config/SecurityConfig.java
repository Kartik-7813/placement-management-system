package com.placementmanagementsystem.config;

import com.placementmanagementsystem.security.CustomUserDetailsService;
import com.placementmanagementsystem.security.JwtAccessDeniedHandler;
import com.placementmanagementsystem.security.JwtAuthenticationEntryPoint;
import com.placementmanagementsystem.security.JwtAuthenticationFilter;
import com.placementmanagementsystem.security.JwtService;
import com.placementmanagementsystem.security.ratelimit.RateLimitingFilter;
import com.placementmanagementsystem.security.ratelimit.RateLimitingService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security configuration defining the filter chain, access rules,
 * session management, and exception handling for the Placement Management System.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${application.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Retry-After"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            ObjectProvider<RateLimitingService> rateLimitingServiceProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                // Enable Cross-Origin Resource Sharing (CORS) with our configured CorsConfigurationSource
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF: Not needed because our REST APIs are stateless and rely on Bearer tokens
                // stored in HTTP Authorization headers, which browsers never send automatically with cross-site requests.
                .csrf(csrf -> csrf.disable())

                // Stateless session management: Never create or use HTTP Sessions for security context.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Custom 401 Unauthorized and 403 Forbidden handlers in consistent JSON format.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Define URL-level authorization rules.
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints for registration and login
                        .requestMatchers("/api/auth/**").permitAll()

                        // OpenAPI / Swagger UI documentation endpoints
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Actuator health check and probe endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        ).permitAll()

                        // Error dispatch
                        .requestMatchers("/error").permitAll()

                        // Public read-only endpoints for job listings and company profiles
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies", "/api/companies/**").permitAll()

                        // All other business endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Register JWT authentication filter before standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        RateLimitingService rateLimitingService = rateLimitingServiceProvider.getIfAvailable();
        if (rateLimitingService != null) {
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
            RateLimitingFilter rateLimitingFilter = new RateLimitingFilter(rateLimitingService, objectMapper);
            http.addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }
}

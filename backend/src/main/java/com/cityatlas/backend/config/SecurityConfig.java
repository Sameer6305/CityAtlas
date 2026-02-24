package com.cityatlas.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security Configuration for CityAtlas Backend
 * 
 * ⚠️ DEVELOPMENT CONFIGURATION ONLY ⚠️
 * Current setup: All endpoints are publicly accessible without authentication
 * 
 * TODO: Before production deployment:
 * 1. Enable JWT-based authentication
 * 2. Add role-based authorization (@PreAuthorize)
 * 3. Secure sensitive endpoints
 * 4. Enable CSRF protection for stateful endpoints (if needed)
 * 5. Restrict CORS to production frontend URL
 * 
 * JWT Implementation Plan:
 * - Add JWT token generation/validation utilities
 * - Implement JwtAuthenticationFilter
 * - Configure authentication entry point
 * - Add UserDetailsService implementation
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cityatlas.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String corsAllowedOrigins;

    /**
     * Security Filter Chain Configuration
     * 
     * Development Mode:
     * - All endpoints: permitAll()
     * - CSRF: Disabled (standard for REST APIs)
     * - CORS: Enabled for localhost frontend
     * - Sessions: Stateless (preparation for JWT)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ============================================
            // CORS Configuration
            // ============================================
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ============================================
            // CSRF Protection - DISABLED for REST API
            // ============================================
            .csrf(csrf -> csrf.disable())
            
            // ============================================
            // Authorization Rules - DEVELOPMENT ONLY
            // ============================================
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no auth required)
                // Note: context-path (/api) is stripped by the servlet container,
                // so matchers here use servlet-relative paths (no /api prefix).
                .requestMatchers(
                    "/public/**",
                    "/health/**",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // ⚠️ DEVELOPMENT: Allow all other requests without authentication
                // TODO: Replace with authenticated() and add JWT filter
                .anyRequest().permitAll()
                
                // TODO: Production configuration example:
                // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // .requestMatchers("/api/cities/**").authenticated()
                // .anyRequest().authenticated()
            )
            
            // ============================================
            // Session Management - Stateless for JWT
            // ============================================
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // ============================================
            // JWT Filter - TO BE ADDED
            // ============================================
            // TODO: Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // ============================================
            // Exception Handling - TO BE CONFIGURED
            // ============================================
            // TODO: Add custom authentication entry point
            // .exceptionHandling(ex -> ex
            //     .authenticationEntryPoint(jwtAuthenticationEntryPoint())
            // )
        ;
        
        return http.build();
    }

    /**
     * CORS Configuration
     *
     * Origins are driven by the `cityatlas.cors.allowed-origins` property (comma-separated).
     * Override via env var: CITYATLAS_CORS_ALLOWED_ORIGINS=https://yourdomain.com
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins from config / env var
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        
        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Apply configuration to all endpoints
        // Note: context-path is /api, so servlet-relative paths start at /
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    // ============================================
    // JWT Components - TO BE IMPLEMENTED
    // ============================================
    
    /*
     * TODO: Add these beans when implementing JWT authentication
     * 
     * @Bean
     * public JwtAuthenticationFilter jwtAuthenticationFilter() {
     *     return new JwtAuthenticationFilter();
     * }
     * 
     * @Bean
     * public AuthenticationManager authenticationManager(
     *         AuthenticationConfiguration authConfig) throws Exception {
     *     return authConfig.getAuthenticationManager();
     * }
     * 
     * @Bean
     * public PasswordEncoder passwordEncoder() {
     *     return new BCryptPasswordEncoder();
     * }
     * 
     * @Bean
     * public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
     *     return new JwtAuthenticationEntryPoint();
     * }
     */
}

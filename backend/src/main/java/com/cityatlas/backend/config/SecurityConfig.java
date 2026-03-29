package com.cityatlas.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cityatlas.backend.security.JwtAuthFilter;

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

    // AUTH: JWT-based, demo credentials in AuthController
    // FIXED: Route protection now requires JWT for all non-auth, non-health API paths.

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Value("${cityatlas.cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Security Filter Chain Configuration
     * 
     * Development Mode:
     * - All endpoints: permitAll()
     * - CSRF: Disabled (standard for REST APIs)
    * - CORS: Enabled for configured frontend origins
     * - Sessions: Stateless (preparation for JWT)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ============================================
            // CORS Configuration
            // ============================================
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // ============================================
            // CSRF Protection - DISABLED for REST API
            // ============================================
            .csrf(csrf -> csrf.disable())
            
            // ============================================
            // Authorization Rules
            // ============================================
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/health/**",
                    "/actuator/health/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/cities/**",
                    "/weather/**",
                    "/air-quality/**"
                ).permitAll()

                .anyRequest().authenticated()
            )
            
            // ============================================
            // Session Management - Stateless for JWT
            // ============================================
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                // FIXED: Return 401 (not 403) when token is missing/invalid on protected routes.
                .authenticationEntryPoint((request, response, authException) -> response.sendError(401))
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
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
    
}

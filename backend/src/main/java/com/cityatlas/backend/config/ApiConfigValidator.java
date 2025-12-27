package com.cityatlas.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * API Configuration Validator
 * 
 * Validates external API configuration on application startup and logs warnings
 * if placeholder values are detected.
 * 
 * This helps prevent accidental deployment to production with invalid API keys.
 * 
 * Security Best Practices:
 * - Placeholder detection alerts developers during development
 * - Prevents silent failures in production
 * - Encourages proper environment variable setup
 * - Logs configuration status without exposing secrets
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiConfigValidator {

    private final ExternalApiConfig apiConfig;

    /**
     * Validate API configuration after application startup
     * 
     * Checks for placeholder values and logs appropriate warnings.
     * This runs AFTER all Spring beans are initialized and properties are bound.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateApiConfiguration() {
        log.info("=================================================================");
        log.info("EXTERNAL API CONFIGURATION VALIDATION");
        log.info("=================================================================");
        
        boolean hasPlaceholders = false;
        
        // Validate OpenWeatherMap
        if (apiConfig.getOpenweather().isPlaceholder()) {
            log.warn("⚠️  OpenWeatherMap API: Using PLACEHOLDER key (set OPENWEATHER_API_KEY env variable)");
            hasPlaceholders = true;
        } else {
            log.info("✅ OpenWeatherMap API: Configured (key length: {} chars)", 
                apiConfig.getOpenweather().getApiKey().length());
        }
        
        // Validate OpenAQ
        if (apiConfig.getOpenaq().isPlaceholder()) {
            log.warn("⚠️  OpenAQ API: Using PLACEHOLDER key (set OPENAQ_API_KEY env variable)");
            hasPlaceholders = true;
        } else {
            log.info("✅ OpenAQ API: Configured (key length: {} chars)", 
                apiConfig.getOpenaq().getApiKey().length());
        }
        
        // Validate Spotify
        if (apiConfig.getSpotify().isPlaceholder()) {
            log.warn("⚠️  Spotify API: Using PLACEHOLDER credentials (set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET env variables)");
            hasPlaceholders = true;
        } else {
            log.info("✅ Spotify API: Configured (client ID length: {} chars)", 
                apiConfig.getSpotify().getClientId().length());
        }
        
        // Validate Unsplash
        if (apiConfig.getUnsplash().isPlaceholder()) {
            log.warn("⚠️  Unsplash API: Using PLACEHOLDER key (set UNSPLASH_ACCESS_KEY env variable)");
            hasPlaceholders = true;
        } else {
            log.info("✅ Unsplash API: Configured (key length: {} chars)", 
                apiConfig.getUnsplash().getAccessKey().length());
        }
        
        // Log retry and timeout configuration
        log.info("📊 Retry Config: max-attempts={}, backoff={}ms", 
            apiConfig.getRetry().getMaxAttempts(),
            apiConfig.getRetry().getBackoffMs());
        log.info("⏱️  Timeout Config: connection={}ms, read={}ms",
            apiConfig.getTimeout().getConnectionMs(),
            apiConfig.getTimeout().getReadMs());
        
        // Summary
        if (hasPlaceholders) {
            log.warn("=================================================================");
            log.warn("⚠️  WARNING: Placeholder API keys detected!");
            log.warn("External API integrations will NOT work until real keys are provided.");
            log.warn("Set environment variables before deploying to production.");
            log.warn("=================================================================");
        } else {
            log.info("=================================================================");
            log.info("✅ All external API keys configured successfully");
            log.info("=================================================================");
        }
    }
}

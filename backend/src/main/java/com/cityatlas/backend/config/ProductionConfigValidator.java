package com.cityatlas.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigValidator {

    private static final List<String> REQUIRED_ENV_VARS = Arrays.asList(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET",
            "CITYATLAS_CORS_ALLOWED_ORIGINS",
            "OPENWEATHER_API_KEY",
            "OPENAQ_API_KEY",
            "SPOTIFY_CLIENT_ID",
            "SPOTIFY_CLIENT_SECRET",
            "SPOTIFY_REDIRECT_URI",
            "UNSPLASH_ACCESS_KEY"
    );

    private final ExternalApiConfig apiConfig;

    @PostConstruct
    void validateOnStartup() {
        for (String key : REQUIRED_ENV_VARS) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required environment variable: " + key);
            }
            String normalized = value.trim().toUpperCase();
            if (normalized.contains("YOUR_") || normalized.contains("PLACEHOLDER") || normalized.startsWith("CHANGE_ME")) {
                throw new IllegalStateException("Environment variable uses placeholder value: " + key);
            }
        }

        if (apiConfig.getOpenweather().isPlaceholder()
                || apiConfig.getOpenaq().isPlaceholder()
                || apiConfig.getSpotify().isPlaceholder()
                || apiConfig.getUnsplash().isPlaceholder()) {
            throw new IllegalStateException("External API configuration is invalid for production startup");
        }
    }
}

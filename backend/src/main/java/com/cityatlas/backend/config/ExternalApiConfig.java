package com.cityatlas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * External API Configuration Properties
 * 
 * Reads API keys and configuration from application.properties using type-safe binding.
 * All API keys must be provided via environment variables in production.
 * 
 * SECURITY WARNING:
 * - NEVER commit real API keys to version control
 * - Always use environment variables for secrets
 * - The placeholder values in application.properties are for development/testing only
 * 
 * Usage Example:
 * <pre>
 * {@code
 * @Autowired
 * private ExternalApiConfig apiConfig;
 * 
 * String weatherApiKey = apiConfig.getOpenweather().getApiKey();
 * String baseUrl = apiConfig.getOpenweather().getBaseUrl();
 * }
 * </pre>
 * 
 * Environment Variable Setup:
 * <pre>
 * export OPENWEATHER_API_KEY=your_actual_key_here
 * export OPENAQ_API_KEY=your_actual_key_here
 * export SPOTIFY_CLIENT_ID=your_client_id_here
 * export SPOTIFY_CLIENT_SECRET=your_client_secret_here
 * export UNSPLASH_ACCESS_KEY=your_access_key_here
 * </pre>
 * 
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Configuration
@ConfigurationProperties(prefix = "cityatlas.external")
@Validated
@Getter
@Setter
public class ExternalApiConfig {

    /**
     * OpenWeatherMap API configuration
     */
    private OpenWeatherConfig openweather = new OpenWeatherConfig();
    
    /**
     * OpenAQ API configuration
     */
    private OpenAqConfig openaq = new OpenAqConfig();
    
    /**
     * Spotify API configuration
     */
    private SpotifyConfig spotify = new SpotifyConfig();
    
    /**
     * Unsplash API configuration
     */
    private UnsplashConfig unsplash = new UnsplashConfig();
    
    /**
     * Retry configuration for external API calls
     */
    private RetryConfig retry = new RetryConfig();
    
    /**
     * Timeout configuration for external API calls
     */
    private TimeoutConfig timeout = new TimeoutConfig();

    /**
     * OpenWeatherMap API Configuration
     * 
     * Used for weather data, forecasts, and air quality information.
     * Sign up at: https://openweathermap.org/api
     */
    @Getter
    @Setter
    public static class OpenWeatherConfig {
        /**
         * OpenWeatherMap API key
         * Must be set via OPENWEATHER_API_KEY environment variable
         */
        @NotBlank(message = "OpenWeatherMap API key is required")
        private String apiKey;
        
        /**
         * OpenWeatherMap API base URL
         */
        @NotBlank(message = "OpenWeatherMap base URL is required")
        private String baseUrl;
        
        /**
         * Check if this is a placeholder (not a real API key)
         * @return true if using placeholder value
         */
        public boolean isPlaceholder() {
            return apiKey == null || apiKey.contains("placeholder");
        }
    }

    /**
     * OpenAQ API Configuration
     * 
     * Used for real-time air quality monitoring and historical AQI data.
     * Sign up at: https://openaq.org/
     */
    @Getter
    @Setter
    public static class OpenAqConfig {
        /**
         * OpenAQ API key
         * Must be set via OPENAQ_API_KEY environment variable
         */
        @NotBlank(message = "OpenAQ API key is required")
        private String apiKey;
        
        /**
         * OpenAQ API base URL
         */
        @NotBlank(message = "OpenAQ base URL is required")
        private String baseUrl;
        
        /**
         * Check if this is a placeholder (not a real API key)
         * @return true if using placeholder value
         */
        public boolean isPlaceholder() {
            return apiKey == null || apiKey.contains("placeholder");
        }
    }

    /**
     * Spotify API Configuration
     * 
     * Used for music scene data, cultural events, and local playlists.
     * Sign up at: https://developer.spotify.com/dashboard
     */
    @Getter
    @Setter
    public static class SpotifyConfig {
        /**
         * Spotify Client ID
         * Must be set via SPOTIFY_CLIENT_ID environment variable
         */
        @NotBlank(message = "Spotify Client ID is required")
        private String clientId;
        
        /**
         * Spotify Client Secret
         * Must be set via SPOTIFY_CLIENT_SECRET environment variable
         */
        @NotBlank(message = "Spotify Client Secret is required")
        private String clientSecret;
        
        /**
         * Spotify API base URL
         */
        @NotBlank(message = "Spotify base URL is required")
        private String baseUrl;
        
        /**
         * Spotify OAuth token endpoint
         */
        @NotBlank(message = "Spotify auth URL is required")
        private String authUrl;
        
        /**
         * Check if these are placeholder credentials (not real)
         * @return true if using placeholder values
         */
        public boolean isPlaceholder() {
            return (clientId != null && clientId.contains("placeholder")) ||
                   (clientSecret != null && clientSecret.contains("placeholder"));
        }
    }

    /**
     * Unsplash API Configuration
     * 
     * Used for high-quality city imagery and visual content.
     * Sign up at: https://unsplash.com/developers
     */
    @Getter
    @Setter
    public static class UnsplashConfig {
        /**
         * Unsplash Access Key
         * Must be set via UNSPLASH_ACCESS_KEY environment variable
         */
        @NotBlank(message = "Unsplash Access Key is required")
        private String accessKey;
        
        /**
         * Unsplash API base URL
         */
        @NotBlank(message = "Unsplash base URL is required")
        private String baseUrl;
        
        /**
         * Check if this is a placeholder (not a real API key)
         * @return true if using placeholder value
         */
        public boolean isPlaceholder() {
            return accessKey == null || accessKey.contains("placeholder");
        }
    }

    /**
     * Retry Configuration for External API Calls
     * 
     * Defines retry behavior when external API calls fail.
     */
    @Getter
    @Setter
    public static class RetryConfig {
        /**
         * Maximum number of retry attempts
         */
        @Positive(message = "Max attempts must be positive")
        private int maxAttempts = 3;
        
        /**
         * Backoff delay in milliseconds between retries
         */
        @Positive(message = "Backoff delay must be positive")
        private long backoffMs = 1000;
    }

    /**
     * Timeout Configuration for External API Calls
     * 
     * Defines connection and read timeout limits.
     */
    @Getter
    @Setter
    public static class TimeoutConfig {
        /**
         * Connection timeout in milliseconds
         */
        @Positive(message = "Connection timeout must be positive")
        private int connectionMs = 5000;
        
        /**
         * Read timeout in milliseconds
         */
        @Positive(message = "Read timeout must be positive")
        private int readMs = 10000;
    }
}

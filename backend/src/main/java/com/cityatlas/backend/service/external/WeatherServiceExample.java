package com.cityatlas.backend.service.external;

import com.cityatlas.backend.config.ExternalApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Example Weather Service
 * 
 * Demonstrates how to use ExternalApiConfig to integrate with OpenWeatherMap API.
 * This is a template/example - implement actual HTTP client logic as needed.
 * 
 * Best Practices:
 * - Always check for placeholder values before making API calls
 * - Implement proper error handling and retries
 * - Use caching to respect rate limits
 * - Log API usage for monitoring
 * 
 * @see ExternalApiConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceExample {

    private final ExternalApiConfig apiConfig;

    /**
     * Example method showing how to access OpenWeatherMap configuration
     * 
     * @param cityName The city to fetch weather for
     * @return Weather description (example implementation)
     * @throws IllegalStateException if API key is not configured
     */
    public String fetchWeatherExample(String cityName) {
        // 1. Get configuration
        ExternalApiConfig.OpenWeatherConfig weatherConfig = apiConfig.getOpenweather();
        
        // 2. Validate configuration (check for placeholder)
        if (weatherConfig.isPlaceholder()) {
            log.error("Cannot fetch weather: OpenWeatherMap API key not configured");
            throw new IllegalStateException(
                "OpenWeatherMap API key not configured. Set OPENWEATHER_API_KEY environment variable."
            );
        }
        
        // 3. Get API key and base URL
        String apiKey = weatherConfig.getApiKey();
        String baseUrl = weatherConfig.getBaseUrl();
        
        // 4. Build API URL (example)
        String apiUrl = String.format("%s/weather?q=%s&appid=%s&units=metric", 
            baseUrl, cityName, apiKey);
        
        log.info("Fetching weather for city: {} from URL: {}", cityName, 
            baseUrl + "/weather?q=" + cityName); // Don't log API key!
        
        // 5. Make HTTP request (pseudo-code - implement with RestTemplate, WebClient, etc.)
        // RestTemplate restTemplate = new RestTemplate();
        // WeatherResponse response = restTemplate.getForObject(apiUrl, WeatherResponse.class);
        
        // 6. Return result (example)
        return "Weather data for " + cityName + " (example - implement actual API call)";
    }

    /**
     * Example showing retry configuration usage
     * 
     * @param cityName The city name
     * @return Weather data
     */
    public String fetchWeatherWithRetry(String cityName) {
        int maxAttempts = apiConfig.getRetry().getMaxAttempts();
        long backoffMs = apiConfig.getRetry().getBackoffMs();
        
        log.debug("Configured retry: maxAttempts={}, backoffMs={}", maxAttempts, backoffMs);
        
        // Implement retry logic here
        // For Spring Retry: @Retryable(maxAttempts = maxAttempts, backoff = @Backoff(delay = backoffMs))
        
        return fetchWeatherExample(cityName);
    }

    /**
     * Example showing timeout configuration usage
     * 
     * @return Configured timeouts
     */
    public String getTimeoutConfiguration() {
        int connectionTimeout = apiConfig.getTimeout().getConnectionMs();
        int readTimeout = apiConfig.getTimeout().getReadMs();
        
        return String.format("Connection timeout: %dms, Read timeout: %dms", 
            connectionTimeout, readTimeout);
    }

    /**
     * Check if the weather service is properly configured
     * 
     * @return true if API key is configured, false if using placeholder
     */
    public boolean isConfigured() {
        return !apiConfig.getOpenweather().isPlaceholder();
    }
}

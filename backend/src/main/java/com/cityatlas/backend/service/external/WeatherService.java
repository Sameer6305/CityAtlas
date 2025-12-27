package com.cityatlas.backend.service.external;

import java.time.Duration;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cityatlas.backend.config.ExternalApiConfig;
import com.cityatlas.backend.dto.response.WeatherDTO;
import com.cityatlas.backend.exception.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Weather Service - OpenWeatherMap API Integration
 * 
 * Provides weather data integration with OpenWeatherMap API.
 * 
 * Features:
 * - Fetches current weather by city name
 * - Maps OpenWeatherMap response to simplified WeatherDTO
 * - Handles missing API key gracefully (returns empty Mono, logs warning)
 * - Does NOT fail application startup if key is missing
 * - Includes retry logic for transient failures
 * 
 * Configuration:
 * - API key read from environment variable: OPENWEATHER_API_KEY
 * - Base URL configured in application.properties
 * - Timeouts and retry settings from ExternalApiConfig
 * 
 * @see WeatherDTO
 * @see ExternalApiConfig
 * @see WebClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WebClient webClient;
    private final ExternalApiConfig apiConfig;

    /**
     * Fetch current weather for a city by name
     * 
     * If API key is not configured (placeholder value), this method:
     * - Logs a warning (not an error)
     * - Returns Mono.empty() instead of failing
     * - Does NOT throw exceptions that would break the application
     * 
     * This allows the application to start and run even without weather API integration,
     * which is important for:
     * - Local development without API keys
     * - Testing other features
     * - Graceful degradation in production
     * 
     * Caching:
     * - Results are cached with key = cityName
     * - Cache name: "weather"
     * - Reduces API calls and improves response time
     * - Cache works even if empty (transparent to caller)
     * 
     * @param cityName City name to fetch weather for (e.g., "Paris", "New York")
     * @return Mono containing WeatherDTO if successful, empty Mono if API key not configured,
     *         error Mono if API call fails
     */
    @Cacheable(value = "weather", key = "#cityName", unless = "#result == null")
    public Mono<WeatherDTO> fetchCurrentWeather(String cityName) {
        // Graceful handling: If API key not configured, log warning and return empty
        // This prevents application startup failure
        if (apiConfig.getOpenweather().isPlaceholder()) {
            log.warn("OpenWeatherMap API key not configured. Skipping weather fetch for: {}. " +
                "Set OPENWEATHER_API_KEY environment variable to enable weather integration.", 
                cityName);
            return Mono.empty();
        }

        // Validate input
        if (cityName == null || cityName.isBlank()) {
            log.error("City name is required for weather fetch");
            return Mono.error(new IllegalArgumentException("City name cannot be null or empty"));
        }

        // Get configuration
        String apiKey = apiConfig.getOpenweather().getApiKey();
        String baseUrl = apiConfig.getOpenweather().getBaseUrl();

        log.debug("Fetching weather for city: {}", cityName);

        // Create a customized WebClient for OpenWeatherMap
        // mutate() creates a copy without affecting the shared bean
        return webClient.mutate()
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/weather")
                .queryParam("q", cityName)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")  // Use metric units (Celsius, meters/sec)
                .build())
            .retrieve()
            // Handle error responses (4xx, 5xx)
            .onStatus(HttpStatusCode::isError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("OpenWeatherMap API error: status={}, body={}", 
                            response.statusCode(), errorBody);
                        
                        // Create meaningful error message based on status code
                        String errorMessage;
                        if (response.statusCode().value() == 404) {
                            errorMessage = "City not found: " + cityName;
                        } else if (response.statusCode().value() == 401) {
                            errorMessage = "Invalid API key";
                        } else if (response.statusCode().value() == 429) {
                            errorMessage = "Rate limit exceeded";
                        } else {
                            errorMessage = "Failed to fetch weather: " + errorBody;
                        }
                        
                        return Mono.error(new ExternalApiException(
                            "OpenWeatherMap",
                            response.statusCode(),
                            errorMessage
                        ));
                    })
            )
            // Deserialize JSON response to internal response structure
            .bodyToMono(WeatherDTO.OpenWeatherResponse.class)
            // Map OpenWeatherMap response to our simplified DTO
            .map(WeatherDTO::fromOpenWeatherResponse)
            // Retry transient failures (5xx, timeouts, network errors)
            .retryWhen(Retry.backoff(
                apiConfig.getRetry().getMaxAttempts(), 
                Duration.ofMillis(apiConfig.getRetry().getBackoffMs())
            )
            .filter(throwable -> {
                // Only retry if it's a retryable error
                if (throwable instanceof ExternalApiException) {
                    ExternalApiException apiEx = (ExternalApiException) throwable;
                    boolean shouldRetry = apiEx.isRetryable();
                    log.debug("ExternalApiException: retryable={}", shouldRetry);
                    return shouldRetry;
                }
                // Retry network errors, timeouts, etc.
                return true;
            })
            .doBeforeRetry(retrySignal -> 
                log.warn("Retrying OpenWeatherMap API call (attempt {}/{})", 
                    retrySignal.totalRetries() + 1, 
                    apiConfig.getRetry().getMaxAttempts())
            ))
            .doOnSuccess(result -> {
                if (result != null) {
                    log.info("Successfully fetched weather for city: {} - Temp: {}°C, Condition: {}", 
                        cityName, result.getTemperature(), result.getWeatherMain());
                }
            })
            .doOnError(error -> 
                log.error("Failed to fetch weather for city: {}", cityName, error)
            );
    }

    /**
     * Check if the weather service is properly configured
     * 
     * This can be used by other services to check if weather integration is available
     * before attempting to fetch weather data.
     * 
     * @return true if API key is configured (not placeholder), false otherwise
     */
    public boolean isConfigured() {
        return !apiConfig.getOpenweather().isPlaceholder();
    }
}

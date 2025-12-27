package com.cityatlas.backend.service.external;

import java.time.Duration;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cityatlas.backend.config.ExternalApiConfig;
import com.cityatlas.backend.dto.response.AirQualityDTO;
import com.cityatlas.backend.exception.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Air Quality Service - OpenAQ API Integration
 * 
 * Provides real-time air quality data integration with OpenAQ API.
 * OpenAQ aggregates air quality data from monitoring stations worldwide.
 * 
 * Features:
 * - Fetches latest AQI (Air Quality Index) data by city
 * - Supports both public access and API key-based authentication
 * - Maps pollutant concentrations (PM2.5, PM10, O3, NO2, SO2, CO)
 * - Calculates AQI from PM2.5 measurements
 * - Handles missing data gracefully
 * - Does NOT fail application startup if API key is missing
 * 
 * OpenAQ API Notes:
 * - API v2 may work without key for basic queries (rate limited)
 * - API key increases rate limits and provides better access
 * - Some endpoints require authentication
 * 
 * Configuration:
 * - API key read from environment variable: OPENAQ_API_KEY
 * - Base URL configured in application.properties
 * - Timeouts and retry settings from ExternalApiConfig
 * 
 * @see AirQualityDTO
 * @see ExternalApiConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private final WebClient webClient;
    private final ExternalApiConfig apiConfig;

    /**
     * Fetch latest air quality data for a city
     * 
     * This method handles both public and authenticated access:
     * - If API key configured: Sends X-API-Key header for authenticated access
     * - If API key not configured: Tries public access (may be rate limited)
     * 
     * If no data is available or API fails, returns Mono.empty() instead of failing,
     * allowing graceful degradation of the application.
     * 
     * Caching:
     * - Results are cached with key = cityName
     * - Cache name: "airQuality"
     * - Reduces API calls and respects rate limits
     * - Cache works even if empty (transparent to caller)
     * 
     * @param cityName City name to fetch air quality for (e.g., "Paris", "Delhi")
     * @return Mono containing AirQualityDTO if data available, empty Mono otherwise
     */
    @Cacheable(value = "airQuality", key = "#cityName", unless = "#result == null")
    public Mono<AirQualityDTO> fetchAirQuality(String cityName) {
        // Validate input
        if (cityName == null || cityName.isBlank()) {
            log.error("City name is required for air quality fetch");
            return Mono.error(new IllegalArgumentException("City name cannot be null or empty"));
        }

        // Check if API key is configured
        boolean hasApiKey = !apiConfig.getOpenaq().isPlaceholder();
        String apiKey = hasApiKey ? apiConfig.getOpenaq().getApiKey() : null;
        String baseUrl = apiConfig.getOpenaq().getBaseUrl();

        if (!hasApiKey) {
            log.debug("OpenAQ API key not configured. Attempting public access for: {}", cityName);
        } else {
            log.debug("Fetching air quality for city: {} (authenticated)", cityName);
        }

        // Build WebClient with optional API key header
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.mutate()
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/latest")
                .queryParam("city", cityName)
                .queryParam("limit", "1")
                .queryParam("order_by", "lastUpdated")
                .queryParam("sort", "desc")
                .build());

        // Add API key header if available
        if (hasApiKey) {
            requestSpec = requestSpec.header("X-API-Key", apiKey);
        }

        return requestSpec
            .retrieve()
            // Handle error responses
            .onStatus(HttpStatusCode::isError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("OpenAQ API error: status={}, body={}", 
                            response.statusCode(), errorBody);
                        
                        // Create meaningful error message
                        String errorMessage;
                        if (response.statusCode().value() == 404) {
                            errorMessage = "No air quality data found for city: " + cityName;
                        } else if (response.statusCode().value() == 401 || response.statusCode().value() == 403) {
                            errorMessage = "Authentication failed. Check OPENAQ_API_KEY";
                        } else if (response.statusCode().value() == 429) {
                            errorMessage = "Rate limit exceeded. Set OPENAQ_API_KEY for higher limits";
                        } else {
                            errorMessage = "Failed to fetch air quality: " + errorBody;
                        }
                        
                        return Mono.error(new ExternalApiException(
                            "OpenAQ",
                            response.statusCode(),
                            errorMessage
                        ));
                    })
            )
            // Deserialize JSON response
            .bodyToMono(AirQualityDTO.OpenAqResponse.class)
            // Map to simplified DTO
            .map(AirQualityDTO::fromOpenAqResponse)
            // Handle case where no data is available
            .flatMap(airQuality -> {
                if (airQuality == null) {
                    log.warn("No air quality data available for city: {}", cityName);
                    return Mono.empty();
                }
                return Mono.just(airQuality);
            })
            // Retry transient failures
            .retryWhen(Retry.backoff(
                apiConfig.getRetry().getMaxAttempts(), 
                Duration.ofMillis(apiConfig.getRetry().getBackoffMs())
            )
            .filter(throwable -> {
                // Only retry retryable errors
                if (throwable instanceof ExternalApiException) {
                    ExternalApiException apiEx = (ExternalApiException) throwable;
                    boolean shouldRetry = apiEx.isRetryable();
                    log.debug("ExternalApiException: retryable={}", shouldRetry);
                    return shouldRetry;
                }
                // Retry network errors, timeouts
                return true;
            })
            .doBeforeRetry(retrySignal -> 
                log.warn("Retrying OpenAQ API call (attempt {}/{})", 
                    retrySignal.totalRetries() + 1, 
                    apiConfig.getRetry().getMaxAttempts())
            ))
            // Handle errors gracefully - return empty instead of failing
            .onErrorResume(throwable -> {
                // For non-retryable errors or after max retries, log and return empty
                // This prevents application crashes when air quality data is unavailable
                if (throwable instanceof ExternalApiException) {
                    ExternalApiException apiEx = (ExternalApiException) throwable;
                    if (apiEx.isClientError()) {
                        log.warn("Air quality data not available for city: {} - {}", 
                            cityName, apiEx.getMessage());
                    } else {
                        log.error("Failed to fetch air quality for city: {}", cityName, throwable);
                    }
                } else {
                    log.error("Unexpected error fetching air quality for city: {}", cityName, throwable);
                }
                return Mono.empty();
            })
            .doOnSuccess(result -> {
                if (result != null) {
                    log.info("Successfully fetched air quality for city: {} - AQI: {} ({})", 
                        cityName, result.getAqi(), result.getAqiCategory());
                }
            });
    }

    /**
     * Fetch air quality by coordinates (latitude, longitude)
     * 
     * Useful for precise location-based queries.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radiusKm Search radius in kilometers
     * @return Mono containing AirQualityDTO if data available, empty Mono otherwise
     */
    public Mono<AirQualityDTO> fetchAirQualityByCoordinates(Double latitude, Double longitude, Integer radiusKm) {
        // Validate input
        if (latitude == null || longitude == null) {
            log.error("Latitude and longitude are required for air quality fetch");
            return Mono.error(new IllegalArgumentException("Coordinates cannot be null"));
        }

        boolean hasApiKey = !apiConfig.getOpenaq().isPlaceholder();
        String apiKey = hasApiKey ? apiConfig.getOpenaq().getApiKey() : null;
        String baseUrl = apiConfig.getOpenaq().getBaseUrl();

        log.debug("Fetching air quality for coordinates: {}, {} (radius: {}km)", 
            latitude, longitude, radiusKm != null ? radiusKm : 25);

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.mutate()
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/latest")
                    .queryParam("coordinates", String.format("%f,%f", latitude, longitude))
                    .queryParam("limit", "1")
                    .queryParam("order_by", "lastUpdated")
                    .queryParam("sort", "desc");
                
                if (radiusKm != null) {
                    builder.queryParam("radius", radiusKm * 1000); // Convert to meters
                }
                
                return builder.build();
            });

        if (hasApiKey) {
            requestSpec = requestSpec.header("X-API-Key", apiKey);
        }

        return requestSpec
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                response.bodyToMono(String.class)
                    .flatMap(errorBody -> Mono.error(new ExternalApiException(
                        "OpenAQ",
                        response.statusCode(),
                        "Failed to fetch air quality by coordinates: " + errorBody
                    )))
            )
            .bodyToMono(AirQualityDTO.OpenAqResponse.class)
            .map(AirQualityDTO::fromOpenAqResponse)
            .flatMap(airQuality -> airQuality != null ? Mono.just(airQuality) : Mono.empty())
            .retryWhen(Retry.backoff(
                apiConfig.getRetry().getMaxAttempts(), 
                Duration.ofMillis(apiConfig.getRetry().getBackoffMs())
            )
            .filter(throwable -> 
                throwable instanceof ExternalApiException && 
                ((ExternalApiException) throwable).isRetryable()
            ))
            .onErrorResume(throwable -> {
                log.error("Failed to fetch air quality by coordinates: {}, {}", 
                    latitude, longitude, throwable);
                return Mono.empty();
            })
            .doOnSuccess(result -> {
                if (result != null) {
                    log.info("Successfully fetched air quality for coordinates {}, {} - AQI: {}", 
                        latitude, longitude, result.getAqi());
                }
            });
    }

    /**
     * Check if the air quality service is properly configured
     * 
     * Note: OpenAQ may work without API key (public access), so this returns true
     * even if key is not configured, just logs a warning.
     * 
     * @return true always (service available with or without key)
     */
    public boolean isConfigured() {
        boolean hasKey = !apiConfig.getOpenaq().isPlaceholder();
        if (!hasKey) {
            log.debug("OpenAQ API key not configured. Public access will be used (rate limited).");
        }
        return true; // Service is always available, with or without key
    }

    /**
     * Check if authenticated access is available (API key configured)
     * 
     * @return true if API key is configured, false otherwise
     */
    public boolean hasApiKey() {
        return !apiConfig.getOpenaq().isPlaceholder();
    }
}

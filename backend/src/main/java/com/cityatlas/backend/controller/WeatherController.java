package com.cityatlas.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.response.WeatherDTO;
import com.cityatlas.backend.service.external.WeatherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Weather Controller
 * 
 * Provides REST endpoints for weather data integration.
 * 
 * Endpoints:
 * - GET /weather/current?city={cityName} - Fetch current weather for a city
 * - GET /weather/status - Check if weather service is configured
 * 
 * Note: This controller handles reactive Mono responses from WeatherService.
 * Spring WebFlux automatically handles the async nature of Mono.
 */
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Get current weather for a city
     * 
     * Query parameter: city (required)
     * 
     * Response scenarios:
     * - 200 OK: Weather data successfully fetched
     * - 204 No Content: Weather service not configured (missing API key)
     * - 400 Bad Request: City parameter missing or invalid
     * - 404 Not Found: City not found in OpenWeatherMap
     * - 429 Too Many Requests: Rate limit exceeded
     * - 500 Internal Server Error: API call failed
     * 
     * @param cityName City name to fetch weather for
     * @return Mono with weather data or appropriate error
     */
    @GetMapping("/current")
    public Mono<ResponseEntity<WeatherDTO>> getCurrentWeather(
            @RequestParam(name = "city") String cityName) {
        
        log.info("Received weather request for city: {}", cityName);
        
        // Validate city parameter
        if (cityName == null || cityName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return weatherService.fetchCurrentWeather(cityName)
            .map(weather -> {
                log.debug("Successfully fetched weather for: {}", cityName);
                return ResponseEntity.ok(weather);
            })
            .defaultIfEmpty(
                // If Mono is empty (API key not configured), return 204 No Content
                // This is better than 500 error for graceful degradation
                ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            )
            .onErrorResume(throwable -> {
                log.error("Error fetching weather for city: {}", cityName, throwable);
                
                // Map exceptions to appropriate HTTP status codes
                if (throwable.getMessage() != null && throwable.getMessage().contains("City not found")) {
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                } else if (throwable.getMessage() != null && throwable.getMessage().contains("Rate limit")) {
                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
                } else if (throwable instanceof IllegalArgumentException) {
                    return Mono.just(ResponseEntity.badRequest().build());
                } else {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                }
            });
    }

    /**
     * Check weather service configuration status
     * 
     * Useful for:
     * - Health checks
     * - Frontend to know if weather integration is available
     * - Debugging configuration issues
     * 
     * @return Status message indicating if weather service is configured
     */
    @GetMapping("/status")
    public ResponseEntity<ServiceStatusResponse> getServiceStatus() {
        boolean isConfigured = weatherService.isConfigured();
        
        ServiceStatusResponse response = ServiceStatusResponse.builder()
            .service("OpenWeatherMap")
            .configured(isConfigured)
            .message(isConfigured 
                ? "Weather service is configured and ready" 
                : "Weather service not configured. Set OPENWEATHER_API_KEY environment variable.")
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Service status response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ServiceStatusResponse {
        private String service;
        private boolean configured;
        private String message;
    }
}

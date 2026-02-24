package com.cityatlas.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.response.AirQualityDTO;
import com.cityatlas.backend.service.external.AirQualityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Air Quality Controller
 * 
 * Provides REST endpoints for air quality data integration.
 * 
 * Endpoints:
 * - GET /air-quality/city?name={cityName} - Fetch AQI data by city name
 * - GET /air-quality/coordinates?lat={lat}&lon={lon}&radius={km} - Fetch by coordinates
 * - GET /air-quality/status - Check if service is configured
 */
@RestController
@RequestMapping("/air-quality")
@RequiredArgsConstructor
@Slf4j
public class AirQualityController {

    private final AirQualityService airQualityService;

    /**
     * Get air quality data for a city
     * 
     * Query parameter: name (required) - City name
     * 
     * Response scenarios:
     * - 200 OK: Air quality data successfully fetched
     * - 204 No Content: No air quality data available for city
     * - 400 Bad Request: City parameter missing or invalid
     * - 429 Too Many Requests: Rate limit exceeded (set API key for higher limits)
     * - 500 Internal Server Error: API call failed
     * 
     * @param cityName City name to fetch air quality for
     * @return Mono with air quality data or appropriate error
     */
    @GetMapping("/city")
    public Mono<ResponseEntity<AirQualityDTO>> getAirQualityByCity(
            @RequestParam(name = "name") String cityName) {
        
        log.info("Received air quality request for city: {}", cityName);
        
        // Validate city parameter
        if (cityName == null || cityName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return airQualityService.fetchAirQuality(cityName)
            .map(airQuality -> {
                log.debug("Successfully fetched air quality for: {}", cityName);
                return ResponseEntity.ok(airQuality);
            })
            .defaultIfEmpty(
                // If Mono is empty (no data available), return 204 No Content
                ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            )
            .onErrorResume(throwable -> {
                log.error("Error fetching air quality for city: {}", cityName, throwable);
                
                // Map exceptions to appropriate HTTP status codes
                if (throwable.getMessage() != null && throwable.getMessage().contains("No air quality data")) {
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
     * Get air quality data by coordinates
     * 
     * Query parameters:
     * - lat (required): Latitude
     * - lon (required): Longitude
     * - radius (optional): Search radius in kilometers (default: 25km)
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radiusKm Search radius in kilometers
     * @return Mono with air quality data or appropriate error
     */
    @GetMapping("/coordinates")
    public Mono<ResponseEntity<AirQualityDTO>> getAirQualityByCoordinates(
            @RequestParam(name = "lat") Double latitude,
            @RequestParam(name = "lon") Double longitude,
            @RequestParam(name = "radius", required = false, defaultValue = "25") Integer radiusKm) {
        
        log.info("Received air quality request for coordinates: {}, {} (radius: {}km)", 
            latitude, longitude, radiusKm);
        
        // Validate coordinates
        if (latitude == null || longitude == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return airQualityService.fetchAirQualityByCoordinates(latitude, longitude, radiusKm)
            .map(airQuality -> {
                log.debug("Successfully fetched air quality for coordinates: {}, {}", 
                    latitude, longitude);
                return ResponseEntity.ok(airQuality);
            })
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
            .onErrorResume(throwable -> {
                log.error("Error fetching air quality for coordinates: {}, {}", 
                    latitude, longitude, throwable);
                
                if (throwable instanceof IllegalArgumentException) {
                    return Mono.just(ResponseEntity.badRequest().build());
                } else {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                }
            });
    }

    /**
     * Check air quality service configuration status
     * 
     * Returns information about service availability and authentication status.
     * 
     * @return Status message indicating if service is configured
     */
    @GetMapping("/status")
    public ResponseEntity<ServiceStatusResponse> getServiceStatus() {
        boolean isConfigured = airQualityService.isConfigured();
        
        ServiceStatusResponse response = ServiceStatusResponse.builder()
            .service("Open-Meteo")
            .configured(isConfigured)
            .authenticated(false)
            .message("Air quality service powered by Open-Meteo (CAMS data). No API key required.")
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
        private boolean authenticated;
        private String message;
    }
}

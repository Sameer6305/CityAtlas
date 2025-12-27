package com.cityatlas.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.response.SpotifyMetadataDTO;
import com.cityatlas.backend.service.SpotifyService;

import reactor.core.publisher.Mono;

/**
 * Spotify API Controller
 * 
 * REST endpoints for accessing Spotify music metadata for cities.
 * Provides cultural and music scene information without audio streaming.
 * 
 * Endpoints:
 * - GET /api/spotify/city?name={cityName} - Get city music metadata
 * - GET /api/spotify/status - Check Spotify API configuration
 * - POST /api/spotify/refresh-token - Force token refresh (admin)
 * 
 * Response Codes:
 * - 200 OK: Data retrieved successfully
 * - 204 No Content: No data available (not configured or no results)
 * - 400 Bad Request: Invalid request parameters
 * - 429 Too Many Requests: Spotify rate limit exceeded
 * - 500 Internal Server Error: Unexpected error
 * 
 * Usage Example:
 * <pre>
 * GET /api/spotify/city?name=Austin
 * Response:
 * {
 *   "cityName": "Austin",
 *   "artists": [...],
 *   "playlists": [...],
 *   "genres": ["country", "rock", "indie"],
 *   "totalResults": 15
 * }
 * </pre>
 * 
 * IMPORTANT:
 * - No audio streaming or downloads
 * - Metadata only for cultural analysis
 * - Respects Spotify API terms of service
 * 
 * @see SpotifyService
 */
@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);
    
    private final SpotifyService spotifyService;
    
    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }
    
    /**
     * Get Spotify metadata for a city
     * 
     * Returns music scene information including local artists,
     * popular playlists, and genre tags.
     * 
     * @param cityName Name of the city (e.g., "Nashville", "Tokyo")
     * @return Spotify metadata or 204 if not available
     */
    @GetMapping("/city")
    public Mono<ResponseEntity<SpotifyMetadataDTO>> getCityMetadata(
            @RequestParam(name = "name") String cityName) {
        
        if (cityName == null || cityName.trim().isEmpty()) {
            logger.warn("City name parameter is required");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        logger.info("Fetching Spotify metadata for city: {}", cityName);
        
        return spotifyService.getCityMetadata(cityName)
                .map(metadata -> {
                    if (metadata.getTotalResults() == 0) {
                        logger.info("No Spotify results found for {}", cityName);
                        return ResponseEntity.noContent().<SpotifyMetadataDTO>build();
                    }
                    
                    logger.info("Found {} Spotify results for {} ({} artists, {} playlists)", 
                            metadata.getTotalResults(),
                            cityName,
                            metadata.getArtists().size(),
                            metadata.getPlaylists().size());
                    
                    return ResponseEntity.ok(metadata);
                })
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(error -> {
                    logger.error("Error fetching Spotify metadata for {}: {}", cityName, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    
    /**
     * Check Spotify API configuration status
     * 
     * Returns information about whether credentials are configured
     * and if the service is ready to make API calls.
     * 
     * @return Configuration status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        boolean hasCredentials = spotifyService.hasCredentials();
        status.put("configured", hasCredentials);
        status.put("service", "Spotify");
        status.put("authType", "OAuth2 Client Credentials");
        
        if (!hasCredentials) {
            status.put("message", "Spotify credentials not configured. Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET.");
            status.put("instructions", "See EXTERNAL_API_SETUP.md for configuration details");
        } else {
            status.put("message", "Spotify API is configured and ready");
            status.put("capabilities", new String[]{
                "Search artists by city",
                "Search playlists by city",
                "Extract genre metadata",
                "Metadata only (no audio streaming)"
            });
        }
        
        logger.debug("Spotify service status: configured={}", hasCredentials);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Force refresh of Spotify access token
     * 
     * Clears cached token and forces re-authentication.
     * Useful for testing or when token becomes invalid.
     * 
     * @return Success message
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken() {
        logger.info("Manual token refresh requested");
        
        if (!spotifyService.hasCredentials()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Spotify credentials not configured");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        spotifyService.clearTokenCache();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Token cache cleared. New token will be acquired on next API call.");
        
        return ResponseEntity.ok(response);
    }
}

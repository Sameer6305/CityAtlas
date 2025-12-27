package com.cityatlas.backend.controller;

import com.cityatlas.backend.dto.response.UnsplashImageDTO;
import com.cityatlas.backend.service.CityImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unsplash Image API Controller
 * 
 * REST endpoints for accessing high-quality city images from Unsplash.
 * Handles image search, random images, and download tracking.
 * 
 * Endpoints:
 * - GET /api/images/city?name={cityName}&count={count} - Search city images
 * - GET /api/images/random?city={cityName} - Get random city image
 * - POST /api/images/track-download - Track image download (required)
 * - GET /api/images/status - Check Unsplash API configuration
 * 
 * Response Codes:
 * - 200 OK: Images retrieved successfully
 * - 204 No Content: No images available (not configured or no results)
 * - 400 Bad Request: Invalid request parameters
 * - 403 Forbidden: Invalid API key
 * - 429 Too Many Requests: Rate limit exceeded
 * - 500 Internal Server Error: Unexpected error
 * 
 * Usage Example:
 * <pre>
 * GET /api/images/city?name=Paris&count=5
 * Response:
 * [
 *   {
 *     "id": "abc123",
 *     "urls": {
 *       "regular": "https://images.unsplash.com/...",
 *       "small": "https://images.unsplash.com/..."
 *     },
 *     "user": {
 *       "name": "John Doe",
 *       "username": "johndoe"
 *     },
 *     "description": "Paris city skyline",
 *     "downloadLocation": "https://api.unsplash.com/photos/abc123/download"
 *   }
 * ]
 * </pre>
 * 
 * IMPORTANT - Attribution Requirements:
 * - Always display photographer name and "on Unsplash"
 * - Link to photographer's profile with UTM parameters
 * - Call /track-download when using an image
 * 
 * @see CityImageService
 */
@RestController
@RequestMapping("/api/images")
public class UnsplashController {
    
    private static final Logger logger = LoggerFactory.getLogger(UnsplashController.class);
    
    private final CityImageService cityImageService;
    
    public UnsplashController(CityImageService cityImageService) {
        this.cityImageService = cityImageService;
    }
    
    /**
     * Search for city images
     * 
     * Returns a list of high-quality images for the specified city.
     * Results are landscape-oriented and suitable for headers/galleries.
     * 
     * @param cityName Name of the city (e.g., "Tokyo", "New York")
     * @param count Number of images to return (1-30, default 10)
     * @return List of images or 204 if not available
     */
    @GetMapping("/city")
    public Mono<ResponseEntity<List<UnsplashImageDTO>>> searchCityImages(
            @RequestParam(name = "name") String cityName,
            @RequestParam(name = "count", required = false, defaultValue = "10") Integer count) {
        
        if (cityName == null || cityName.trim().isEmpty()) {
            logger.warn("City name parameter is required");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        if (count != null && (count < 1 || count > 30)) {
            logger.warn("Count must be between 1 and 30, got: {}", count);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        logger.info("Searching images for city: {} (count: {})", cityName, count);
        
        return cityImageService.searchCityImages(cityName, count)
                .map(images -> {
                    if (images.isEmpty()) {
                        logger.info("No images found for {}", cityName);
                        return ResponseEntity.noContent().<List<UnsplashImageDTO>>build();
                    }
                    
                    logger.info("Returning {} images for {}", images.size(), cityName);
                    return ResponseEntity.ok(images);
                })
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(error -> {
                    logger.error("Error searching images for {}: {}", cityName, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    
    /**
     * Get a random city image
     * 
     * Returns a single random image for the specified city.
     * Useful for featured images or variety in display.
     * 
     * @param cityName Name of the city
     * @return Random image or 204 if not available
     */
    @GetMapping("/random")
    public Mono<ResponseEntity<UnsplashImageDTO>> getRandomCityImage(
            @RequestParam(name = "city") String cityName) {
        
        if (cityName == null || cityName.trim().isEmpty()) {
            logger.warn("City parameter is required");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        logger.info("Fetching random image for city: {}", cityName);
        
        return cityImageService.getRandomCityImage(cityName)
                .map(image -> {
                    logger.info("Returning random image for {} (ID: {})", cityName, image.getId());
                    return ResponseEntity.ok(image);
                })
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(error -> {
                    logger.error("Error fetching random image for {}: {}", cityName, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    
    /**
     * Track image download
     * 
     * MUST be called when using an image (per Unsplash API guidelines).
     * Helps support photographers and maintain API access.
     * 
     * @param downloadLocation Download tracking URL from image object
     * @return Success message
     */
    @PostMapping("/track-download")
    public Mono<ResponseEntity<Map<String, String>>> trackDownload(
            @RequestBody Map<String, String> request) {
        
        String downloadLocation = request.get("downloadLocation");
        
        if (downloadLocation == null || downloadLocation.trim().isEmpty()) {
            logger.warn("downloadLocation parameter is required");
            Map<String, String> error = new HashMap<>();
            error.put("error", "downloadLocation is required");
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
        
        logger.info("Tracking image download: {}", downloadLocation);
        
        return cityImageService.trackDownload(downloadLocation)
                .then(Mono.fromCallable(() -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Download tracked successfully");
                    return ResponseEntity.ok(response);
                }))
                .onErrorResume(error -> {
                    logger.error("Error tracking download: {}", error.getMessage());
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Failed to track download");
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse));
                });
    }
    
    /**
     * Check Unsplash API configuration status
     * 
     * Returns information about whether access key is configured
     * and if the service is ready to fetch images.
     * 
     * @return Configuration status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        boolean hasAccessKey = cityImageService.hasAccessKey();
        status.put("configured", hasAccessKey);
        status.put("service", "Unsplash");
        
        if (!hasAccessKey) {
            status.put("message", "Unsplash access key not configured. Set UNSPLASH_ACCESS_KEY.");
            status.put("instructions", "See EXTERNAL_API_SETUP.md for configuration details");
        } else {
            status.put("message", "Unsplash API is configured and ready");
            status.put("capabilities", new String[]{
                "Search city images",
                "Get random city image",
                "Track image downloads",
                "Automatic photographer attribution"
            });
            status.put("rateLimit", "50 requests/hour (demo), 5000 requests/hour (production)");
            status.put("attribution", "REQUIRED - Must display photographer name and link");
        }
        
        logger.debug("Unsplash service status: configured={}", hasAccessKey);
        
        return ResponseEntity.ok(status);
    }
}

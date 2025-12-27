package com.cityatlas.backend.service;

import com.cityatlas.backend.config.ExternalApiConfig;
import com.cityatlas.backend.dto.response.UnsplashImageDTO;
import com.cityatlas.backend.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Unsplash API Integration Service (City Image Service)
 * 
 * Provides access to high-quality city images from Unsplash.
 * Used for visual content in city profiles and galleries.
 * 
 * Features:
 * - Search images by city name
 * - Get random city images
 * - Automatic attribution handling
 * - Rate limit management
 * - Graceful degradation
 * 
 * Usage Example:
 * <pre>
 * {@code
 * Mono<List<UnsplashImageDTO>> images = cityImageService.searchCityImages("Paris", 10);
 * images.subscribe(imageList -> {
 *     imageList.forEach(img -> {
 *         System.out.println(img.getUrls().getRegular());
 *         System.out.println(img.getAttributionText());
 *     });
 * });
 * }
 * </pre>
 * 
 * IMPORTANT - Unsplash API Guidelines:
 * - MUST display photographer attribution
 * - MUST trigger download tracking URL when using images
 * - MUST include UTM parameters in attribution links
 * - Rate limit: 50 requests/hour (demo), 5000/hour (production)
 * 
 * Attribution Example:
 * <pre>
 * Photo by [Photographer Name] on Unsplash
 * Link: https://unsplash.com/@username?utm_source=cityatlas&utm_medium=referral
 * </pre>
 * 
 * @see <a href="https://unsplash.com/documentation">Unsplash API Documentation</a>
 * @see <a href="https://help.unsplash.com/en/articles/2511245-unsplash-api-guidelines">API Guidelines</a>
 */
@Service
public class CityImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(CityImageService.class);
    
    private final WebClient webClient;
    private final ExternalApiConfig.UnsplashConfig unsplashConfig;
    
    public CityImageService(WebClient webClient, ExternalApiConfig externalApiConfig) {
        this.webClient = webClient;
        this.unsplashConfig = externalApiConfig.getUnsplash();
    }
    
    /**
     * Check if Unsplash API key is properly configured
     * 
     * @return true if access key is available and not a placeholder
     */
    public boolean hasAccessKey() {
        return unsplashConfig != null 
                && !unsplashConfig.isPlaceholder()
                && unsplashConfig.getAccessKey() != null;
    }
    
    /**
     * Search for city images
     * 
     * @param cityName Name of the city
     * @param count Number of images to return (max 30)
     * @return Mono with list of images
     */
    public Mono<List<UnsplashImageDTO>> searchCityImages(String cityName, Integer count) {
        if (!hasAccessKey()) {
            logger.warn("Unsplash access key not configured, cannot search images for {}", cityName);
            return Mono.empty();
        }
        
        // Limit count to prevent excessive results
        int limitedCount = Math.min(count != null ? count : 10, 30);
        
        String searchQuery = cityName + " city skyline architecture";
        String searchUrl = String.format(
                "%s/search/photos?query=%s&per_page=%d&orientation=landscape",
                unsplashConfig.getBaseUrl(),
                searchQuery.replace(" ", "+"),
                limitedCount
        );
        
        logger.info("Searching Unsplash for city images: {} (limit: {})", cityName, limitedCount);
        
        return webClient.get()
                .uri(searchUrl)
                .header(HttpHeaders.AUTHORIZATION, "Client-ID " + unsplashConfig.getAccessKey())
                .header("Accept-Version", "v1")
                .retrieve()
                .onStatus(
                        status -> status.value() == 403,
                        response -> {
                            logger.error("Unsplash access forbidden - check API key validity");
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    HttpStatus.FORBIDDEN,
                                    "Access forbidden - invalid API key or rate limit exceeded"
                            ));
                        }
                )
                .onStatus(
                        status -> status.value() == 429,
                        response -> {
                            logger.warn("Unsplash rate limit exceeded");
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded - 50 requests/hour on demo tier"
                            ));
                        }
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            logger.error("Unsplash search failed: {} - {}", response.statusCode(), body);
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    response.statusCode(),
                                    "Search failed: " + response.statusCode()
                            ));
                        })
                )
                .bodyToMono(UnsplashImageDTO.UnsplashSearchResponse.class)
                .map(response -> {
                    if (response == null || response.getResults() == null) {
                        logger.warn("No images found for {}", cityName);
                        return List.<UnsplashImageDTO>of();
                    }
                    
                    logger.info("Found {} images for {} (total available: {})", 
                            response.getResults().size(), cityName, response.getTotal());
                    return response.getResults();
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            if (throwable instanceof ExternalApiException) {
                                ExternalApiException apiEx = (ExternalApiException) throwable;
                                return apiEx.isRetryable();
                            }
                            return false;
                        })
                )
                .onErrorResume(error -> {
                    logger.error("Failed to search Unsplash for {}: {}", cityName, error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Get a random city image
     * 
     * @param cityName Name of the city
     * @return Mono with random image
     */
    public Mono<UnsplashImageDTO> getRandomCityImage(String cityName) {
        if (!hasAccessKey()) {
            logger.warn("Unsplash access key not configured, cannot get random image for {}", cityName);
            return Mono.empty();
        }
        
        String searchQuery = cityName + " city";
        String randomUrl = String.format(
                "%s/photos/random?query=%s&orientation=landscape",
                unsplashConfig.getBaseUrl(),
                searchQuery.replace(" ", "+")
        );
        
        logger.info("Fetching random image for city: {}", cityName);
        
        return webClient.get()
                .uri(randomUrl)
                .header(HttpHeaders.AUTHORIZATION, "Client-ID " + unsplashConfig.getAccessKey())
                .header("Accept-Version", "v1")
                .retrieve()
                .onStatus(
                        status -> status.value() == 403,
                        response -> {
                            logger.error("Unsplash access forbidden - check API key validity");
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    HttpStatus.FORBIDDEN,
                                    "Access forbidden - invalid API key"
                            ));
                        }
                )
                .onStatus(
                        status -> status.value() == 429,
                        response -> {
                            logger.warn("Unsplash rate limit exceeded");
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded"
                            ));
                        }
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            logger.error("Unsplash random image failed: {} - {}", response.statusCode(), body);
                            return Mono.error(new ExternalApiException(
                                    "Unsplash",
                                    response.statusCode(),
                                    "Random image failed: " + response.statusCode()
                            ));
                        })
                )
                .bodyToMono(UnsplashImageDTO.class)
                .doOnNext(image -> {
                    if (image != null) {
                        logger.info("Retrieved random image for {} (ID: {})", cityName, image.getId());
                    }
                })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            if (throwable instanceof ExternalApiException) {
                                ExternalApiException apiEx = (ExternalApiException) throwable;
                                return apiEx.isRetryable();
                            }
                            return false;
                        })
                )
                .onErrorResume(error -> {
                    logger.error("Failed to get random image for {}: {}", cityName, error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Track image download
     * MUST be called when using an image (per Unsplash API guidelines)
     * 
     * @param downloadLocation Download tracking URL from image object
     * @return Mono that completes when tracking is done
     */
    public Mono<Void> trackDownload(String downloadLocation) {
        if (!hasAccessKey() || downloadLocation == null || downloadLocation.isEmpty()) {
            logger.debug("Skipping download tracking (no key or invalid URL)");
            return Mono.empty();
        }
        
        logger.debug("Tracking image download: {}", downloadLocation);
        
        return webClient.get()
                .uri(downloadLocation)
                .header(HttpHeaders.AUTHORIZATION, "Client-ID " + unsplashConfig.getAccessKey())
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.debug("Download tracked successfully"))
                .onErrorResume(error -> {
                    logger.warn("Failed to track download: {}", error.getMessage());
                    return Mono.empty(); // Don't fail the main operation if tracking fails
                });
    }
}

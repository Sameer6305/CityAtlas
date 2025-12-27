package com.cityatlas.backend.service;

import com.cityatlas.backend.config.ExternalApiConfig;
import com.cityatlas.backend.dto.response.SpotifyAuthDTO;
import com.cityatlas.backend.dto.response.SpotifyMetadataDTO;
import com.cityatlas.backend.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spotify API Integration Service
 * 
 * Provides access to Spotify metadata for city cultural analysis.
 * Uses OAuth2 Client Credentials flow for authentication.
 * 
 * Features:
 * - Automatic token management (acquire, cache, refresh)
 * - City-specific music metadata (artists, playlists, genres)
 * - Graceful degradation (works without credentials in dev)
 * - Retry logic for transient failures
 * 
 * Authentication Flow:
 * 1. Check if token exists and is valid
 * 2. If not, request new token from Spotify
 * 3. Cache token until expiration
 * 4. Use token for all API calls
 * 
 * Usage Example:
 * <pre>
 * {@code
 * Mono<SpotifyMetadataDTO> metadata = spotifyService.getCityMetadata("Nashville");
 * metadata.subscribe(data -> {
 *     System.out.println("Found " + data.getArtists().size() + " artists");
 * });
 * }
 * </pre>
 * 
 * IMPORTANT:
 * - Does NOT stream or download audio
 * - Metadata only (artist names, playlist titles, genres)
 * - Respects Spotify API rate limits
 * 
 * @see <a href="https://developer.spotify.com/documentation/web-api">Spotify Web API</a>
 */
@Service
public class SpotifyService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);
    
    private final WebClient webClient;
    private final ExternalApiConfig.SpotifyConfig spotifyConfig;
    
    // Token cache (in-memory, single instance)
    private volatile SpotifyAuthDTO cachedToken;
    private final Object tokenLock = new Object();
    
    public SpotifyService(WebClient webClient, ExternalApiConfig externalApiConfig) {
        this.webClient = webClient;
        this.spotifyConfig = externalApiConfig.getSpotify();
    }
    
    /**
     * Check if Spotify credentials are properly configured
     * 
     * @return true if credentials are available and not placeholders
     */
    public boolean hasCredentials() {
        return spotifyConfig != null 
                && !spotifyConfig.isPlaceholder()
                && spotifyConfig.getClientId() != null
                && spotifyConfig.getClientSecret() != null;
    }
    
    /**
     * Get or refresh access token
     * Thread-safe token caching with automatic refresh
     * 
     * @return Mono with valid access token
     */
    private Mono<SpotifyAuthDTO> getAccessToken() {
        // Check if cached token is still valid
        if (cachedToken != null && cachedToken.isValid()) {
            logger.debug("Using cached Spotify token (expires in {} seconds)", 
                    cachedToken.getSecondsUntilExpiration());
            return Mono.just(cachedToken);
        }
        
        // Need to acquire new token
        synchronized (tokenLock) {
            // Double-check after acquiring lock
            if (cachedToken != null && cachedToken.isValid()) {
                return Mono.just(cachedToken);
            }
            
            logger.info("Acquiring new Spotify access token...");
            return authenticateWithSpotify()
                    .doOnSuccess(token -> {
                        cachedToken = token;
                        logger.info("Spotify token acquired successfully (expires in {} seconds)", 
                                token.getExpiresIn());
                    })
                    .doOnError(error -> {
                        logger.error("Failed to acquire Spotify token: {}", error.getMessage());
                    });
        }
    }
    
    /**
     * Authenticate with Spotify using Client Credentials flow
     * 
     * OAuth2 Flow:
     * 1. POST to https://accounts.spotify.com/api/token
     * 2. Authorization: Basic base64(client_id:client_secret)
     * 3. Body: grant_type=client_credentials
     * 4. Response: access_token, token_type, expires_in
     * 
     * @return Mono with authentication response
     */
    private Mono<SpotifyAuthDTO> authenticateWithSpotify() {
        if (!hasCredentials()) {
            logger.warn("Spotify credentials not configured, skipping authentication");
            return Mono.empty();
        }
        
        // Create Basic Authorization header
        String credentials = spotifyConfig.getClientId() + ":" + spotifyConfig.getClientSecret();
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        
        // Prepare form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        
        return webClient.post()
                .uri(spotifyConfig.getAuthUrl())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            logger.error("Spotify authentication failed: {} - {}", response.statusCode(), body);
                            return Mono.error(new ExternalApiException(
                                    "Spotify",
                                    response.statusCode(),
                                    "Authentication failed: " + response.statusCode()
                            ));
                        })
                )
                .bodyToMono(SpotifyAuthDTO.class)
                .doOnNext(SpotifyAuthDTO::setIssuedAtNow)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            if (throwable instanceof ExternalApiException) {
                                ExternalApiException apiEx = (ExternalApiException) throwable;
                                return apiEx.isRetryable();
                            }
                            return false;
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("Spotify authentication retry exhausted after {} attempts", 
                                    retrySignal.totalRetries());
                            return retrySignal.failure();
                        })
                )
                .onErrorResume(error -> {
                    logger.error("Spotify authentication error: {}", error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Get music metadata for a specific city
     * 
     * Searches Spotify for:
     * - Artists associated with the city
     * - Playlists mentioning the city
     * - Popular genres in the region
     * 
     * @param cityName Name of the city
     * @return Mono with aggregated metadata
     */
    public Mono<SpotifyMetadataDTO> getCityMetadata(String cityName) {
        if (!hasCredentials()) {
            logger.warn("Spotify credentials not configured, returning empty metadata for {}", cityName);
            return Mono.empty();
        }
        
        return getAccessToken()
                .flatMap(token -> searchSpotify(cityName, token))
                .map(searchResponse -> buildMetadataDTO(cityName, searchResponse))
                .onErrorResume(error -> {
                    logger.error("Failed to fetch Spotify metadata for {}: {}", cityName, error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Search Spotify API for city-related content
     * 
     * @param cityName City to search for
     * @param token Valid access token
     * @return Mono with search response
     */
    private Mono<SpotifyMetadataDTO.SpotifySearchResponse> searchSpotify(
            String cityName, 
            SpotifyAuthDTO token) {
        
        String query = cityName.replace(" ", "+");
        String searchUrl = String.format(
                "%s/search?q=%s&type=artist,playlist&limit=10",
                spotifyConfig.getBaseUrl(),
                query
        );
        
        return webClient.get()
                .uri(searchUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .retrieve()
                .onStatus(
                        status -> status.value() == 429,
                        response -> {
                            logger.warn("Spotify rate limit exceeded");
                            return Mono.error(new ExternalApiException(
                                    "Spotify",
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    "Rate limit exceeded"
                            ));
                        }
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            logger.error("Spotify search failed: {} - {}", response.statusCode(), body);
                            return Mono.error(new ExternalApiException(
                                    "Spotify",
                                    response.statusCode(),
                                    "Search failed: " + response.statusCode()
                            ));
                        })
                )
                .bodyToMono(SpotifyMetadataDTO.SpotifySearchResponse.class)
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
                    logger.error("Spotify search error for {}: {}", cityName, error.getMessage());
                    return Mono.empty();
                });
    }
    
    /**
     * Build metadata DTO from Spotify search response
     * 
     * @param cityName City name
     * @param searchResponse Spotify API response
     * @return Populated metadata DTO
     */
    private SpotifyMetadataDTO buildMetadataDTO(
            String cityName, 
            SpotifyMetadataDTO.SpotifySearchResponse searchResponse) {
        
        if (searchResponse == null) {
            return SpotifyMetadataDTO.builder()
                    .cityName(cityName)
                    .artists(new ArrayList<>())
                    .playlists(new ArrayList<>())
                    .genres(new ArrayList<>())
                    .totalResults(0)
                    .build();
        }
        
        // Convert artists
        List<SpotifyMetadataDTO.Artist> artists = new ArrayList<>();
        Set<String> allGenres = new HashSet<>();
        
        if (searchResponse.getArtists() != null && searchResponse.getArtists().getItems() != null) {
            artists = searchResponse.getArtists().getItems().stream()
                    .map(SpotifyMetadataDTO.Artist::fromSpotifyResponse)
                    .collect(Collectors.toList());
            
            // Extract genres from all artists
            searchResponse.getArtists().getItems().forEach(artist -> {
                if (artist.getGenres() != null) {
                    allGenres.addAll(artist.getGenres());
                }
            });
        }
        
        // Convert playlists
        List<SpotifyMetadataDTO.Playlist> playlists = new ArrayList<>();
        if (searchResponse.getPlaylists() != null && searchResponse.getPlaylists().getItems() != null) {
            playlists = searchResponse.getPlaylists().getItems().stream()
                    .map(SpotifyMetadataDTO.Playlist::fromSpotifyResponse)
                    .collect(Collectors.toList());
        }
        
        int totalResults = artists.size() + playlists.size();
        
        return SpotifyMetadataDTO.builder()
                .cityName(cityName)
                .artists(artists)
                .playlists(playlists)
                .genres(new ArrayList<>(allGenres))
                .totalResults(totalResults)
                .build();
    }
    
    /**
     * Clear cached token (for testing or manual refresh)
     */
    public void clearTokenCache() {
        synchronized (tokenLock) {
            cachedToken = null;
            logger.info("Spotify token cache cleared");
        }
    }
}

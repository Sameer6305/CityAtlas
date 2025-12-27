package com.cityatlas.backend.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spotify OAuth2 Access Token Response
 * 
 * Represents the response from Spotify's token endpoint when using
 * Client Credentials flow for server-to-server authentication.
 * 
 * OAuth2 Flow:
 * 1. POST to https://accounts.spotify.com/api/token
 * 2. Authorization: Basic {base64(client_id:client_secret)}
 * 3. grant_type=client_credentials
 * 4. Response: access_token, token_type, expires_in
 * 
 * Usage Example:
 * <pre>
 * {@code
 * SpotifyAuthDTO auth = spotifyService.authenticate();
 * String token = auth.getAccessToken();
 * boolean valid = auth.isValid();
 * }
 * </pre>
 * 
 * Token Lifecycle:
 * - Tokens typically expire after 3600 seconds (1 hour)
 * - Must be refreshed before making API calls
 * - No refresh_token provided (client credentials flow)
 * 
 * @see <a href="https://developer.spotify.com/documentation/web-api/tutorials/client-credentials-flow">Spotify Client Credentials Flow</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAuthDTO {
    
    /**
     * The access token for Spotify API calls
     * Use in Authorization: Bearer {access_token} header
     */
    @JsonProperty("access_token")
    private String accessToken;
    
    /**
     * Token type (always "Bearer" for Spotify)
     */
    @JsonProperty("token_type")
    private String tokenType;
    
    /**
     * Token expiration time in seconds (typically 3600)
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    /**
     * Timestamp when token was issued (not from Spotify, set locally)
     */
    private Instant issuedAt;
    
    /**
     * Check if token is still valid
     * Considers a 60-second buffer before actual expiration
     * 
     * @return true if token is valid and not expired
     */
    public boolean isValid() {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        
        if (issuedAt == null || expiresIn == null) {
            return false;
        }
        
        // Token is valid if current time < (issued_at + expires_in - 60 second buffer)
        Instant expirationTime = issuedAt.plusSeconds(expiresIn - 60);
        return Instant.now().isBefore(expirationTime);
    }
    
    /**
     * Get time remaining until token expires (in seconds)
     * 
     * @return seconds until expiration, 0 if already expired
     */
    public long getSecondsUntilExpiration() {
        if (!isValid()) {
            return 0;
        }
        
        Instant expirationTime = issuedAt.plusSeconds(expiresIn);
        long seconds = expirationTime.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }
    
    /**
     * Set the issued timestamp to current time
     * Should be called immediately after receiving token from Spotify
     */
    public void setIssuedAtNow() {
        this.issuedAt = Instant.now();
    }
}

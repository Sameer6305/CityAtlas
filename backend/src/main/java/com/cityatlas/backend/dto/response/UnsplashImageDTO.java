package com.cityatlas.backend.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unsplash City Image Response
 * 
 * Represents high-quality city images from Unsplash API.
 * Used for visual content in city profiles and galleries.
 * 
 * Features:
 * - Multiple image sizes (raw, full, regular, small, thumb)
 * - Photographer attribution
 * - Download tracking URLs
 * - Color and dimension metadata
 * 
 * Usage Example:
 * <pre>
 * {@code
 * UnsplashImageDTO image = unsplashService.getCityImage("Paris");
 * String imageUrl = image.getUrls().getRegular();
 * String attribution = image.getUser().getName();
 * }
 * </pre>
 * 
 * IMPORTANT:
 * - Must provide attribution to photographer
 * - Use download_location to track downloads
 * - Respect Unsplash API guidelines
 * 
 * @see <a href="https://unsplash.com/documentation">Unsplash API Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnsplashImageDTO {
    
    /**
     * Unique image ID
     */
    private String id;
    
    /**
     * Image creation timestamp
     */
    @JsonProperty("created_at")
    private String createdAt;
    
    /**
     * Image width in pixels
     */
    private Integer width;
    
    /**
     * Image height in pixels
     */
    private Integer height;
    
    /**
     * Dominant color (hex code)
     */
    private String color;
    
    /**
     * Number of likes
     */
    private Integer likes;
    
    /**
     * Image description
     */
    private String description;
    
    /**
     * Alternative description for accessibility
     */
    @JsonProperty("alt_description")
    private String altDescription;
    
    /**
     * Image URLs in various sizes
     */
    private ImageUrls urls;
    
    /**
     * Photographer information
     */
    private User user;
    
    /**
     * External links
     */
    private Links links;
    
    /**
     * Download tracking URL (MUST trigger this when downloading)
     */
    @JsonProperty("download_location")
    private String downloadLocation;
    
    /**
     * Get attribution text for photographer
     * Required by Unsplash API guidelines
     * 
     * @return Formatted attribution string
     */
    public String getAttributionText() {
        if (user != null && user.getName() != null) {
            return String.format("Photo by %s on Unsplash", user.getName());
        }
        return "Photo from Unsplash";
    }
    
    /**
     * Get photographer link for attribution
     * 
     * @return URL to photographer's Unsplash profile
     */
    public String getAttributionLink() {
        if (user != null && user.getLinks() != null && user.getLinks().getHtml() != null) {
            return user.getLinks().getHtml() + "?utm_source=cityatlas&utm_medium=referral";
        }
        return "https://unsplash.com/?utm_source=cityatlas&utm_medium=referral";
    }
    
    /**
     * Image URLs in different sizes
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageUrls {
        
        /**
         * Raw unprocessed image (largest, requires special permission)
         */
        private String raw;
        
        /**
         * Full resolution image (recommended for high-quality display)
         */
        private String full;
        
        /**
         * Regular size (1080px width, most common use case)
         */
        private String regular;
        
        /**
         * Small size (400px width, for thumbnails)
         */
        private String small;
        
        /**
         * Thumbnail (200px width, for previews)
         */
        private String thumb;
    }
    
    /**
     * Photographer information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        
        /**
         * User ID
         */
        private String id;
        
        /**
         * Username
         */
        private String username;
        
        /**
         * Display name
         */
        private String name;
        
        /**
         * Portfolio/website URL
         */
        @JsonProperty("portfolio_url")
        private String portfolioUrl;
        
        /**
         * User profile image
         */
        @JsonProperty("profile_image")
        private ProfileImage profileImage;
        
        /**
         * External links
         */
        private UserLinks links;
    }
    
    /**
     * Profile image URLs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileImage {
        private String small;
        private String medium;
        private String large;
    }
    
    /**
     * User profile links
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserLinks {
        
        /**
         * User profile HTML page
         */
        private String html;
        
        /**
         * User photos API endpoint
         */
        private String photos;
        
        /**
         * User likes API endpoint
         */
        private String likes;
    }
    
    /**
     * Image links
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        
        /**
         * Image page on Unsplash
         */
        private String html;
        
        /**
         * Direct download URL
         */
        private String download;
        
        /**
         * Download tracking URL
         */
        @JsonProperty("download_location")
        private String downloadLocation;
    }
    
    /**
     * Unsplash search response wrapper
     * Internal class for JSON deserialization
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnsplashSearchResponse {
        
        /**
         * Total number of results
         */
        private Integer total;
        
        /**
         * Total pages available
         */
        @JsonProperty("total_pages")
        private Integer totalPages;
        
        /**
         * Array of image results
         */
        private List<UnsplashImageDTO> results;
    }
}

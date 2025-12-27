package com.cityatlas.backend.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spotify City Metadata Response
 * 
 * Aggregates music and cultural metadata for a specific city from Spotify.
 * Includes local artists, popular playlists, and genre tags to represent
 * the city's music scene.
 * 
 * Data Sources:
 * - Search API: Find artists/playlists by city name
 * - Artist API: Get detailed artist information
 * - Playlist API: Get playlist details and tracks
 * 
 * Usage Example:
 * <pre>
 * {@code
 * SpotifyMetadataDTO metadata = spotifyService.getCityMetadata("Tokyo");
 * List<Artist> artists = metadata.getArtists();
 * List<Playlist> playlists = metadata.getPlaylists();
 * }
 * </pre>
 * 
 * Limitations:
 * - Does NOT include audio streaming URLs
 * - Does NOT include downloadable content
 * - Metadata only for cultural analysis
 * 
 * @see <a href="https://developer.spotify.com/documentation/web-api">Spotify Web API</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyMetadataDTO {
    
    /**
     * City name for this metadata
     */
    private String cityName;
    
    /**
     * List of artists associated with the city
     */
    @Builder.Default
    private List<Artist> artists = new ArrayList<>();
    
    /**
     * List of playlists related to the city
     */
    @Builder.Default
    private List<Playlist> playlists = new ArrayList<>();
    
    /**
     * Popular music genres in the city
     */
    @Builder.Default
    private List<String> genres = new ArrayList<>();
    
    /**
     * Total number of results found
     */
    private Integer totalResults;
    
    /**
     * Spotify Artist Information
     * 
     * Represents a music artist with metadata (no audio content)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        
        /**
         * Spotify artist ID
         */
        private String id;
        
        /**
         * Artist name
         */
        private String name;
        
        /**
         * Artist genres
         */
        @Builder.Default
        private List<String> genres = new ArrayList<>();
        
        /**
         * Artist popularity (0-100)
         */
        private Integer popularity;
        
        /**
         * Number of followers
         */
        private Integer followers;
        
        /**
         * External Spotify URL (web link, not audio)
         */
        @JsonProperty("external_url")
        private String externalUrl;
        
        /**
         * Artist image URLs
         */
        @Builder.Default
        private List<Image> images = new ArrayList<>();
        
        /**
         * Create Artist from Spotify API response
         * 
         * @param spotifyArtist Spotify API artist object
         * @return Artist DTO
         */
        public static Artist fromSpotifyResponse(SpotifyArtist spotifyArtist) {
            if (spotifyArtist == null) {
                return null;
            }
            
            return Artist.builder()
                    .id(spotifyArtist.getId())
                    .name(spotifyArtist.getName())
                    .genres(spotifyArtist.getGenres() != null ? spotifyArtist.getGenres() : new ArrayList<>())
                    .popularity(spotifyArtist.getPopularity())
                    .followers(spotifyArtist.getFollowers() != null ? spotifyArtist.getFollowers().getTotal() : null)
                    .externalUrl(spotifyArtist.getExternalUrls() != null ? spotifyArtist.getExternalUrls().getSpotify() : null)
                    .images(spotifyArtist.getImages() != null ? spotifyArtist.getImages() : new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Spotify Playlist Information
     * 
     * Represents a curated playlist with metadata (no audio content)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Playlist {
        
        /**
         * Spotify playlist ID
         */
        private String id;
        
        /**
         * Playlist name
         */
        private String name;
        
        /**
         * Playlist description
         */
        private String description;
        
        /**
         * Playlist owner username
         */
        private String owner;
        
        /**
         * Number of tracks in playlist
         */
        @JsonProperty("track_count")
        private Integer trackCount;
        
        /**
         * External Spotify URL (web link, not audio)
         */
        @JsonProperty("external_url")
        private String externalUrl;
        
        /**
         * Playlist cover images
         */
        @Builder.Default
        private List<Image> images = new ArrayList<>();
        
        /**
         * Create Playlist from Spotify API response
         * 
         * @param spotifyPlaylist Spotify API playlist object
         * @return Playlist DTO
         */
        public static Playlist fromSpotifyResponse(SpotifyPlaylist spotifyPlaylist) {
            if (spotifyPlaylist == null) {
                return null;
            }
            
            return Playlist.builder()
                    .id(spotifyPlaylist.getId())
                    .name(spotifyPlaylist.getName())
                    .description(spotifyPlaylist.getDescription())
                    .owner(spotifyPlaylist.getOwner() != null ? spotifyPlaylist.getOwner().getDisplayName() : null)
                    .trackCount(spotifyPlaylist.getTracks() != null ? spotifyPlaylist.getTracks().getTotal() : null)
                    .externalUrl(spotifyPlaylist.getExternalUrls() != null ? spotifyPlaylist.getExternalUrls().getSpotify() : null)
                    .images(spotifyPlaylist.getImages() != null ? spotifyPlaylist.getImages() : new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Spotify Image
     * 
     * Represents an image with dimensions
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        
        /**
         * Image URL
         */
        private String url;
        
        /**
         * Image height in pixels
         */
        private Integer height;
        
        /**
         * Image width in pixels
         */
        private Integer width;
    }
    
    // ========== Spotify API Response Classes ==========
    // These map to the actual Spotify API JSON structure
    
    /**
     * Spotify API Artist Object
     * Internal class for JSON deserialization
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpotifyArtist {
        private String id;
        private String name;
        private List<String> genres;
        private Integer popularity;
        private Followers followers;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        private List<Image> images;
    }
    
    /**
     * Spotify API Playlist Object
     * Internal class for JSON deserialization
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpotifyPlaylist {
        private String id;
        private String name;
        private String description;
        private Owner owner;
        private Tracks tracks;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        private List<Image> images;
    }
    
    /**
     * Spotify API Search Response
     * Internal class for JSON deserialization
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpotifySearchResponse {
        private ArtistsResult artists;
        private PlaylistsResult playlists;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArtistsResult {
        private List<SpotifyArtist> items;
        private Integer total;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistsResult {
        private List<SpotifyPlaylist> items;
        private Integer total;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Followers {
        private Integer total;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalUrls {
        private String spotify;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Owner {
        @JsonProperty("display_name")
        private String displayName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tracks {
        private Integer total;
    }
}

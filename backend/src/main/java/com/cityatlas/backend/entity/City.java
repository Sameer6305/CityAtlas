package com.cityatlas.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * City Entity
 * 
 * Core entity representing a city in the CityAtlas system.
 * Contains basic city information and relationships to other entities.
 * 
 * Indexes:
 * - slug: Unique index for URL-friendly lookups (primary access pattern)
 * - country: For filtering cities by country
 * - country + state: Composite index for regional queries
 * - population: For sorting/filtering by size (top cities queries)
 * - state: For state-level analytics and filtering
 */
@Entity
@Table(name = "cities", indexes = {
    @Index(name = "idx_city_slug", columnList = "slug", unique = true),
    @Index(name = "idx_city_country", columnList = "country"),
    @Index(name = "idx_city_country_state", columnList = "country, state"),
    @Index(name = "idx_city_population", columnList = "population"),
    @Index(name = "idx_city_state", columnList = "state")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"sections", "metrics", "analyticsEvents", "aiSummary"})
public class City {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * City display name (e.g., "San Francisco")
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * URL-friendly identifier (e.g., "san-francisco")
     * Must be unique across all cities
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;
    
    /**
     * State or province (optional)
     */
    @Column(length = 100)
    private String state;
    
    /**
     * Country name
     */
    @Column(nullable = false, length = 100)
    private String country;
    
    /**
     * Population count
     */
    @Column(nullable = false)
    private Long population;
    
    /**
     * GDP per capita in USD
     */
    @Column(name = "gdp_per_capita")
    private Double gdpPerCapita;
    
    /**
     * Geographic coordinates - Latitude
     */
    @Column(name = "latitude")
    private Double latitude;
    
    /**
     * Geographic coordinates - Longitude
     */
    @Column(name = "longitude")
    private Double longitude;
    
    /**
     * Cost of living index (100 = national average)
     */
    @Column(name = "cost_of_living_index")
    private Integer costOfLivingIndex;
    
    /**
     * Unemployment rate as percentage
     */
    @Column(name = "unemployment_rate")
    private Double unemploymentRate;
    
    /**
     * Banner/hero image URL
     */
    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;
    
    /**
     * Brief description or tagline
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * City sections (economy, environment, etc.)
     */
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CitySection> sections = new ArrayList<>();
    
    /**
     * City metrics (various measurements over time)
     */
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Metrics> metrics = new ArrayList<>();
    
    /**
     * Analytics events (user interactions, data updates)
     */
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnalyticsEvent> analyticsEvents = new ArrayList<>();
    
    /**
     * AI-generated summary (one per city)
     */
    @OneToOne(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    private AISummary aiSummary;
    
    /**
     * Timestamp when record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Timestamp of last data refresh from external sources
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}

package com.cityatlas.backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * City Section Entity
 * 
 * Represents different content sections for a city.
 * Each section contains structured data for a specific aspect (economy, environment, etc.)
 * 
 * Indexes:
 * - city_id + section_type: Unique composite for fast section lookup per city
 * - city_id: For retrieving all sections of a city
 * - city_id + published: For filtering published sections only (common query)
 * - section_type: For cross-city section analysis
 */
@Entity
@Table(name = "city_sections", indexes = {
    @Index(name = "idx_section_city_type", columnList = "city_id, section_type", unique = true),
    @Index(name = "idx_section_city_id", columnList = "city_id"),
    @Index(name = "idx_section_city_published", columnList = "city_id, published"),
    @Index(name = "idx_section_type", columnList = "section_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "city")
public class CitySection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Parent city
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;
    
    /**
     * Section type (ECONOMY, ENVIRONMENT, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 50)
    private SectionType sectionType;
    
    /**
     * Section title/heading
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * Section content (JSON or rich text)
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * Display order for section listing
     */
    @Column(name = "display_order")
    private Integer displayOrder;
    
    /**
     * Whether section is published/visible
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean published = true;
    
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
}

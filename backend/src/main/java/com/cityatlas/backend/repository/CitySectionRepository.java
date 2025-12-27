package com.cityatlas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.CitySection;
import com.cityatlas.backend.entity.SectionType;

/**
 * City Section Repository
 * 
 * Data access layer for CitySection entity.
 * Handles city content sections (economy, environment, etc.)
 */
@Repository
public interface CitySectionRepository extends JpaRepository<CitySection, Long> {
    
    /**
     * Find all sections for a specific city
     * 
     * @param city City entity
     * @return List of sections for that city
     */
    List<CitySection> findByCity(City city);
    
    /**
     * Find all sections for a city ordered by display order
     * 
     * @param city City entity
     * @return Ordered list of sections
     */
    List<CitySection> findByCityOrderByDisplayOrderAsc(City city);
    
    /**
     * Find specific section for a city by type
     * 
     * @param city City entity
     * @param sectionType Section type (ECONOMY, ENVIRONMENT, etc.)
     * @return Optional containing section if found
     */
    Optional<CitySection> findByCityAndSectionType(City city, SectionType sectionType);
    
    /**
     * Find all published sections for a city
     * 
     * @param city City entity
     * @param published Publication status
     * @return List of published sections
     */
    List<CitySection> findByCityAndPublished(City city, Boolean published);
    
    /**
     * Find all sections of a specific type across all cities
     * 
     * @param sectionType Section type
     * @return List of sections of that type
     */
    List<CitySection> findBySectionType(SectionType sectionType);
    
    /**
     * Count sections for a city
     * 
     * @param city City entity
     * @return Number of sections
     */
    long countByCity(City city);
    
    /**
     * Check if section exists for city and type
     * 
     * @param city City entity
     * @param sectionType Section type
     * @return true if section exists
     */
    boolean existsByCityAndSectionType(City city, SectionType sectionType);
}

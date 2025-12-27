package com.cityatlas.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.City;

/**
 * City Repository
 * 
 * Data access layer for City entity.
 * Provides CRUD operations and custom queries for city data.
 */
@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    
    /**
     * Find city by slug (URL-friendly identifier)
     * Primary lookup method for API endpoints
     * 
     * @param slug City slug (e.g., "san-francisco")
     * @return Optional containing city if found
     */
    Optional<City> findBySlug(String slug);
    
    /**
     * Check if city exists by slug
     * Useful for validation without loading full entity
     * 
     * @param slug City slug
     * @return true if city exists
     */
    boolean existsBySlug(String slug);
    
    /**
     * Find all cities in a specific country
     * 
     * @param country Country name
     * @return List of cities in that country
     */
    List<City> findByCountry(String country);
    
    /**
     * Find cities by country and state
     * 
     * @param country Country name
     * @param state State/province name
     * @return List of cities matching criteria
     */
    List<City> findByCountryAndState(String country, String state);
    
    /**
     * Find cities with population greater than specified value
     * Useful for filtering large cities
     * 
     * @param population Minimum population
     * @return List of cities above population threshold
     */
    List<City> findByPopulationGreaterThan(Long population);
    
    /**
     * Find cities within a population range
     * 
     * @param minPopulation Minimum population
     * @param maxPopulation Maximum population
     * @return List of cities in population range
     */
    List<City> findByPopulationBetween(Long minPopulation, Long maxPopulation);
    
    /**
     * Search cities by name (case-insensitive partial match)
     * 
     * @param name Search term
     * @return List of matching cities
     */
    List<City> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find cities ordered by population descending
     * Useful for "largest cities" queries
     * 
     * @return List of all cities sorted by population
     */
    List<City> findAllByOrderByPopulationDesc();
    
    /**
     * Find top N cities by population
     * 
     * @param limit Number of cities to return
     * @return Top N cities by population
     */
    @Query("SELECT c FROM City c ORDER BY c.population DESC LIMIT :limit")
    List<City> findTopCitiesByPopulation(@Param("limit") int limit);
    
    /**
     * Get total count of cities by country
     * Analytics query for country statistics
     * 
     * @param country Country name
     * @return Count of cities
     */
    long countByCountry(String country);
}

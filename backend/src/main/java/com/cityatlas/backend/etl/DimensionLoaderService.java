package com.cityatlas.backend.etl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.analytics.DimCity;
import com.cityatlas.backend.entity.analytics.DimCity.CitySizeCategory;
import com.cityatlas.backend.entity.analytics.DimCity.GdpTier;
import com.cityatlas.backend.repository.CityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * DIMENSION LOADER SERVICE - Populates Dimension Tables
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Manages the loading and maintenance of dimension tables (dim_city).
 * Implements Slowly Changing Dimension Type 2 (SCD Type 2) logic.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * SCD TYPE 2 EXPLAINED
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Slowly Changing Dimensions track historical changes to dimension attributes.
 * When an attribute changes, instead of updating the existing row, we:
 * 
 *   1. Mark the old row as expired (set valid_to = yesterday, is_current = false)
 *   2. Insert a new row with updated values (valid_from = today, is_current = true)
 * 
 *   BEFORE CHANGE:
 *   ┌────┬──────────────┬────────────┬────────────┬────────────┬──────────┐
 *   │ id │ city_slug    │ population │ valid_from │ valid_to   │ current  │
 *   ├────┼──────────────┼────────────┼────────────┼────────────┼──────────┤
 *   │ 1  │ san-fran     │ 870,000    │ 2024-01-01 │ 9999-12-31 │ true     │
 *   └────┴──────────────┴────────────┴────────────┴────────────┴──────────┘
 * 
 *   AFTER CHANGE (population updated):
 *   ┌────┬──────────────┬────────────┬────────────┬────────────┬──────────┐
 *   │ id │ city_slug    │ population │ valid_from │ valid_to   │ current  │
 *   ├────┼──────────────┼────────────┼────────────┼────────────┼──────────┤
 *   │ 1  │ san-fran     │ 870,000    │ 2024-01-01 │ 2026-01-08 │ false    │ ← expired
 *   │ 2  │ san-fran     │ 885,000    │ 2026-01-09 │ 9999-12-31 │ true     │ ← new
 *   └────┴──────────────┴────────────┴────────────┴────────────┴──────────┘
 * 
 *   This allows historical queries:
 *   - "Show me facts as of 2025-06-01" → JOINs to row with id=1
 *   - "Show me current facts" → JOINs to row with id=2 (is_current=true)
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHEN TO USE SCD TYPE 2
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │ Use SCD Type 2 when:                                                    │
 *   ├─────────────────────────────────────────────────────────────────────────┤
 *   │ ✓ Historical accuracy matters for reporting                            │
 *   │ ✓ Attributes change infrequently (yearly population updates)           │
 *   │ ✓ You need to analyze trends over time with correct historical context│
 *   │ ✓ Regulatory/compliance requires historical audit trail                │
 *   └─────────────────────────────────────────────────────────────────────────┘
 *   
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │ Use SCD Type 1 (overwrite) when:                                       │
 *   ├─────────────────────────────────────────────────────────────────────────┤
 *   │ ✓ Only current state matters (user preferences)                        │
 *   │ ✓ Changes are corrections, not true changes                            │
 *   │ ✓ Storage is limited                                                   │
 *   └─────────────────────────────────────────────────────────────────────────┘
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DimensionLoaderService {
    
    private final CityRepository cityRepository;
    
    // Using direct JDBC/EntityManager for analytics tables
    // In production, create proper repositories for DimCity, etc.
    
    /**
     * Forever date used as valid_to for current records.
     * 9999-12-31 is a common convention in data warehousing.
     */
    private static final LocalDate FOREVER_DATE = LocalDate.of(9999, 12, 31);
    
    /**
     * Population thresholds for city size classification.
     */
    private static final long POPULATION_SMALL_MAX = 100_000;
    private static final long POPULATION_MEDIUM_MAX = 1_000_000;
    private static final long POPULATION_LARGE_MAX = 10_000_000;
    
    /**
     * GDP thresholds for economic tier classification.
     */
    private static final double GDP_LOW_MAX = 20_000;
    private static final double GDP_MEDIUM_MAX = 50_000;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIAL LOAD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Perform initial load of dim_city from OLTP cities table.
     * 
     * USE CASE: First-time setup or full refresh of dimension table.
     * 
     * PROCESS:
     * 1. Read all cities from OLTP table
     * 2. Transform to DimCity format with derived attributes
     * 3. Insert with valid_from = today, valid_to = forever, is_current = true
     * 
     * @return List of DimCity entities ready for persistence
     */
    @Transactional(readOnly = true)
    public List<DimCity> buildInitialDimCityLoad() {
        log.info("[ETL-DIM] Starting initial dim_city load");
        
        List<City> allCities = cityRepository.findAll();
        log.info("[ETL-DIM] Found {} cities in OLTP table", allCities.size());
        
        LocalDate today = LocalDate.now();
        
        List<DimCity> dimCities = allCities.stream()
            .map(city -> transformToDimCity(city, today))
            .toList();
        
        log.info("[ETL-DIM] Transformed {} cities to dimension format", dimCities.size());
        return dimCities;
    }
    
    /**
     * Transform an OLTP City entity to analytics DimCity format.
     * 
     * TRANSFORMATIONS:
     * 1. Copy core attributes (name, slug, country, etc.)
     * 2. Compute derived attributes (size category, region, GDP tier)
     * 3. Set SCD Type 2 metadata (valid dates, current flag)
     */
    public DimCity transformToDimCity(City city, LocalDate validFrom) {
        return DimCity.builder()
            // Source reference
            .sourceCity(city)
            
            // Core attributes (denormalized from OLTP)
            .citySlug(city.getSlug())
            .cityName(city.getName())
            .state(city.getState())
            .country(city.getCountry())
            .population(city.getPopulation())
            .gdpPerCapita(city.getGdpPerCapita())
            .latitude(city.getLatitude())
            .longitude(city.getLongitude())
            
            // Derived attributes (computed during ETL)
            .citySizeCategory(classifyCitySize(city.getPopulation()))
            .region(mapRegion(city.getCountry(), city.getState()))
            .gdpTier(classifyGdpTier(city.getGdpPerCapita()))
            
            // SCD Type 2 metadata
            .validFrom(validFrom)
            .validTo(FOREVER_DATE)
            .isCurrent(true)
            
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INCREMENTAL LOAD (SCD Type 2)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Detect changes in OLTP cities and apply SCD Type 2 logic.
     * 
     * PROCESS:
     * 1. Compare current dim_city rows with OLTP cities
     * 2. For each changed city:
     *    a. Expire the current dim_city row
     *    b. Insert new row with updated values
     * 3. For each new city:
     *    a. Insert new row
     * 
     * @param currentDimCities Current dim_city rows (is_current = true)
     * @return ChangeSet containing updates and inserts
     */
    @Transactional(readOnly = true)
    public DimensionChangeSet detectChanges(List<DimCity> currentDimCities) {
        log.info("[ETL-DIM] Detecting dimension changes. Current dim count: {}", 
                 currentDimCities.size());
        
        List<City> oltpCities = cityRepository.findAll();
        LocalDate today = LocalDate.now();
        
        DimensionChangeSet changeSet = new DimensionChangeSet();
        
        // Build lookup map for existing dimensions
        java.util.Map<Long, DimCity> existingBySourceId = currentDimCities.stream()
            .filter(d -> d.getSourceCity() != null)
            .collect(java.util.stream.Collectors.toMap(
                d -> d.getSourceCity().getId(),
                d -> d
            ));
        
        for (City city : oltpCities) {
            DimCity existing = existingBySourceId.get(city.getId());
            
            if (existing == null) {
                // ─────────────────────────────────────────────────────────────
                // NEW CITY: Insert new dimension row
                // ─────────────────────────────────────────────────────────────
                DimCity newDim = transformToDimCity(city, today);
                changeSet.inserts.add(newDim);
                log.debug("[ETL-DIM] New city detected: {}", city.getSlug());
                
            } else if (hasSignificantChange(existing, city)) {
                // ─────────────────────────────────────────────────────────────
                // CHANGED CITY: Expire old row + insert new row
                // ─────────────────────────────────────────────────────────────
                
                // Prepare expiration of current row
                DimCity expired = existing;
                expired.setValidTo(today.minusDays(1));
                expired.setIsCurrent(false);
                changeSet.expirations.add(expired);
                
                // Prepare new version
                DimCity newVersion = transformToDimCity(city, today);
                changeSet.inserts.add(newVersion);
                
                log.debug("[ETL-DIM] City changed: {} (population {} → {})",
                         city.getSlug(), existing.getPopulation(), city.getPopulation());
            }
            // If no change, do nothing (existing row remains current)
        }
        
        log.info("[ETL-DIM] Change detection complete. New: {}, Changed: {}",
                 changeSet.inserts.size() - changeSet.expirations.size(),
                 changeSet.expirations.size());
        
        return changeSet;
    }
    
    /**
     * Check if city attributes have changed enough to warrant a new version.
     * 
     * We track these attributes for SCD Type 2:
     * - Population (5% change threshold)
     * - GDP per capita (10% change threshold)
     * - Name changes (any change)
     * 
     * Small changes are ignored to avoid dimension explosion.
     */
    private boolean hasSignificantChange(DimCity dim, City city) {
        // Name change is always significant
        if (!dim.getCityName().equals(city.getName())) {
            return true;
        }
        
        // Population change > 5% is significant
        if (dim.getPopulation() != null && city.getPopulation() != null) {
            double popChange = Math.abs(city.getPopulation() - dim.getPopulation()) 
                             / (double) dim.getPopulation();
            if (popChange > 0.05) {
                return true;
            }
        }
        
        // GDP change > 10% is significant
        if (dim.getGdpPerCapita() != null && city.getGdpPerCapita() != null) {
            double gdpChange = Math.abs(city.getGdpPerCapita() - dim.getGdpPerCapita())
                             / dim.getGdpPerCapita();
            if (gdpChange > 0.10) {
                return true;
            }
        }
        
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DERIVED ATTRIBUTE COMPUTATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Classify city by population into size categories.
     * 
     * THRESHOLDS:
     * - SMALL:  < 100,000
     * - MEDIUM: 100,000 - 1,000,000
     * - LARGE:  1,000,000 - 10,000,000
     * - MEGA:   > 10,000,000
     */
    public CitySizeCategory classifyCitySize(Long population) {
        if (population == null || population <= 0) {
            return CitySizeCategory.SMALL;
        }
        
        if (population < POPULATION_SMALL_MAX) {
            return CitySizeCategory.SMALL;
        } else if (population < POPULATION_MEDIUM_MAX) {
            return CitySizeCategory.MEDIUM;
        } else if (population < POPULATION_LARGE_MAX) {
            return CitySizeCategory.LARGE;
        } else {
            return CitySizeCategory.MEGA;
        }
    }
    
    /**
     * Map country/state to geographic region.
     * 
     * This is a simplified mapping. In production, use a reference table.
     */
    public String mapRegion(String country, String state) {
        if (country == null) {
            return "Unknown";
        }
        
        return switch (country.toLowerCase()) {
            case "united states", "usa", "canada", "mexico" -> "North America";
            case "united kingdom", "germany", "france", "italy", "spain" -> "Western Europe";
            case "poland", "czechia", "hungary", "romania" -> "Eastern Europe";
            case "china", "japan", "south korea", "taiwan" -> "East Asia";
            case "india", "pakistan", "bangladesh" -> "South Asia";
            case "australia", "new zealand" -> "Oceania";
            case "brazil", "argentina", "chile", "colombia" -> "South America";
            case "nigeria", "south africa", "egypt", "kenya" -> "Africa";
            case "uae", "saudi arabia", "israel", "turkey" -> "Middle East";
            default -> "Other";
        };
    }
    
    /**
     * Classify city by GDP per capita into economic tiers.
     * 
     * THRESHOLDS:
     * - LOW:    < $20,000
     * - MEDIUM: $20,000 - $50,000
     * - HIGH:   > $50,000
     */
    public GdpTier classifyGdpTier(Double gdpPerCapita) {
        if (gdpPerCapita == null || gdpPerCapita <= 0) {
            return GdpTier.LOW;
        }
        
        if (gdpPerCapita < GDP_LOW_MAX) {
            return GdpTier.LOW;
        } else if (gdpPerCapita < GDP_MEDIUM_MAX) {
            return GdpTier.MEDIUM;
        } else {
            return GdpTier.HIGH;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT CLASSES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Container for dimension changes detected during incremental load.
     */
    public static class DimensionChangeSet {
        public final java.util.List<DimCity> inserts = new java.util.ArrayList<>();
        public final java.util.List<DimCity> expirations = new java.util.ArrayList<>();
        
        public int totalChanges() {
            return inserts.size() + expirations.size();
        }
        
        public boolean hasChanges() {
            return !inserts.isEmpty() || !expirations.isEmpty();
        }
    }
}

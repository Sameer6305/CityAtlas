package com.cityatlas.backend.etl;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.cityatlas.backend.entity.City;
import com.cityatlas.backend.entity.MetricType;
import com.cityatlas.backend.entity.Metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 *                    DATA QUALITY DEMO
 * ============================================================================
 * 
 * Demonstrates data quality validation with sample data.
 * Enable with: etl.quality.demo=true
 * 
 * SAMPLE LOG OUTPUT:
 * 
 * [DQ-NULL] Null fields detected: city | metric_id=null
 * [DQ-RANGE] AQI value 650.00 exceeds maximum 500.00 | city=test-city
 * [DQ-FALLBACK] Using regional average for new-york:AQI = 45.0 (Tier 2)
 * [DQ-AQI] Valid AQI: 85.0 (Moderate)
 */
@Component
@ConditionalOnProperty(name = "etl.quality.demo", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class DataQualityDemo implements CommandLineRunner {
    
    private final DataQualityValidator validator;
    private final DataQualityFallback fallback;

    @Override
    public void run(String... args) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           DATA QUALITY VALIDATION DEMO                       ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        
        demoNullChecks();
        demoRangeValidation();
        demoAqiValidation();
        demoPopulationValidation();
        demoFallbackLogic();
        
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           DEMO COMPLETE                                      ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    private void demoNullChecks() {
        log.info("\n┌─────────────────────────────────────────────────────────────┐");
        log.info("│ DEMO: NULL CHECKS                                           │");
        log.info("└─────────────────────────────────────────────────────────────┘");
        
        // Test 1: Null metric
        log.info("\n→ Testing null metric record:");
        var result1 = validator.validateNotNull(null);
        log.info("  Result: passed={}, code={}", result1.passed(), result1.errorCode());
        
        // Test 2: Metric with null city
        log.info("\n→ Testing metric with null city:");
        Metrics nullCity = Metrics.builder()
            .id(1L)
            .city(null)
            .metricType(MetricType.AQI)
            .value(50.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result2 = validator.validateNotNull(nullCity);
        log.info("  Result: passed={}, code={}", result2.passed(), result2.errorCode());
        
        // Test 3: Valid metric
        log.info("\n→ Testing valid metric:");
        City testCity = City.builder().id(1L).slug("new-york").name("New York").build();
        Metrics validMetric = Metrics.builder()
            .id(2L)
            .city(testCity)
            .metricType(MetricType.AQI)
            .value(75.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result3 = validator.validateNotNull(validMetric);
        log.info("  Result: passed={}", result3.passed());
    }

    private void demoRangeValidation() {
        log.info("\n┌─────────────────────────────────────────────────────────────┐");
        log.info("│ DEMO: RANGE VALIDATION                                      │");
        log.info("└─────────────────────────────────────────────────────────────┘");
        
        City testCity = City.builder().id(1L).slug("test-city").name("Test City").build();
        
        // Test 1: AQI above max (500)
        log.info("\n→ Testing AQI above maximum (650 > 500):");
        Metrics highAqi = Metrics.builder()
            .id(10L)
            .city(testCity)
            .metricType(MetricType.AQI)
            .value(650.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result1 = validator.validateRange(highAqi);
        log.info("  Result: passed={}, code={}", result1.passed(), result1.errorCode());
        
        // Test 2: Negative population
        log.info("\n→ Testing negative population (-1000):");
        Metrics negPop = Metrics.builder()
            .id(11L)
            .city(testCity)
            .metricType(MetricType.POPULATION)
            .value(-1000.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result2 = validator.validateRange(negPop);
        log.info("  Result: passed={}, code={}", result2.passed(), result2.errorCode());
        
        // Test 3: Valid GDP
        log.info("\n→ Testing valid GDP per capita (55000):");
        Metrics validGdp = Metrics.builder()
            .id(12L)
            .city(testCity)
            .metricType(MetricType.GDP_PER_CAPITA)
            .value(55000.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result3 = validator.validateRange(validGdp);
        log.info("  Result: passed={}", result3.passed());
        
        // Test 4: Auto-clamp demonstration
        log.info("\n→ Testing auto-clamp for out-of-range unemployment (150%):");
        Metrics highUnemp = Metrics.builder()
            .id(13L)
            .city(testCity)
            .metricType(MetricType.UNEMPLOYMENT_RATE)
            .value(150.0)
            .recordedAt(LocalDateTime.now())
            .build();
        var result4 = validator.validateRangeWithClamp(highUnemp, true);
        log.info("  Result: passed={}, new value={}", result4.passed(), highUnemp.getValue());
    }

    private void demoAqiValidation() {
        log.info("\n┌─────────────────────────────────────────────────────────────┐");
        log.info("│ DEMO: AQI SPECIFIC VALIDATION                               │");
        log.info("└─────────────────────────────────────────────────────────────┘");
        
        // Test AQI categories
        double[] testValues = {25.0, 75.0, 125.0, 175.0, 275.0, 400.0, 550.0, -10.0};
        
        for (double aqi : testValues) {
            log.info("\n→ Testing AQI value: {}", aqi);
            var result = validator.validateAqi(aqi);
            log.info("  Result: passed={}, message={}", result.passed(), 
                    result.message() != null ? result.message() : result.errorCode());
        }
        
        // Test null AQI
        log.info("\n→ Testing null AQI:");
        var nullResult = validator.validateAqi(null);
        log.info("  Result: passed={}, code={}", nullResult.passed(), nullResult.errorCode());
    }

    private void demoPopulationValidation() {
        log.info("\n┌─────────────────────────────────────────────────────────────┐");
        log.info("│ DEMO: POPULATION SPECIFIC VALIDATION                        │");
        log.info("└─────────────────────────────────────────────────────────────┘");
        
        long[] testValues = {0L, 500_000L, 8_500_000L, 50_000_000L, 60_000_000L, -100L};
        
        for (long pop : testValues) {
            log.info("\n→ Testing population: {}", String.format("%,d", pop));
            var result = validator.validatePopulation(pop);
            log.info("  Result: passed={}, message={}", result.passed(),
                    result.message() != null ? result.message() : result.errorCode());
        }
    }

    private void demoFallbackLogic() {
        log.info("\n┌─────────────────────────────────────────────────────────────┐");
        log.info("│ DEMO: FALLBACK LOGIC                                        │");
        log.info("└─────────────────────────────────────────────────────────────┘");
        
        // Cache a value
        log.info("\n→ Caching AQI value for new-york: 42.0");
        fallback.cacheValue("new-york", MetricType.AQI, 42.0);
        
        // Test Tier 1: Cache hit
        log.info("\n→ Testing fallback for new-york:AQI (should hit cache):");
        var result1 = fallback.resolveFallback("new-york", "US", MetricType.AQI);
        log.info("  Result: value={}, tier={}", result1.value(), result1.tier());
        
        // Test Tier 2: Regional average (no cache)
        log.info("\n→ Testing fallback for boston:GDP_PER_CAPITA (no cache, has regional):");
        var result2 = fallback.resolveFallback("boston", "US", MetricType.GDP_PER_CAPITA);
        log.info("  Result: value={}, tier={}", result2.value(), result2.tier());
        
        // Test Tier 3: Global default
        log.info("\n→ Testing fallback for mumbai:COST_OF_LIVING (no cache, no regional):");
        var result3 = fallback.resolveFallback("mumbai", "XX", MetricType.COST_OF_LIVING);
        log.info("  Result: value={}, tier={}", result3.value(), result3.tier());
        
        // Test No fallback available (population)
        log.info("\n→ Testing fallback for unknown-city:POPULATION (no fallback available):");
        var result4 = fallback.resolveFallback("unknown-city", "XX", MetricType.POPULATION);
        log.info("  Result: value={}, tier={}", result4.value(), result4.tier());
        
        // Test getValueWithFallback
        log.info("\n→ Testing getValueWithFallback with null value:");
        var result5 = fallback.getValueWithFallback(null, "chicago", "US", MetricType.UNEMPLOYMENT_RATE);
        log.info("  Result: value={}, usedFallback={}, tier={}", 
                result5.value(), result5.usedFallback(), result5.fallbackTier());
        
        // Test getValueWithFallback with valid value
        log.info("\n→ Testing getValueWithFallback with valid value (3.5):");
        var result6 = fallback.getValueWithFallback(3.5, "chicago", "US", MetricType.UNEMPLOYMENT_RATE);
        log.info("  Result: value={}, usedFallback={}", result6.value(), result6.usedFallback());
        
        // Show cache stats
        log.info("\n→ Cache statistics:");
        var stats = fallback.getCacheStats();
        log.info("  Total entries: {}", stats.totalEntries());
        log.info("  Entries by type: {}", stats.entriesByType());
    }
}

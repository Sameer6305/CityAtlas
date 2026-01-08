package com.cityatlas.backend.etl;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cityatlas.backend.entity.MetricType;
import com.cityatlas.backend.entity.Metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * METRIC NORMALIZATION SERVICE - Second Stage of ETL Pipeline
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * Normalizes metric values to consistent scales for cross-city comparison.
 * Different metrics use different scales (AQI: 0-500, Population: millions),
 * so normalization is essential for meaningful analytics.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY NORMALIZATION IS REQUIRED
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 *   PROBLEM: Raw metrics have incompatible scales
 *   ─────────────────────────────────────────────
 *   
 *   Metric Type         │ NYC Value    │ LA Value     │ Which is "better"?
 *   ────────────────────┼──────────────┼──────────────┼────────────────────
 *   AQI                 │ 45           │ 78           │ Lower is better
 *   Population          │ 8,300,000    │ 3,900,000    │ Depends on context
 *   GDP per Capita      │ $85,000      │ $72,000      │ Higher is better
 *   Cost of Living      │ 187          │ 166          │ Lower is better
 *   
 *   Without normalization, averaging these would be meaningless!
 * 
 *   SOLUTION: Normalize to 0-100 scale with consistent direction
 *   ───────────────────────────────────────────────────────────────
 *   
 *   Metric Type         │ NYC (0-100)  │ LA (0-100)   │ Interpretation
 *   ────────────────────┼──────────────┼──────────────┼────────────────────
 *   AQI Score           │ 91           │ 69           │ Higher = better air
 *   Population Rank     │ 95           │ 78           │ Higher = more people
 *   GDP Score           │ 88           │ 75           │ Higher = wealthier
 *   Cost Score          │ 23           │ 45           │ Higher = more affordable
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * NORMALIZATION METHODS
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 1. MIN-MAX SCALING (0-100)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Formula: normalized = (value - min) / (max - min) * 100
 *    
 *    Use when: You want to preserve relative differences
 *    Example: GDP per capita → 0-100 score
 * 
 * 2. PERCENTILE RANKING (0.0-1.0)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Formula: rank = (position in sorted list) / (total count)
 *    
 *    Use when: You want to show relative standing among peers
 *    Example: "Top 10% in air quality" → 0.90 percentile
 * 
 * 3. Z-SCORE STANDARDIZATION
 *    ─────────────────────────────────────────────────────────────────────────
 *    Formula: z = (value - mean) / stdDev
 *    
 *    Use when: You want to identify outliers and deviations
 *    Example: "2.5 standard deviations above average"
 * 
 * 4. INVERSE SCALING (for "lower is better" metrics)
 *    ─────────────────────────────────────────────────────────────────────────
 *    Formula: normalized = 100 - minMaxScale(value)
 *    
 *    Use when: Lower raw values are better (AQI, cost)
 *    Ensures higher normalized scores always mean "better"
 */
@Service
@Slf4j
public class MetricNormalizationService {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // METRIC CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Configuration for each metric type.
     * Defines normalization method, direction, and known bounds.
     */
    private static final Map<MetricType, MetricConfig> METRIC_CONFIGS = Map.ofEntries(
        // Environmental Metrics (lower is better for pollution)
        Map.entry(MetricType.AQI, new MetricConfig(0, 500, true, NormMethod.MIN_MAX)),
        Map.entry(MetricType.CARBON_EMISSIONS, new MetricConfig(0, 50, true, NormMethod.MIN_MAX)),
        Map.entry(MetricType.WATER_QUALITY, new MetricConfig(0, 100, false, NormMethod.MIN_MAX)),
        
        // Economic Metrics
        Map.entry(MetricType.UNEMPLOYMENT_RATE, new MetricConfig(0, 25, true, NormMethod.MIN_MAX)),
        Map.entry(MetricType.GDP_PER_CAPITA, new MetricConfig(10000, 150000, false, NormMethod.PERCENTILE)),
        Map.entry(MetricType.COST_OF_LIVING, new MetricConfig(50, 300, true, NormMethod.MIN_MAX)),
        Map.entry(MetricType.AVERAGE_SALARY, new MetricConfig(20000, 200000, false, NormMethod.PERCENTILE)),
        
        // Demographic Metrics
        Map.entry(MetricType.POPULATION, new MetricConfig(10000, 40000000, false, NormMethod.PERCENTILE)),
        Map.entry(MetricType.POPULATION_GROWTH, new MetricConfig(-5, 10, false, NormMethod.MIN_MAX)),
        Map.entry(MetricType.MEDIAN_AGE, new MetricConfig(20, 50, false, NormMethod.MIN_MAX)),
        
        // Infrastructure Metrics
        Map.entry(MetricType.TRANSIT_COVERAGE, new MetricConfig(0, 100, false, NormMethod.MIN_MAX)),
        Map.entry(MetricType.INTERNET_SPEED, new MetricConfig(10, 1000, false, NormMethod.PERCENTILE)),
        Map.entry(MetricType.HOUSING_AFFORDABILITY, new MetricConfig(0, 10, true, NormMethod.MIN_MAX))
    );
    
    /**
     * Default configuration for unknown metric types.
     */
    private static final MetricConfig DEFAULT_CONFIG = 
        new MetricConfig(0, 100, false, NormMethod.MIN_MAX);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BATCH NORMALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Normalize a batch of metrics for analytics loading.
     * 
     * PROCESS:
     * 1. Group metrics by type
     * 2. Calculate statistics for each type
     * 3. Apply appropriate normalization method
     * 4. Return normalized values with metadata
     * 
     * @param cleanMetrics Cleaned metrics from DataCleaningService
     * @return List of normalized metric results
     */
    public List<NormalizedMetric> normalizeMetrics(List<Metrics> cleanMetrics) {
        log.info("[ETL-NORM] Starting normalization. Input count: {}", cleanMetrics.size());
        
        List<NormalizedMetric> results = new ArrayList<>();
        
        // ─────────────────────────────────────────────────────────────────────
        // STEP 1: Group by metric type for type-specific normalization
        // ─────────────────────────────────────────────────────────────────────
        // Each metric type has its own scale and interpretation
        
        Map<MetricType, List<Metrics>> byType = cleanMetrics.stream()
            .collect(Collectors.groupingBy(Metrics::getMetricType));
        
        for (Map.Entry<MetricType, List<Metrics>> entry : byType.entrySet()) {
            MetricType type = entry.getKey();
            List<Metrics> typeMetrics = entry.getValue();
            MetricConfig config = METRIC_CONFIGS.getOrDefault(type, DEFAULT_CONFIG);
            
            log.debug("[ETL-NORM] Normalizing {} {} metrics using {} method",
                     typeMetrics.size(), type, config.method);
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 2: Calculate statistics for this metric type
            // ─────────────────────────────────────────────────────────────────
            
            DoubleSummaryStatistics stats = typeMetrics.stream()
                .mapToDouble(Metrics::getValue)
                .summaryStatistics();
            
            // Sort for percentile calculation
            List<Double> sortedValues = typeMetrics.stream()
                .map(Metrics::getValue)
                .sorted()
                .collect(Collectors.toList());
            
            // ─────────────────────────────────────────────────────────────────
            // STEP 3: Apply normalization based on config
            // ─────────────────────────────────────────────────────────────────
            
            for (Metrics m : typeMetrics) {
                double normalizedValue;
                double percentileRank;
                
                switch (config.method) {
                    case MIN_MAX:
                        normalizedValue = minMaxNormalize(
                            m.getValue(), 
                            config.minBound, 
                            config.maxBound,
                            config.inverse
                        );
                        percentileRank = calculatePercentile(m.getValue(), sortedValues);
                        break;
                        
                    case PERCENTILE:
                        percentileRank = calculatePercentile(m.getValue(), sortedValues);
                        normalizedValue = config.inverse 
                            ? (1 - percentileRank) * 100 
                            : percentileRank * 100;
                        break;
                        
                    case Z_SCORE:
                        double mean = stats.getAverage();
                        double stdDev = calculateStdDev(typeMetrics);
                        double zScore = stdDev > 0 ? (m.getValue() - mean) / stdDev : 0;
                        // Convert z-score to 0-100 scale (assuming -3 to +3 range)
                        normalizedValue = Math.max(0, Math.min(100, (zScore + 3) / 6 * 100));
                        if (config.inverse) {
                            normalizedValue = 100 - normalizedValue;
                        }
                        percentileRank = calculatePercentile(m.getValue(), sortedValues);
                        break;
                        
                    default:
                        normalizedValue = m.getValue();
                        percentileRank = 0.5;
                }
                
                results.add(new NormalizedMetric(
                    m,
                    normalizedValue,
                    percentileRank,
                    config.method.name()
                ));
            }
        }
        
        log.info("[ETL-NORM] Normalization complete. Output count: {}", results.size());
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NORMALIZATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Min-Max normalization to 0-100 scale.
     * 
     * FORMULA:
     *   normalized = (value - min) / (max - min) * 100
     * 
     * If inverse=true:
     *   normalized = 100 - ((value - min) / (max - min) * 100)
     * 
     * @param value Raw value to normalize
     * @param min Known minimum for this metric type
     * @param max Known maximum for this metric type
     * @param inverse If true, flip so higher normalized = lower raw value
     * @return Normalized value between 0 and 100
     */
    private double minMaxNormalize(double value, double min, double max, boolean inverse) {
        // Clamp value to known bounds to avoid >100 or <0
        double clamped = Math.max(min, Math.min(max, value));
        
        // Avoid division by zero
        if (max == min) {
            return 50.0; // Neutral if no range
        }
        
        double normalized = (clamped - min) / (max - min) * 100;
        
        return inverse ? 100 - normalized : normalized;
    }
    
    /**
     * Calculate percentile rank of a value in a sorted list.
     * 
     * FORMULA:
     *   percentile = (number of values below) / (total count - 1)
     * 
     * Result is 0.0 to 1.0:
     *   0.0 = lowest value
     *   0.5 = median
     *   1.0 = highest value
     * 
     * @param value Value to rank
     * @param sortedValues Sorted list of all values
     * @return Percentile rank between 0.0 and 1.0
     */
    private double calculatePercentile(double value, List<Double> sortedValues) {
        if (sortedValues.size() <= 1) {
            return 0.5; // Neutral if only one value
        }
        
        // Count values strictly less than this value
        long countBelow = sortedValues.stream()
            .filter(v -> v < value)
            .count();
        
        return (double) countBelow / (sortedValues.size() - 1);
    }
    
    /**
     * Calculate standard deviation for Z-score normalization.
     */
    private double calculateStdDev(List<Metrics> metrics) {
        double mean = metrics.stream()
            .mapToDouble(Metrics::getValue)
            .average()
            .orElse(0.0);
        
        double variance = metrics.stream()
            .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLE VALUE NORMALIZATION (for real-time use)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Normalize a single metric value without batch context.
     * Uses configured bounds for the metric type.
     * 
     * @param type Metric type
     * @param value Raw value
     * @return Normalized value between 0 and 100
     */
    public double normalizeSingleValue(MetricType type, double value) {
        MetricConfig config = METRIC_CONFIGS.getOrDefault(type, DEFAULT_CONFIG);
        return minMaxNormalize(value, config.minBound, config.maxBound, config.inverse);
    }
    
    /**
     * Get the configured bounds for a metric type.
     * Useful for UI display (showing scale context).
     */
    public MetricBounds getMetricBounds(MetricType type) {
        MetricConfig config = METRIC_CONFIGS.getOrDefault(type, DEFAULT_CONFIG);
        return new MetricBounds(config.minBound, config.maxBound, config.inverse);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CLASSES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Normalization method enum.
     */
    public enum NormMethod {
        MIN_MAX,      // Scale to 0-100 using known bounds
        PERCENTILE,   // Rank-based normalization
        Z_SCORE       // Standard deviation based
    }
    
    /**
     * Configuration for a metric type.
     * 
     * @param minBound Theoretical/practical minimum value
     * @param maxBound Theoretical/practical maximum value
     * @param inverse If true, lower raw values = higher normalized scores
     * @param method Normalization method to use
     */
    private record MetricConfig(
        double minBound,
        double maxBound,
        boolean inverse,
        NormMethod method
    ) {}
    
    /**
     * Result of normalizing a single metric.
     */
    public record NormalizedMetric(
        Metrics originalMetric,
        double normalizedValue,
        double percentileRank,
        String normalizationMethod
    ) {
        public Long getCityId() {
            return originalMetric.getCity().getId();
        }
        
        public MetricType getMetricType() {
            return originalMetric.getMetricType();
        }
    }
    
    /**
     * Metric bounds for UI display.
     */
    public record MetricBounds(
        double min,
        double max,
        boolean lowerIsBetter
    ) {}
}

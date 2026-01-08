package com.cityatlas.backend.etl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cityatlas.backend.entity.MetricType;
import com.cityatlas.backend.entity.Metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 *                    DATA QUALITY VALIDATOR
 * ============================================================================
 * 
 * Lightweight data quality validation for CityAtlas metrics.
 * No heavy frameworks - just readable, maintainable checks.
 * 
 * VALIDATION RULES:
 * +-----------------+------------------+------------------+------------------+
 * | Metric Type     | Min Value        | Max Value        | Notes            |
 * +-----------------+------------------+------------------+------------------+
 * | AQI             | 0                | 500              | 0=Good, 500=Haz  |
 * | POPULATION      | 0                | 50,000,000       | 50M max city     |
 * | GDP_PER_CAPITA  | 100              | 500,000          | USD              |
 * | UNEMPLOYMENT    | 0                | 100              | Percentage       |
 * | COST_OF_LIVING  | 20               | 300              | Index (100=avg)  |
 * +-----------------+------------------+------------------+------------------+
 * 
 * @author CityAtlas ETL Team
 */
@Component
@Slf4j
public class DataQualityValidator {

    // ========================================================================
    //                    VALIDATION BOUNDS BY METRIC TYPE
    // ========================================================================
    
    private static final Map<MetricType, ValidationBounds> BOUNDS = new HashMap<>();
    
    static {
        // Environmental
        BOUNDS.put(MetricType.AQI, new ValidationBounds(0, 500, "AQI (0=Good, 500=Hazardous)"));
        BOUNDS.put(MetricType.CARBON_EMISSIONS, new ValidationBounds(0, 100, "Tons CO2 per capita"));
        BOUNDS.put(MetricType.WATER_QUALITY, new ValidationBounds(0, 100, "Water Quality Index"));
        
        // Economic
        BOUNDS.put(MetricType.UNEMPLOYMENT_RATE, new ValidationBounds(0, 100, "Unemployment %"));
        BOUNDS.put(MetricType.GDP_PER_CAPITA, new ValidationBounds(100, 500_000, "GDP in USD"));
        BOUNDS.put(MetricType.COST_OF_LIVING, new ValidationBounds(20, 300, "Index (100=national avg)"));
        BOUNDS.put(MetricType.AVERAGE_SALARY, new ValidationBounds(1000, 500_000, "Annual salary USD"));
        
        // Demographic
        BOUNDS.put(MetricType.POPULATION, new ValidationBounds(0, 50_000_000, "City population"));
        BOUNDS.put(MetricType.POPULATION_GROWTH, new ValidationBounds(-20, 50, "Growth rate %"));
        BOUNDS.put(MetricType.MEDIAN_AGE, new ValidationBounds(10, 70, "Years"));
        
        // Infrastructure
        BOUNDS.put(MetricType.TRANSIT_COVERAGE, new ValidationBounds(0, 100, "Coverage %"));
        BOUNDS.put(MetricType.INTERNET_SPEED, new ValidationBounds(0, 10_000, "Mbps"));
        BOUNDS.put(MetricType.HOUSING_AFFORDABILITY, new ValidationBounds(0, 500, "Affordability Index"));
    }

    // ========================================================================
    //                    NULL CHECKS
    // ========================================================================
    
    /**
     * Validate that all required fields are non-null.
     * 
     * @param metric The metric to validate
     * @return ValidationResult with pass/fail and detailed message
     */
    public ValidationResult validateNotNull(Metrics metric) {
        List<String> nullFields = new ArrayList<>();
        
        if (metric == null) {
            return ValidationResult.fail("NULL_RECORD", "Entire metric record is null");
        }
        
        if (metric.getCity() == null) {
            nullFields.add("city");
        }
        if (metric.getMetricType() == null) {
            nullFields.add("metricType");
        }
        if (metric.getValue() == null) {
            nullFields.add("value");
        }
        if (metric.getRecordedAt() == null) {
            nullFields.add("recordedAt");
        }
        
        if (!nullFields.isEmpty()) {
            String message = "Null fields detected: " + String.join(", ", nullFields);
            log.warn("[DQ-NULL] {} | metric_id={}", message, metric.getId());
            return ValidationResult.fail("NULL_FIELD", message);
        }
        
        return ValidationResult.pass();
    }
    
    /**
     * Batch null validation with summary logging.
     */
    public BatchValidationResult validateNotNullBatch(List<Metrics> metrics) {
        List<Metrics> valid = new ArrayList<>();
        List<FailedRecord> failed = new ArrayList<>();
        
        for (Metrics m : metrics) {
            ValidationResult result = validateNotNull(m);
            if (result.passed()) {
                valid.add(m);
            } else {
                failed.add(new FailedRecord(m, result.errorCode(), result.message()));
            }
        }
        
        if (!failed.isEmpty()) {
            log.warn("[DQ-NULL] Batch null check: {} passed, {} failed", 
                    valid.size(), failed.size());
        }
        
        return new BatchValidationResult(valid, failed);
    }

    // ========================================================================
    //                    RANGE VALIDATION
    // ========================================================================
    
    /**
     * Validate that metric value is within expected range for its type.
     * 
     * @param metric The metric to validate
     * @return ValidationResult with pass/fail and range info
     */
    public ValidationResult validateRange(Metrics metric) {
        if (metric == null || metric.getMetricType() == null || metric.getValue() == null) {
            return ValidationResult.fail("INVALID_INPUT", "Cannot validate range on null metric/type/value");
        }
        
        MetricType type = metric.getMetricType();
        Double value = metric.getValue();
        
        ValidationBounds bounds = BOUNDS.get(type);
        if (bounds == null) {
            // Unknown metric type - log but allow through
            log.debug("[DQ-RANGE] No bounds defined for metric type: {}", type);
            return ValidationResult.pass();
        }
        
        if (value < bounds.min()) {
            String message = String.format(
                "%s value %.2f is below minimum %.2f (%s)",
                type, value, bounds.min(), bounds.description()
            );
            log.warn("[DQ-RANGE] {} | city={}", message, 
                    metric.getCity() != null ? metric.getCity().getSlug() : "unknown");
            return ValidationResult.fail("BELOW_MIN", message);
        }
        
        if (value > bounds.max()) {
            String message = String.format(
                "%s value %.2f exceeds maximum %.2f (%s)",
                type, value, bounds.max(), bounds.description()
            );
            log.warn("[DQ-RANGE] {} | city={}", message,
                    metric.getCity() != null ? metric.getCity().getSlug() : "unknown");
            return ValidationResult.fail("ABOVE_MAX", message);
        }
        
        return ValidationResult.pass();
    }
    
    /**
     * Validate range with auto-clamping option.
     * 
     * @param metric The metric to validate
     * @param autoClamp If true, clamp out-of-range values to bounds
     * @return ValidationResult (always passes if autoClamp is true)
     */
    public ValidationResult validateRangeWithClamp(Metrics metric, boolean autoClamp) {
        ValidationResult result = validateRange(metric);
        
        if (!result.passed() && autoClamp) {
            MetricType type = metric.getMetricType();
            ValidationBounds bounds = BOUNDS.get(type);
            
            if (bounds != null) {
                double original = metric.getValue();
                double clamped = Math.max(bounds.min(), Math.min(bounds.max(), original));
                metric.setValue(clamped);
                
                log.info("[DQ-RANGE] Auto-clamped {} from {} to {} | city={}",
                        type, original, clamped,
                        metric.getCity() != null ? metric.getCity().getSlug() : "unknown");
                
                return ValidationResult.pass("Value clamped from " + original + " to " + clamped);
            }
        }
        
        return result;
    }
    
    /**
     * Batch range validation with summary statistics.
     */
    public BatchValidationResult validateRangeBatch(List<Metrics> metrics) {
        List<Metrics> valid = new ArrayList<>();
        List<FailedRecord> failed = new ArrayList<>();
        Map<MetricType, Integer> failuresByType = new HashMap<>();
        
        for (Metrics m : metrics) {
            ValidationResult result = validateRange(m);
            if (result.passed()) {
                valid.add(m);
            } else {
                failed.add(new FailedRecord(m, result.errorCode(), result.message()));
                failuresByType.merge(m.getMetricType(), 1, Integer::sum);
            }
        }
        
        if (!failed.isEmpty()) {
            log.warn("[DQ-RANGE] Batch range check: {} passed, {} failed | breakdown: {}",
                    valid.size(), failed.size(), failuresByType);
        }
        
        return new BatchValidationResult(valid, failed);
    }

    // ========================================================================
    //                    COMBINED VALIDATION
    // ========================================================================
    
    /**
     * Run all validation checks on a single metric.
     * 
     * @param metric The metric to validate
     * @return List of all validation results (may contain multiple failures)
     */
    public List<ValidationResult> validateAll(Metrics metric) {
        List<ValidationResult> results = new ArrayList<>();
        
        // Null check first
        ValidationResult nullCheck = validateNotNull(metric);
        results.add(nullCheck);
        
        if (nullCheck.passed()) {
            // Range check only if nulls passed
            results.add(validateRange(metric));
            
            // Staleness check
            results.add(validateFreshness(metric));
        }
        
        return results;
    }
    
    /**
     * Validate data freshness (not stale).
     */
    public ValidationResult validateFreshness(Metrics metric) {
        if (metric == null || metric.getRecordedAt() == null) {
            return ValidationResult.fail("NULL_TIMESTAMP", "Cannot check freshness without timestamp");
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recorded = metric.getRecordedAt();
        long hoursOld = java.time.Duration.between(recorded, now).toHours();
        
        if (hoursOld > 168) { // > 1 week
            log.warn("[DQ-STALE] Metric is {} hours old (> 1 week) | type={}, city={}",
                    hoursOld, metric.getMetricType(),
                    metric.getCity() != null ? metric.getCity().getSlug() : "unknown");
            return ValidationResult.fail("STALE_DATA", "Data is " + hoursOld + " hours old");
        }
        
        if (hoursOld > 24) { // > 1 day
            log.debug("[DQ-STALE] Metric is {} hours old (> 24h) | type={}", 
                    hoursOld, metric.getMetricType());
            return ValidationResult.pass("Data is " + hoursOld + " hours old (acceptable)");
        }
        
        return ValidationResult.pass();
    }

    // ========================================================================
    //                    SPECIAL VALIDATORS
    // ========================================================================
    
    /**
     * Validate AQI specifically with detailed category logging.
     */
    public ValidationResult validateAqi(Double aqi) {
        if (aqi == null) {
            log.warn("[DQ-AQI] Null AQI value received");
            return ValidationResult.fail("NULL_AQI", "AQI value is null");
        }
        
        if (aqi < 0) {
            log.warn("[DQ-AQI] Negative AQI: {}", aqi);
            return ValidationResult.fail("NEGATIVE_AQI", "AQI cannot be negative: " + aqi);
        }
        
        if (aqi > 500) {
            log.warn("[DQ-AQI] AQI exceeds scale maximum (500): {}", aqi);
            return ValidationResult.fail("AQI_OVERFLOW", "AQI exceeds 500 scale: " + aqi);
        }
        
        // Log AQI category for debugging
        String category = getAqiCategory(aqi.intValue());
        log.debug("[DQ-AQI] Valid AQI: {} ({})", aqi, category);
        
        return ValidationResult.pass("AQI " + aqi + " (" + category + ")");
    }
    
    private String getAqiCategory(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }
    
    /**
     * Validate population specifically.
     */
    public ValidationResult validatePopulation(Long population) {
        if (population == null) {
            log.warn("[DQ-POP] Null population value received");
            return ValidationResult.fail("NULL_POPULATION", "Population value is null");
        }
        
        if (population < 0) {
            log.warn("[DQ-POP] Negative population: {}", population);
            return ValidationResult.fail("NEGATIVE_POPULATION", "Population cannot be negative");
        }
        
        if (population > 50_000_000) {
            log.warn("[DQ-POP] Population exceeds reasonable city max (50M): {}", population);
            return ValidationResult.fail("POPULATION_OVERFLOW", 
                    "Population " + population + " exceeds city maximum");
        }
        
        if (population == 0) {
            log.debug("[DQ-POP] Zero population - may be placeholder data");
            return ValidationResult.pass("Population is zero (may need fallback)");
        }
        
        return ValidationResult.pass();
    }

    // ========================================================================
    //                    RESULT TYPES
    // ========================================================================
    
    /**
     * Single validation result.
     */
    public record ValidationResult(
        boolean passed,
        String errorCode,
        String message
    ) {
        public static ValidationResult pass() {
            return new ValidationResult(true, null, null);
        }
        
        public static ValidationResult pass(String info) {
            return new ValidationResult(true, null, info);
        }
        
        public static ValidationResult fail(String code, String message) {
            return new ValidationResult(false, code, message);
        }
    }
    
    /**
     * Batch validation result.
     */
    public record BatchValidationResult(
        List<Metrics> validRecords,
        List<FailedRecord> failedRecords
    ) {
        public int validCount() { return validRecords.size(); }
        public int failedCount() { return failedRecords.size(); }
        public double passRate() {
            int total = validCount() + failedCount();
            return total > 0 ? (double) validCount() / total * 100 : 0;
        }
    }
    
    /**
     * Failed record with reason.
     */
    public record FailedRecord(
        Metrics metric,
        String errorCode,
        String message
    ) {}
    
    /**
     * Validation bounds for a metric type.
     */
    public record ValidationBounds(
        double min,
        double max,
        String description
    ) {}
}

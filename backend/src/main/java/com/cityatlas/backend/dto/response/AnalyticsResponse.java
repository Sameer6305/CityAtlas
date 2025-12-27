package com.cityatlas.backend.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics Response DTO
 * 
 * Contains comprehensive analytics data for a city including:
 * - Environmental metrics (AQI)
 * - Economic indicators (employment, cost of living)
 * - Demographics (population trends)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    
    /**
     * City identifier
     */
    private String citySlug;
    
    /**
     * City name for display
     */
    private String cityName;
    
    /**
     * Air Quality Index (AQI) trend data
     */
    private List<AQIDataPoint> aqiTrend;
    
    /**
     * Employment distribution by sector
     */
    private List<JobSectorData> jobSectors;
    
    /**
     * Cost of living breakdown by category
     */
    private List<CostOfLivingData> costOfLiving;
    
    /**
     * Population growth historical data
     */
    private List<PopulationDataPoint> populationTrend;
    
    // ============================================
    // Nested DTOs for Chart Data
    // ============================================
    
    /**
     * AQI Data Point for environmental quality tracking
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AQIDataPoint {
        private String month;
        private Integer aqi;
        private String category; // Good, Moderate, Unhealthy, Hazardous
    }
    
    /**
     * Job Sector Distribution Data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobSectorData {
        private String sector;
        private Integer employees;
        private Double percentage;
        private Double growthRate;
    }
    
    /**
     * Cost of Living Index by Category
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostOfLivingData {
        private String category;
        private Integer index;
        private Integer nationalAverage;
        private Integer monthlyAvg;
    }
    
    /**
     * Population Data Point for demographic trends
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopulationDataPoint {
        private String year;
        private Double population; // in millions
        private Double growthRate;
    }
}

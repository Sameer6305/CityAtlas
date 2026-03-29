package com.cityatlas.backend.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cityatlas.backend.entity.MetricType;
import com.cityatlas.backend.entity.analytics.DimCity;
import com.cityatlas.backend.entity.analytics.FactCityMetrics;

@Repository
public interface FactCityMetricsRepository extends JpaRepository<FactCityMetrics, Long> {

    boolean existsByDimCityAndMetricTypeAndMetricDate(
            DimCity dimCity,
            MetricType metricType,
            LocalDate metricDate);
}

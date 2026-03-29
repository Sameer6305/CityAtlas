package com.cityatlas.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.etl.DataQualityFallback;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/observability/data-quality")
@RequiredArgsConstructor
public class DataQualityObservabilityController {

    private final DataQualityFallback dataQualityFallback;

    @GetMapping("/fallback-usage")
    public ResponseEntity<DataQualityFallback.FallbackUsageStats> getFallbackUsage() {
        return ResponseEntity.ok(dataQualityFallback.getFallbackUsageStats());
    }

    @GetMapping("/source-reliability")
    public ResponseEntity<DataQualityFallback.DataSourceReliabilityStats> getSourceReliability() {
        return ResponseEntity.ok(dataQualityFallback.getDataSourceReliabilityStats());
    }
}

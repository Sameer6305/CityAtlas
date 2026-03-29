package com.cityatlas.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.service.CityFeatureStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/observability/feature-store")
@RequiredArgsConstructor
public class FeatureStoreObservabilityController {

    private final CityFeatureStore cityFeatureStore;

    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(cityFeatureStore.getCacheStats());
    }

    @GetMapping("/computation-stats")
    public ResponseEntity<Map<String, Object>> getComputationStats() {
        return ResponseEntity.ok(cityFeatureStore.getComputationStats());
    }
}

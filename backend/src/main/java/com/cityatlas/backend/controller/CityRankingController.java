package com.cityatlas.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.service.CityFeatureStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rankings/cities")
@RequiredArgsConstructor
public class CityRankingController {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final CityFeatureStore cityFeatureStore;

    @GetMapping("/overall")
    public ResponseEntity<Map<String, Object>> getTopOverallCities(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Map<String, Object>> rankings = cityFeatureStore.getTopOverallRankingView(safeLimit);

        return ResponseEntity.ok(Map.of(
                "rankingType", "overall",
                "limit", safeLimit,
                "count", rankings.size(),
                "items", rankings
        ));
    }

    @GetMapping("/economy")
    public ResponseEntity<Map<String, Object>> getTopEconomyCities(
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Map<String, Object>> rankings = cityFeatureStore.getTopEconomyRankingView(safeLimit);

        return ResponseEntity.ok(Map.of(
                "rankingType", "economy",
                "limit", safeLimit,
                "count", rankings.size(),
                "items", rankings
        ));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}

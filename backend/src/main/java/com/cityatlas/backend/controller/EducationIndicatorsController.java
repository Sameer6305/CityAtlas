package com.cityatlas.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.exception.ResourceNotFoundException;
import com.cityatlas.backend.service.CityDataAggregator;
import com.cityatlas.backend.service.external.GeoDBCityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class EducationIndicatorsController {

    private final CityDataAggregator cityDataAggregator;
    private final GeoDBCityService geoDBCityService;

    @GetMapping("/{slug}/education-indicators")
    public ResponseEntity<Map<String, Object>> getEducationIndicators(@PathVariable String slug) {
        String cityName = convertSlugToName(slug);
        GeoDBCityService.CityInfo cityInfo = geoDBCityService.findCity(cityName);

        if (cityInfo == null) {
            throw new ResourceNotFoundException("City", "slug", slug);
        }

        CityDataAggregator.EducationIndicators indicators =
                cityDataAggregator.fetchEducationIndicators(cityInfo.countryCode());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("citySlug", slug);
        payload.put("cityName", cityName);
        payload.put("countryCode", cityInfo.countryCode());
        payload.put("literacyRate", indicators.literacyRate());
        payload.put("pupilTeacherRatio", indicators.pupilTeacherRatio());
        payload.put("renewableEnergyPct", indicators.renewableEnergyPct());
        return ResponseEntity.ok(payload);
    }

    private String convertSlugToName(String slug) {
        String[] words = slug.split("-");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(" ");
            }
            name.append(word.substring(0, 1).toUpperCase())
                .append(word.substring(1).toLowerCase());
        }
        return name.toString();
    }
}

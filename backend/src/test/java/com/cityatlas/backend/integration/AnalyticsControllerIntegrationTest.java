package com.cityatlas.backend.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.cityatlas.backend.dto.response.AnalyticsResponse;
import com.cityatlas.backend.service.CityDataAggregator;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "cityatlas.jwt.secret=test-secret-key-with-at-least-thirty-two-characters",
        "cityatlas.demo.email=TEST_DEMO_LOGIN_EMAIL",
        "cityatlas.demo.password=TEST_DEMO_LOGIN_PASSWORD"
})
@SuppressWarnings("removal")
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CityDataAggregator cityDataAggregator;

    @Test
        @WithMockUser(username = "test-user")
    void analyticsEndpointReturnsGracefulPayload() throws Exception {
        AnalyticsResponse response = AnalyticsResponse.builder()
                .citySlug("new-york")
                .cityName("New York")
                .aqiTrend(List.of())
                .jobSectors(List.of())
                .costOfLiving(List.of())
                .populationTrend(List.of())
                .build();

        // FIXED: Mock external-heavy dependency to verify API contract without network flakiness.
        when(cityDataAggregator.buildAnalyticsResponse("new-york")).thenReturn(response);

        mockMvc.perform(get("/cities/new-york/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citySlug").value("new-york"))
                .andExpect(jsonPath("$.cityName").value("New York"));
    }
}

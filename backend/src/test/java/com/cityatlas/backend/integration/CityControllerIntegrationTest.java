package com.cityatlas.backend.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.cityatlas.backend.dto.response.CityResponse;
import com.cityatlas.backend.exception.ResourceNotFoundException;
import com.cityatlas.backend.service.CityDataAggregator;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "cityatlas.jwt.secret=test-secret-key-with-at-least-thirty-two-characters",
        "cityatlas.demo.email=TEST_DEMO_LOGIN_EMAIL",
        "cityatlas.demo.password=TEST_DEMO_LOGIN_PASSWORD"
})
@SuppressWarnings("removal")
class CityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CityDataAggregator cityDataAggregator;

    @Test
    void cityEndpointAllowsPublicReadAccess() throws Exception {
        // Public read endpoints should be reachable without JWT.
        mockMvc.perform(get("/cities/new-york"))
                .andExpect(status().isOk());
    }

    @Test
        @WithMockUser(username = "test-user")
    void getCityBySlugReturnsCityPayload() throws Exception {
        CityResponse city = CityResponse.builder()
                .id(1L)
                .slug("new-york")
                .name("New York")
                .population(8336817L)
                .lastUpdated(LocalDateTime.now())
                .build();

        when(cityDataAggregator.buildCityResponse("new-york")).thenReturn(city);

        mockMvc.perform(get("/cities/new-york"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New York"))
                .andExpect(jsonPath("$.population").value(8336817))
                .andExpect(jsonPath("$.slug").value("new-york"));
    }

    @Test
        @WithMockUser(username = "test-user")
    void getCityBySlugWithInvalidIdReturns404() throws Exception {
        when(cityDataAggregator.buildCityResponse(anyString()))
                .thenThrow(new ResourceNotFoundException("City", "slug", "invalid-city"));

        mockMvc.perform(get("/cities/invalid-city"))
                .andExpect(status().isNotFound());
    }
}

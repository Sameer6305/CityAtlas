/**
 * Data Transfer Objects - API Request/Response Models
 * 
 * Responsibilities:
 * - API contract definition
 * - Request validation rules
 * - Response serialization
 * - Decouple API from database schema
 * 
 * Sub-packages:
 * - dto.request: API request models
 * - dto.response: API response models
 * 
 * Naming Convention: {Entity}{Type}DTO.java
 * Example: CityRequestDTO.java, CityResponseDTO.java, CityDetailsDTO.java
 * 
 * Use @Valid, @NotNull, @NotBlank for validation
 */
package com.cityatlas.backend.dto;

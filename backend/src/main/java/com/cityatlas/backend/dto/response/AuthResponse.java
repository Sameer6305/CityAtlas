package com.cityatlas.backend.dto.response;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    String email
) {}

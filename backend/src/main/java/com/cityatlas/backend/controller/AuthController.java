package com.cityatlas.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cityatlas.backend.dto.request.LoginRequest;
import com.cityatlas.backend.dto.response.AuthResponse;
import com.cityatlas.backend.exception.ValidationException;
import com.cityatlas.backend.security.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${cityatlas.demo.email:demo@example.com}")
    private String demoEmail;

    @Value("${cityatlas.demo.password:hidden-for-security}")
    private String demoPassword;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // FIXED: Demo login endpoint for interview-safe JWT auth flow without DB dependency.
        if (!demoEmail.equalsIgnoreCase(loginRequest.email()) || !demoPassword.equals(loginRequest.password())) {
            throw new ValidationException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(demoEmail);
        return ResponseEntity.ok(new AuthResponse(token, "Bearer", jwtUtil.getExpirationSeconds(), demoEmail));
    }
}

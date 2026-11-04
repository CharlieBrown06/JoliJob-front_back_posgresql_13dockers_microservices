package com.example.jolijob.auth.web.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        String role,
        String userId
) {}

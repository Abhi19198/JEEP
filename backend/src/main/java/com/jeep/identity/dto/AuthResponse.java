package com.jeep.identity.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    String email,
    String role
) {
}

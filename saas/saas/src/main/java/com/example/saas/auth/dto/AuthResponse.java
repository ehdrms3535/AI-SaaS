package com.example.saas.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserView user
) {
    public record UserView(UUID id, String email, String name) {}
}
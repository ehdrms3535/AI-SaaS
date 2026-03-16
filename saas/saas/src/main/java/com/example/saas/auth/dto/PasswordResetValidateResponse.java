package com.example.saas.auth.dto;

import java.time.Instant;

public record PasswordResetValidateResponse(
        boolean valid,
        Instant expiresAt
) {
}

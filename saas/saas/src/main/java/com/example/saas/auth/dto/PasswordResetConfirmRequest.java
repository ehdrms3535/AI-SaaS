package com.example.saas.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank String newPassword
) {
}

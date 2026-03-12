package com.example.saas.serviceapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer durationMinutes,
        @NotNull @Min(0) Integer price,
        Boolean active
) {
}

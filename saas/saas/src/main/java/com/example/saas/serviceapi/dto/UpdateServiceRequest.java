package com.example.saas.serviceapi.dto;

import jakarta.validation.constraints.Min;

public record UpdateServiceRequest(
        String name,
        @Min(1) Integer durationMinutes,
        @Min(0) Integer price,
        Boolean active
) {
}

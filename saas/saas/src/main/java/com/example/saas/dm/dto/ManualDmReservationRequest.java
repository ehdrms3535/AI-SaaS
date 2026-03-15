package com.example.saas.dm.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ManualDmReservationRequest(
        @NotNull UUID customerId,
        @NotNull UUID serviceId,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        String notes
) {
}

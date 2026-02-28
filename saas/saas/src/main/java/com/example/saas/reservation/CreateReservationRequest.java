package com.example.saas.reservation;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID customerId,
        UUID serviceId,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        String notes
) {}
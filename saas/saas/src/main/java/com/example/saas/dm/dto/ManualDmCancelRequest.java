package com.example.saas.dm.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManualDmCancelRequest(
        @NotNull UUID reservationId
) {
}

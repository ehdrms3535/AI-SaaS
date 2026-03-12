package com.example.saas.dm.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DmReservationResponse(
        boolean success,
        boolean customerCreated,
        String reply,
        UUID reservationId,
        UUID customerId,
        UUID serviceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        java.util.List<OffsetDateTime> suggestedStartTimes
) {
}

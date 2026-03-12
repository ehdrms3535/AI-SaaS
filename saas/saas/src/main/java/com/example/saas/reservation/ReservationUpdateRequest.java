package com.example.saas.reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationUpdateRequest(
        UUID customerId,
        UUID serviceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String notes
) {}
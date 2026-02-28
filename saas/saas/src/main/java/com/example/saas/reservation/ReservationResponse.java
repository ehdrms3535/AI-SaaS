package com.example.saas.reservation;

import com.example.saas.domain.Reservation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID organizationId,
        UUID customerId,
        UUID serviceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        UUID createdByUserId,
        String notes
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getOrganizationId(),
                r.getCustomerId(),
                r.getServiceId(),
                r.getStartAt(),
                r.getEndAt(),
                r.getCreatedByUserId(),
                r.getNotes()
        );
    }
}
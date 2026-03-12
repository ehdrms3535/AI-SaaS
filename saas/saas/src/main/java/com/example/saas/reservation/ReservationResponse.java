package com.example.saas.reservation;

import com.example.saas.domain.Reservation;
import com.example.saas.domain.ReservationSource;
import com.example.saas.domain.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID organizationId,
        UUID customerId,
        UUID serviceId,
        ReservationStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        ReservationSource source,
        UUID createdByUserId,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime canceledAt,
        UUID canceledByUserId
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getOrganizationId(),
                r.getCustomerId(),
                r.getServiceId(),
                r.getStatus(),
                r.getStartAt(),
                r.getEndAt(),
                r.getSource(),
                r.getCreatedByUserId(),
                r.getNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCanceledAt(),
                r.getCanceledByUserId()
        );
    }
}

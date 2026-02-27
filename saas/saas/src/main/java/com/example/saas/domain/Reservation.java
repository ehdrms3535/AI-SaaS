package com.example.saas.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;

    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = "service_id", columnDefinition = "uuid")
    private UUID serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationSource source = ReservationSource.MANUAL;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by_user_id", columnDefinition = "uuid")
    private UUID createdByUserId;

    @Column(name = "ai_request_id", columnDefinition = "uuid")
    private UUID aiRequestId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        // DB default(now())가 있으니, null이면 넣어주는 정도로만
        if (this.id == null) this.id = UUID.randomUUID();
    }
}
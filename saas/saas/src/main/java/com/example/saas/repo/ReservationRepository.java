package com.example.saas.repo;

import com.example.saas.domain.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByOrganizationIdOrderByStartAtAsc(UUID organizationId);

    @Query(value = """
        SELECT *
        FROM reservations
        WHERE organization_id = :orgId
          AND start_at < :newEnd
          AND end_at > :newStart
        FOR UPDATE
        """, nativeQuery = true)
    List<Reservation> lockOverlapsForUpdate(
            @Param("orgId") UUID orgId,
            @Param("newStart") OffsetDateTime newStart,
            @Param("newEnd") OffsetDateTime newEnd
    );
}
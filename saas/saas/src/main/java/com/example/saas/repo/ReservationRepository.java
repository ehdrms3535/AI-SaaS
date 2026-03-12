package com.example.saas.repo;

import com.example.saas.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    long countByOrganizationIdAndCanceledAtIsNull(UUID organizationId);

    List<Reservation> findByOrganizationIdOrderByStartAtAsc(UUID organizationId);

    List<Reservation> findByOrganizationIdAndCanceledAtIsNullOrderByStartAtAsc(UUID organizationId);

    @Query("""
        select r
        from Reservation r
        where r.organizationId = :orgId
          and r.canceledAt is null
          and r.startAt < :endAt
          and r.endAt > :startAt
        order by r.startAt asc
    """)
    List<Reservation> findActiveOverlapsInWindow(
            @Param("orgId") UUID orgId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        select r
        from Reservation r
        where r.organizationId = :orgId
          and r.startAt >= :startAt
          and r.endAt <= :endAt
          and r.canceledAt is null
        order by r.startAt asc
    """)
    List<Reservation> findByOrganizationIdAndTimeRange(
            @Param("orgId") UUID orgId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        select r
        from Reservation r
        where r.organizationId = :orgId
          and r.startAt >= :startAt
          and r.endAt <= :endAt
        order by r.startAt asc
    """)
    List<Reservation> findByOrganizationIdAndTimeRangeIncludingCanceled(
            @Param("orgId") UUID orgId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("""
        select r
        from Reservation r
        where r.id = :id
          and r.organizationId = :orgId
    """)
    Optional<Reservation> findByIdAndOrganizationId(
            @Param("id") UUID id,
            @Param("orgId") UUID orgId
    );

    @Query(value = """
        SELECT *
        FROM reservations
        WHERE organization_id = :orgId
          AND canceled_at IS NULL
          AND start_at < :newEnd
          AND end_at > :newStart
        FOR UPDATE
        """, nativeQuery = true)
    List<Reservation> lockOverlapsForUpdate(
            @Param("orgId") UUID orgId,
            @Param("newStart") OffsetDateTime newStart,
            @Param("newEnd") OffsetDateTime newEnd
    );

    // ✅ 추가: 트랜잭션 범위 advisory lock
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void xactLock(@Param("key") String key);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Reservation r
        where r.id = :id
        and r.organizationId = :orgId
    """)
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id,
                                                @Param("orgId") UUID orgId);

    @Query(value = """
        select 1
        from reservations r
        where r.organization_id = :orgId
          and r.canceled_at is null
          and r.id <> :excludeId
          and r.start_at < :newEnd
          and r.end_at > :newStart
        for update
        limit 1
    """, nativeQuery = true)
    Optional<Integer> existsOverlapForUpdate(@Param("orgId") UUID orgId,
                                             @Param("excludeId") UUID excludeId,
                                             @Param("newStart") OffsetDateTime newStart,
                                             @Param("newEnd") OffsetDateTime newEnd);

}

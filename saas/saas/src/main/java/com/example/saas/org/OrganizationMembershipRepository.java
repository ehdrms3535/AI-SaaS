package com.example.saas.org;

import com.example.saas.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    // OWNER 우선 기본 org 선택(없으면 아무거나 1개)
    @Query(value = """
        select organization_id
        from organization_memberships
        where user_id = :userId
        order by case when role = 'OWNER' then 0 else 1 end
        limit 1
        """, nativeQuery = true)
    Optional<UUID> findDefaultOrgIdByUserId(UUID userId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationIdAndRole(UUID userId, UUID organizationId, OrganizationMembership.OrgRole role);

    java.util.List<OrganizationMembership> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select r from Reservation r
    where r.id = :id
      and r.organizationId = :orgId
""")
    Optional<Reservation> findByIdForUpdate(UUID id, UUID orgId);

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
    Optional<Integer> existsOverlapForUpdate(
            UUID orgId,
            UUID excludeId,
            OffsetDateTime newStart,
            OffsetDateTime newEnd
    );
}

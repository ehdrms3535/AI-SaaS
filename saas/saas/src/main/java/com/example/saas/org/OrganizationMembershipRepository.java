package com.example.saas.org;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    // OWNER 우선 기본 org 선택(없으면 아무거나 1개)
    @Query("""
        select m.organizationId
        from OrganizationMembership m
        where m.userId = :userId
        order by case when m.role = com.example.saas.org.OrganizationMembership.OrgRole.OWNER then 0 else 1 end
        """)
    Optional<UUID> findDefaultOrgIdByUserId(UUID userId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}
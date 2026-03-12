package com.example.saas.repo;

import com.example.saas.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    long countByOrganizationId(UUID organizationId);

    List<Service> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<Service> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
        select s
        from Service s
        where s.organizationId = :orgId
          and lower(s.name) like lower(concat('%', :q, '%'))
        order by s.createdAt desc
    """)
    List<Service> searchByOrganizationId(@Param("orgId") UUID organizationId, @Param("q") String query);
}

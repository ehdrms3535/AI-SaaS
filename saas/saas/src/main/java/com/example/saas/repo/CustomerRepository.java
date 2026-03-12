package com.example.saas.repo;

import com.example.saas.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    long countByOrganizationId(UUID organizationId);

    List<Customer> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<Customer> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
        select c
        from Customer c
        where c.organizationId = :orgId
          and (
            lower(c.name) like lower(concat('%', :q, '%'))
            or coalesce(lower(c.phone), '') like lower(concat('%', :q, '%'))
            or coalesce(lower(c.email), '') like lower(concat('%', :q, '%'))
          )
        order by c.createdAt desc
    """)
    List<Customer> searchByOrganizationId(@Param("orgId") UUID organizationId, @Param("q") String query);
}

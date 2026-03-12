package com.example.saas.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsByIdAndOrganizationId(UUID customerId, UUID organizationId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from customers where id = ? and organization_id = ?",
                Integer.class,
                customerId,
                organizationId
        );
        return count != null && count > 0;
    }
}

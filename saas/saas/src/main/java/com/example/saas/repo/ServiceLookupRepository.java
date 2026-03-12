package com.example.saas.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsByIdAndOrganizationId(UUID serviceId, UUID organizationId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from services where id = ? and organization_id = ?",
                Integer.class,
                serviceId,
                organizationId
        );
        return count != null && count > 0;
    }
}

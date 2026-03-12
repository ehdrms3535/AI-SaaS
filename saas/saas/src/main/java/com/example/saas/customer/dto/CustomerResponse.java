package com.example.saas.customer.dto;

import com.example.saas.domain.Customer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID organizationId,
        String name,
        String phone,
        String email,
        String memo,
        OffsetDateTime createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getOrganizationId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getMemo(),
                customer.getCreatedAt()
        );
    }
}

package com.example.saas.serviceapi.dto;

import com.example.saas.domain.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        UUID organizationId,
        String name,
        Integer durationMinutes,
        Integer price,
        Boolean active,
        OffsetDateTime createdAt
) {
    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getOrganizationId(),
                service.getName(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.getActive(),
                service.getCreatedAt()
        );
    }
}

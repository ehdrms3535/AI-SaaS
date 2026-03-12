package com.example.saas.billing;

import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.repo.CustomerRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanLimitService {

    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final ReservationRepository reservationRepository;

    public OrganizationPlan getPlan(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(org -> org.getPlan() == null ? OrganizationPlan.FREE : org.getPlan())
                .orElse(OrganizationPlan.FREE);
    }

    public PlanSnapshot getSnapshot(UUID organizationId) {
        OrganizationPlan plan = getPlan(organizationId);
        return new PlanSnapshot(
                plan,
                customerRepository.countByOrganizationId(organizationId),
                customerLimit(plan),
                serviceRepository.countByOrganizationId(organizationId),
                serviceLimit(plan),
                reservationRepository.countByOrganizationIdAndCanceledAtIsNull(organizationId),
                activeReservationLimit(plan)
        );
    }

    public void assertCanCreateCustomer(UUID organizationId) {
        OrganizationPlan plan = getPlan(organizationId);
        long count = customerRepository.countByOrganizationId(organizationId);
        long limit = customerLimit(plan);
        if (count >= limit) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
    }

    public void assertCanCreateService(UUID organizationId) {
        OrganizationPlan plan = getPlan(organizationId);
        long count = serviceRepository.countByOrganizationId(organizationId);
        long limit = serviceLimit(plan);
        if (count >= limit) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
    }

    public void assertCanCreateReservation(UUID organizationId) {
        OrganizationPlan plan = getPlan(organizationId);
        long count = reservationRepository.countByOrganizationIdAndCanceledAtIsNull(organizationId);
        long limit = activeReservationLimit(plan);
        if (count >= limit) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
    }

    private long customerLimit(OrganizationPlan plan) {
        return switch (plan) {
            case FREE -> 100;
            case PRO -> 1_000;
            case ENTERPRISE -> Long.MAX_VALUE;
        };
    }

    private long serviceLimit(OrganizationPlan plan) {
        return switch (plan) {
            case FREE -> 20;
            case PRO -> 200;
            case ENTERPRISE -> Long.MAX_VALUE;
        };
    }

    private long activeReservationLimit(OrganizationPlan plan) {
        return switch (plan) {
            case FREE -> 500;
            case PRO -> 10_000;
            case ENTERPRISE -> Long.MAX_VALUE;
        };
    }
}

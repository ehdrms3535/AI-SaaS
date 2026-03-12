package com.example.saas.billing;

public record PlanSnapshot(
        OrganizationPlan plan,
        long customerCount,
        long customerLimit,
        long serviceCount,
        long serviceLimit,
        long activeReservationCount,
        long activeReservationLimit
) {
}

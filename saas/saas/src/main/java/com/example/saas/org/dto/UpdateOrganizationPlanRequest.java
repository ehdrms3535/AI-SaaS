package com.example.saas.org.dto;

import com.example.saas.billing.OrganizationPlan;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationPlanRequest(@NotNull OrganizationPlan plan) {
}

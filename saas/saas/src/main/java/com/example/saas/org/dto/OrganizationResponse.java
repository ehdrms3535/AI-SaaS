package com.example.saas.org.dto;

import com.example.saas.billing.OrganizationPlan;
import com.example.saas.billing.PlanSnapshot;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String slug,
        String name,
        String timezone,
        OrganizationPlan plan,
        PlanSnapshot usage,
        OrganizationScheduleResponse schedule,
        OrganizationWebhookResponse webhook
) { }

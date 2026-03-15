package com.example.saas.org.dto;

public record OrganizationWebhookResponse(
        boolean enabled,
        String secret
) {
}

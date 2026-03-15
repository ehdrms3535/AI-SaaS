package com.example.saas.org.dto;

public record UpdateOrganizationWebhookRequest(
        boolean enabled,
        String secret
) {
}

package com.example.saas.channel.dto;

import java.util.UUID;

public record InstagramConnectStartResponse(
        UUID organizationId,
        boolean configured,
        String state,
        String redirectUri,
        String authorizeBaseUrl,
        String scopes,
        String authorizationUrl,
        String message
) {
}

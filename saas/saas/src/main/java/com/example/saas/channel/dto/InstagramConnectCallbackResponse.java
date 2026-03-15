package com.example.saas.channel.dto;

import java.util.UUID;

public record InstagramConnectCallbackResponse(
        UUID channelId,
        UUID organizationId,
        String provider,
        String status,
        String accountName,
        String username,
        String externalAccountId,
        String message,
        Integer errorCode,
        String errorMessage
) {
}

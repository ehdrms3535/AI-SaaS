package com.example.saas.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InboundDmWebhookRequest(
        @NotNull UUID organizationId,
        UUID actorUserId,
        @NotBlank String channel,
        String senderChannelId,
        String senderName,
        String senderPhone,
        String customerHint,
        String serviceHint,
        Integer durationMinutes,
        @NotBlank String message
) {
}

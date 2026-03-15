package com.example.saas.channel.dto;

import com.example.saas.channel.ChannelProvider;
import com.example.saas.channel.ConnectedChannelStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConnectedChannelResponse(
        UUID id,
        UUID organizationId,
        ChannelProvider provider,
        ConnectedChannelStatus status,
        String externalAccountId,
        String accountName,
        String username,
        boolean webhookSubscribed,
        OffsetDateTime tokenExpiresAt,
        OffsetDateTime connectedAt,
        OffsetDateTime disconnectedAt
) {
}

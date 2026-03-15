package com.example.saas.channel.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InstagramChannelSyncResponse(
        UUID channelId,
        UUID organizationId,
        String provider,
        String externalAccountId,
        String accountName,
        String username,
        String tokenUserId,
        String tokenUserName,
        String pageId,
        String pageName,
        int pageCount,
        List<PageSummary> pages,
        DebugTokenSummary debugToken,
        OffsetDateTime tokenExpiresAt,
        String message
) {
    public record PageSummary(
            String id,
            String name,
            String instagramBusinessAccountId,
            String instagramUsername
    ) {
    }

    public record DebugTokenSummary(
            String appId,
            String type,
            String application,
            boolean isValid,
            Long expiresAtEpoch,
            List<String> scopes,
            String userId,
            Map<String, Object> raw
    ) {
    }
}

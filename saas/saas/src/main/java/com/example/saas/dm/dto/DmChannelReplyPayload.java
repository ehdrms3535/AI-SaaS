package com.example.saas.dm.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DmChannelReplyPayload(
        UUID organizationId,
        String channel,
        String senderChannelId,
        String text,
        List<String> quickReplies,
        Map<String, Object> metadata
) {
}

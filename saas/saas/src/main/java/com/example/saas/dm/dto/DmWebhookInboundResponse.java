package com.example.saas.dm.dto;

public record DmWebhookInboundResponse(
        DmReservationResponse result,
        DmChannelReplyPayload replyPayload,
        DmChannelSendResult dispatch
) {
}

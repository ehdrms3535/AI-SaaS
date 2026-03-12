package com.example.saas.dm.dto;

import com.example.saas.domain.DmMessage;
import com.example.saas.domain.DmMessageStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DmMessageResponse(
        UUID id,
        String channel,
        String senderName,
        String senderPhone,
        String customerHint,
        String serviceHint,
        String messageText,
        DmMessageStatus status,
        UUID customerId,
        UUID reservationId,
        String failureReason,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt
) {
    public static DmMessageResponse from(DmMessage message) {
        return new DmMessageResponse(
                message.getId(),
                message.getChannel(),
                message.getSenderName(),
                message.getSenderPhone(),
                message.getCustomerHint(),
                message.getServiceHint(),
                message.getMessageText(),
                message.getStatus(),
                message.getCustomerId(),
                message.getReservationId(),
                message.getFailureReason(),
                message.getReceivedAt(),
                message.getProcessedAt()
        );
    }
}

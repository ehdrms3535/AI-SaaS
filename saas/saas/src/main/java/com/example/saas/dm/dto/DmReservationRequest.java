package com.example.saas.dm.dto;

public record DmReservationRequest(
        String message,
        String customerHint,
        String senderName,
        String senderPhone,
        String serviceHint,
        Integer durationMinutes
) {
}

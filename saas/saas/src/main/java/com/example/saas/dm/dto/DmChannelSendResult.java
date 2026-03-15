package com.example.saas.dm.dto;

public record DmChannelSendResult(
        boolean accepted,
        String providerMessage
) {
}

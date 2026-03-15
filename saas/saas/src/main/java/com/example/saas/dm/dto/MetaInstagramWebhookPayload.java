package com.example.saas.dm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MetaInstagramWebhookPayload(
        String object,
        List<Entry> entry
) {
    public record Entry(
            String id,
            Long time,
            List<Messaging> messaging
    ) {
    }

    public record Messaging(
            UserRef sender,
            UserRef recipient,
            Long timestamp,
            Message message
    ) {
    }

    public record UserRef(
            String id
    ) {
    }

    public record Message(
            String mid,
            String text,
            @JsonProperty("is_echo")
            Boolean isEcho
    ) {
    }
}

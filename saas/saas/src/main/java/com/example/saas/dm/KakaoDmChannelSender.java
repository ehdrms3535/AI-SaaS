package com.example.saas.dm;

import com.example.saas.dm.config.DmSenderProperties;
import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.DmChannelSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoDmChannelSender implements DmChannelSender {

    private final DmSenderProperties properties;
    private final RestClient restClient;

    public KakaoDmChannelSender(DmSenderProperties properties, RestClient dmRestClient) {
        this.properties = properties;
        this.restClient = dmRestClient;
    }

    @Override
    public boolean supports(String channel) {
        return "KAKAO".equalsIgnoreCase(channel);
    }

    @Override
    public DmChannelSendResult send(DmChannelReplyPayload payload) {
        DmSenderProperties.Channel channel = properties.getKakao();
        if (!channel.isEnabled()) {
            log.info("Kakao DM sender disabled, falling back to dry-run log");
            return new DmChannelSendResult(true, "KAKAO_DISABLED_NOOP");
        }
        if (channel.isDryRun() || isBlank(channel.getBaseUrl()) || isBlank(channel.getAccessToken())) {
            log.info("Kakao DM dry-run send: senderChannelId={}, text={}, quickReplies={}, baseUrlConfigured={}, tokenConfigured={}",
                    payload.senderChannelId(),
                    payload.text(),
                    payload.quickReplies(),
                    !isBlank(channel.getBaseUrl()),
                    !isBlank(channel.getAccessToken()));
            return new DmChannelSendResult(true, "KAKAO_DRY_RUN");
        }

        try {
            restClient.post()
                    .uri(channel.getBaseUrl())
                    .header("Authorization", "Bearer " + channel.getAccessToken())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Kakao DM live send success: senderChannelId={}, baseUrl={}", payload.senderChannelId(), channel.getBaseUrl());
            return new DmChannelSendResult(true, "KAKAO_LIVE_SENT");
        } catch (RestClientResponseException e) {
            log.warn("Kakao DM live send failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return new DmChannelSendResult(false, "KAKAO_HTTP_" + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("Kakao DM live send exception: {}", e.getMessage());
            return new DmChannelSendResult(false, "KAKAO_SEND_ERROR");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

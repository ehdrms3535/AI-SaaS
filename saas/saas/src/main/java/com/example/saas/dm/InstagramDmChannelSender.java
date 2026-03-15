package com.example.saas.dm;

import com.example.saas.channel.ChannelProvider;
import com.example.saas.channel.ConnectedChannelStatus;
import com.example.saas.dm.config.DmSenderProperties;
import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.DmChannelSendResult;
import com.example.saas.domain.ConnectedChannel;
import com.example.saas.repo.ConnectedChannelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class InstagramDmChannelSender implements DmChannelSender {

    private final DmSenderProperties properties;
    private final RestClient restClient;
    private final ConnectedChannelRepository connectedChannelRepository;

    public InstagramDmChannelSender(DmSenderProperties properties,
                                    RestClient dmRestClient,
                                    ConnectedChannelRepository connectedChannelRepository) {
        this.properties = properties;
        this.restClient = dmRestClient;
        this.connectedChannelRepository = connectedChannelRepository;
    }

    @Override
    public boolean supports(String channel) {
        return "INSTAGRAM".equalsIgnoreCase(channel);
    }

    @Override
    public DmChannelSendResult send(DmChannelReplyPayload payload) {
        DmSenderProperties.Channel config = properties.getInstagram();
        if (!config.isEnabled()) {
            log.info("Instagram DM sender disabled, falling back to dry-run log");
            return new DmChannelSendResult(true, "INSTAGRAM_DISABLED_NOOP");
        }

        ConnectedChannel connectedChannel = resolveConnectedChannel(payload);
        String businessAccountId = connectedChannel != null ? connectedChannel.getExternalAccountId() : null;
        String accessToken = firstNonBlank(connectedChannel != null ? connectedChannel.getAccessToken() : null, config.getAccessToken());
        String baseUrl = resolveBaseUrl(config, businessAccountId);

        if (config.isDryRun() || isBlank(baseUrl) || isBlank(accessToken)) {
            log.info("Instagram DM dry-run send: senderChannelId={}, businessAccountId={}, text={}, quickReplies={}, baseUrlConfigured={}, tokenConfigured={}",
                    payload.senderChannelId(),
                    businessAccountId,
                    payload.text(),
                    payload.quickReplies(),
                    !isBlank(baseUrl),
                    !isBlank(accessToken));
            return new DmChannelSendResult(true, "INSTAGRAM_DRY_RUN");
        }

        try {
            Map<String, Object> body = buildInstagramBody(payload);
            restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            log.info("Instagram DM live send success: senderChannelId={}, businessAccountId={}, baseUrl={}",
                    payload.senderChannelId(),
                    businessAccountId,
                    baseUrl);
            return new DmChannelSendResult(true, "INSTAGRAM_LIVE_SENT");
        } catch (RestClientResponseException e) {
            log.warn("Instagram DM live send failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return new DmChannelSendResult(false, "INSTAGRAM_HTTP_" + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("Instagram DM live send exception: {}", e.getMessage());
            return new DmChannelSendResult(false, "INSTAGRAM_SEND_ERROR");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String primary, String fallback) {
        return !isBlank(primary) ? primary : fallback;
    }

    private ConnectedChannel resolveConnectedChannel(DmChannelReplyPayload payload) {
        if (payload.organizationId() == null) {
            return null;
        }
        return connectedChannelRepository
                .findFirstByOrganizationIdAndProviderOrderByCreatedAtDesc(payload.organizationId(), ChannelProvider.INSTAGRAM)
                .filter(channel -> channel.getStatus() == ConnectedChannelStatus.ACTIVE)
                .orElse(null);
    }

    private String resolveBaseUrl(DmSenderProperties.Channel config, String businessAccountId) {
        if (!isBlank(config.getBaseUrl())) {
            return config.getBaseUrl().contains("{accountId}")
                    ? config.getBaseUrl().replace("{accountId}", businessAccountId == null ? "" : businessAccountId)
                    : config.getBaseUrl();
        }
        if (isBlank(businessAccountId)) {
            return null;
        }
        return "https://graph.facebook.com/v23.0/" + businessAccountId + "/messages";
    }

    private Map<String, Object> buildInstagramBody(DmChannelReplyPayload payload) {
        // Current live assumption: a Meta-style text send endpoint that accepts
        // recipient.id and message.text. Quick replies are kept in replyPayload
        // for adapters, but are not sent here until the provider contract is fixed.
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("id", payload.senderChannelId());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("text", payload.text());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipient", recipient);
        body.put("message", message);
        return body;
    }
}

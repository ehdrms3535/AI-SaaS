package com.example.saas.api;

import com.example.saas.api.error.BadRequestException;
import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.channel.ChannelProvider;
import com.example.saas.channel.ConnectedChannelStatus;
import com.example.saas.dm.config.DmSenderProperties;
import com.example.saas.dm.DmMessageService;
import com.example.saas.dm.dto.DmWebhookInboundResponse;
import com.example.saas.dm.dto.InboundDmWebhookRequest;
import com.example.saas.dm.dto.MetaInstagramWebhookPayload;
import com.example.saas.domain.ConnectedChannel;
import com.example.saas.domain.Organization;
import com.example.saas.repo.ConnectedChannelRepository;
import com.example.saas.repo.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class DmWebhookController {

    private final DmMessageService dmMessageService;
    private final OrganizationRepository organizationRepository;
    private final ConnectedChannelRepository connectedChannelRepository;
    private final DmSenderProperties dmSenderProperties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @PostMapping("/dm/inbound")
    public ResponseEntity<DmWebhookInboundResponse> inbound(@RequestBody @Valid InboundDmWebhookRequest req,
                                                            @RequestHeader(name = "X-DM-Webhook-Secret", required = false) String webhookSecret) {
        Organization organization = organizationRepository.findById(req.organizationId())
                .orElseThrow(() -> new BadRequestException("ORG_NOT_FOUND", "조직을 찾을 수 없습니다."));
        if (!organization.isDmWebhookEnabled()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (organization.getDmWebhookSecret() == null || organization.getDmWebhookSecret().isBlank()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (webhookSecret == null || !organization.getDmWebhookSecret().equals(webhookSecret.trim())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(dmMessageService.processWebhook(req));
    }

    @GetMapping("/meta/instagram")
    public ResponseEntity<String> verifyInstagramWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        System.out.println("[META_WEBHOOK_VERIFY] mode=" + mode + " tokenPresent=" + (verifyToken != null) + " challengePresent=" + (challenge != null));
        String expectedToken = dmSenderProperties.getInstagram().getWebhookVerifyToken();
        if (!"subscribe".equals(mode) || expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(verifyToken)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(challenge == null ? "" : challenge);
    }

    @PostMapping("/meta/instagram")
    public ResponseEntity<List<DmWebhookInboundResponse>> receiveInstagramWebhook(@RequestBody MetaInstagramWebhookPayload payload) {
        int entryCount = payload.entry() == null ? 0 : payload.entry().size();
        System.out.println("[META_WEBHOOK_POST] object=" + payload.object() + " entryCount=" + entryCount);
        try {
            System.out.println("[META_WEBHOOK_RAW] " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
        } catch (Exception ignored) {
        }
        if (!"instagram".equalsIgnoreCase(payload.object()) && !"page".equalsIgnoreCase(payload.object())) {
            throw new BadRequestException("UNSUPPORTED_WEBHOOK_OBJECT", "지원하지 않는 webhook object 입니다.");
        }

        List<DmWebhookInboundResponse> responses = new ArrayList<>();
        if (payload.entry() == null) {
            return ResponseEntity.ok(responses);
        }

        for (MetaInstagramWebhookPayload.Entry entry : payload.entry()) {
            if (entry.messaging() == null) {
                continue;
            }
            for (MetaInstagramWebhookPayload.Messaging messaging : entry.messaging()) {
                boolean isEcho = messaging.message() != null && Boolean.TRUE.equals(messaging.message().isEcho());
                String eventType = isEcho ? "ECHO" : "INBOUND";
                System.out.println("[META_WEBHOOK_" + eventType + "] recipient=" + (messaging.recipient() != null ? messaging.recipient().id() : null)
                        + " sender=" + (messaging.sender() != null ? messaging.sender().id() : null)
                        + " text=" + (messaging.message() != null ? messaging.message().text() : null));
                if (messaging.message() == null || messaging.message().text() == null || messaging.message().text().isBlank()) {
                    continue;
                }
                if (isEcho) {
                    continue;
                }
                if (messaging.recipient() == null || messaging.recipient().id() == null || messaging.recipient().id().isBlank()) {
                    continue;
                }

                ConnectedChannel channel = connectedChannelRepository
                        .findFirstByProviderAndExternalAccountIdAndStatusOrderByCreatedAtDesc(
                                ChannelProvider.INSTAGRAM,
                                messaging.recipient().id(),
                                ConnectedChannelStatus.ACTIVE
                        )
                        .orElseThrow(() -> new BadRequestException("CHANNEL_NOT_FOUND", "연결된 Instagram 채널을 찾을 수 없습니다."));

                InboundDmWebhookRequest request = new InboundDmWebhookRequest(
                        channel.getOrganizationId(),
                        null,
                        ChannelProvider.INSTAGRAM.name(),
                        messaging.sender() != null ? messaging.sender().id() : null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        messaging.message().text()
                );
                responses.add(dmMessageService.processWebhook(request));
            }
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping(value = "/meta/data-deletion", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> receiveDataDeletionRequest(
            @RequestParam("signed_request") String signedRequest,
            HttpServletRequest request
    ) {
        Map<String, Object> payload = parseSignedRequest(signedRequest);
        String userId = payload.get("user_id") == null ? "unknown" : String.valueOf(payload.get("user_id"));
        String confirmationCode = buildDeletionConfirmationCode(userId);
        String statusUrl = buildBaseUrl(request) + "/webhooks/meta/data-deletion/status?code=" + confirmationCode;

        System.out.println("[META_DATA_DELETION] userId=" + userId + " confirmationCode=" + confirmationCode);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("url", statusUrl);
        response.put("confirmation_code", confirmationCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/meta/data-deletion/status", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dataDeletionStatus(@RequestParam("code") String code) {
        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Data Deletion Status</title>
                  <style>
                    body { font-family: "Segoe UI", system-ui, sans-serif; margin: 0; background: #f4f7fb; color: #162033; }
                    main { max-width: 720px; margin: 48px auto; padding: 0 20px; }
                    .card { background: #fff; border: 1px solid #d9e1f0; border-radius: 18px; padding: 32px; box-shadow: 0 12px 32px rgba(23,38,70,0.08); }
                    h1 { margin-top: 0; }
                    code { background: #eef4ff; padding: 2px 6px; border-radius: 6px; }
                  </style>
                </head>
                <body>
                <main>
                  <section class="card">
                    <h1>Data Deletion Request Received</h1>
                    <p>Your deletion request has been received by SaaS Reservation.</p>
                    <p>Confirmation code: <code>%s</code></p>
                    <p>Status: Received and queued for review.</p>
                    <p>For follow-up, contact <a href="mailto:ehdrms3535@naver.com">ehdrms3535@naver.com</a>.</p>
                  </section>
                </main>
                </body>
                </html>
                """.formatted(escapeHtml(code));
        return ResponseEntity.ok(html);
    }

    private Map<String, Object> parseSignedRequest(String signedRequest) {
        try {
            String[] parts = signedRequest.split("\\.", 2);
            if (parts.length != 2) {
                throw new BadRequestException("INVALID_SIGNED_REQUEST", "signed_request 형식이 올바르지 않습니다.");
            }

            byte[] signature = Base64.getUrlDecoder().decode(parts[0]);
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);

            String secret = dmSenderProperties.getInstagram().getClientSecret();
            if (secret == null || secret.isBlank()) {
                throw new BadRequestException("MISSING_APP_SECRET", "Instagram client secret이 설정되지 않았습니다.");
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(parts[1].getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(signature, expected)) {
                throw new BadRequestException("INVALID_SIGNED_REQUEST", "signed_request 서명이 유효하지 않습니다.");
            }

            return objectMapper.readValue(payloadBytes, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("INVALID_SIGNED_REQUEST", "signed_request를 해석하지 못했습니다.");
        }
    }

    private String buildDeletionConfirmationCode(String userId) {
        return "del-" + userId + "-" + Instant.now().getEpochSecond();
    }

    private String buildBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

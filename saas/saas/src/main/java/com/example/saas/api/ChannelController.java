package com.example.saas.api;

import com.example.saas.api.error.NotFoundException;
import com.example.saas.channel.ChannelProvider;
import com.example.saas.channel.ConnectedChannelStatus;
import com.example.saas.channel.dto.ConnectedChannelResponse;
import com.example.saas.channel.dto.InstagramConnectCallbackResponse;
import com.example.saas.channel.dto.InstagramChannelSyncResponse;
import com.example.saas.channel.dto.InstagramConnectStartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.dm.config.DmSenderProperties;
import com.example.saas.domain.ConnectedChannel;
import com.example.saas.org.OrganizationMembership;
import com.example.saas.org.OrganizationMembershipRepository;
import com.example.saas.repo.ConnectedChannelRepository;
import com.example.saas.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels")
public class ChannelController {

    private final ConnectedChannelRepository connectedChannels;
    private final OrganizationMembershipRepository memberships;
    private final DmSenderProperties dmSenderProperties;
    private final RestClient dmRestClient;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<ConnectedChannelResponse>> list(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        List<ConnectedChannelResponse> channels = connectedChannels.findAllByOrganizationIdOrderByCreatedAtDesc(principal.orgId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(channels);
    }

    @PostMapping("/instagram/connect/start")
    public ResponseEntity<InstagramConnectStartResponse> startInstagramConnect(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertOwner(principal.userId(), principal.orgId());

        DmSenderProperties.Channel instagram = dmSenderProperties.getInstagram();
        String state = UUID.randomUUID().toString().replace("-", "");
        ConnectedChannel channel = connectedChannels
                .findFirstByOrganizationIdAndProviderOrderByCreatedAtDesc(principal.orgId(), ChannelProvider.INSTAGRAM)
                .orElseGet(ConnectedChannel::new);

        OffsetDateTime now = OffsetDateTime.now();
        if (channel.getId() == null) {
            channel.setId(UUID.randomUUID());
            channel.setOrganizationId(principal.orgId());
            channel.setProvider(ChannelProvider.INSTAGRAM);
            channel.setCreatedAt(now);
        }
        channel.setStatus(ConnectedChannelStatus.PENDING);
        channel.setOauthState(state);
        channel.setUpdatedAt(now);
        channel.setDisconnectedAt(null);
        connectedChannels.save(channel);

        boolean configured = instagram.getClientId() != null && !instagram.getClientId().isBlank()
                && instagram.getRedirectUri() != null && !instagram.getRedirectUri().isBlank();

        String authorizationUrl = configured
                ? UriComponentsBuilder.fromUriString(instagram.getOauthAuthorizeUrl())
                        .queryParam("client_id", instagram.getClientId())
                        .queryParam("redirect_uri", instagram.getRedirectUri())
                        .queryParam("scope", instagram.getScopes())
                        .queryParam("response_type", "code")
                        .queryParam("state", state)
                        .build()
                        .toUriString()
                : null;

        String message = configured
                ? "Instagram OAuth 시작 URL을 생성했습니다."
                : "Instagram OAuth 설정이 아직 비어 있습니다. clientId/redirectUri를 먼저 채워주세요.";

        return ResponseEntity.ok(new InstagramConnectStartResponse(
                principal.orgId(),
                configured,
                state,
                instagram.getRedirectUri(),
                instagram.getOauthAuthorizeUrl(),
                instagram.getScopes(),
                authorizationUrl,
                message
        ));
    }

    @GetMapping("/instagram/callback")
    public ResponseEntity<InstagramConnectCallbackResponse> instagramCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_reason", required = false) String errorReason,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @RequestParam(name = "error_code", required = false) Integer errorCode,
            @RequestParam(name = "error_message", required = false) String errorMessage,
            @RequestParam(required = false) String externalAccountId,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String accessToken,
            @RequestParam(required = false) Long expiresIn
    ) {
        if (errorCode != null
                || (errorMessage != null && !errorMessage.isBlank())
                || (error != null && !error.isBlank())
                || (errorDescription != null && !errorDescription.isBlank())) {
            return ResponseEntity.badRequest().body(new InstagramConnectCallbackResponse(
                    null,
                    null,
                    ChannelProvider.INSTAGRAM.name(),
                    ConnectedChannelStatus.PENDING.name(),
                    null,
                    null,
                    null,
                    "Instagram 연결이 Meta 측에서 거부되었습니다.",
                    errorCode,
                    firstNonBlank(errorMessage, errorDescription, errorReason, error)
            ));
        }

        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().body(new InstagramConnectCallbackResponse(
                    null,
                    null,
                    ChannelProvider.INSTAGRAM.name(),
                    ConnectedChannelStatus.PENDING.name(),
                    null,
                    null,
                    null,
                    "state 파라미터가 없어 Instagram 연결을 완료할 수 없습니다.",
                    null,
                    null
            ));
        }

        ConnectedChannel channel = connectedChannels.findByOauthState(state)
                .orElseThrow(() -> new NotFoundException("CHANNEL_STATE_NOT_FOUND", "연결 상태를 찾을 수 없습니다."));

        DmSenderProperties.Channel instagram = dmSenderProperties.getInstagram();
        OffsetDateTime now = OffsetDateTime.now();
        String resolvedAccessToken = firstNonBlank(accessToken, code);
        Long resolvedExpiresIn = expiresIn;

        if (code != null && !code.isBlank() && instagram.getClientSecret() != null && !instagram.getClientSecret().isBlank()) {
            try {
                OAuthTokenResponse tokenResponse = exchangeInstagramCodeForToken(instagram, code);
                resolvedAccessToken = firstNonBlank(tokenResponse.accessToken(), resolvedAccessToken);
                resolvedExpiresIn = tokenResponse.expiresIn() != null ? tokenResponse.expiresIn() : resolvedExpiresIn;
            } catch (RestClientException ex) {
                return ResponseEntity.badRequest().body(new InstagramConnectCallbackResponse(
                        channel.getId(),
                        channel.getOrganizationId(),
                        ChannelProvider.INSTAGRAM.name(),
                        ConnectedChannelStatus.PENDING.name(),
                        null,
                        null,
                        null,
                        "Instagram 토큰 교환에 실패했습니다.",
                        null,
                        ex.getMessage()
                ));
            }
        }

        channel.setStatus(ConnectedChannelStatus.ACTIVE);
        channel.setOauthState(null);
        channel.setExternalAccountId(firstNonBlank(externalAccountId, "ig-" + channel.getId().toString().substring(0, 8)));
        channel.setAccountName(firstNonBlank(accountName, username, "Instagram Business"));
        channel.setUsername(firstNonBlank(username, accountName, "instagram_user"));
        channel.setAccessToken(resolvedAccessToken);
        channel.setTokenExpiresAt(resolvedExpiresIn == null ? null : now.plusSeconds(resolvedExpiresIn));
        channel.setWebhookSubscribed(true);
        channel.setConnectedAt(now);
        channel.setDisconnectedAt(null);
        channel.setUpdatedAt(now);
        connectedChannels.save(channel);

        return ResponseEntity.ok(new InstagramConnectCallbackResponse(
                channel.getId(),
                channel.getOrganizationId(),
                channel.getProvider().name(),
                channel.getStatus().name(),
                channel.getAccountName(),
                channel.getUsername(),
                channel.getExternalAccountId(),
                "Instagram 채널 연결이 완료되었습니다.",
                null,
                null
        ));
    }

    @PostMapping("/{channelId}/sync-instagram")
    public ResponseEntity<InstagramChannelSyncResponse> syncInstagramChannel(@PathVariable UUID channelId,
                                                                             Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        assertOwner(principal.userId(), principal.orgId());

        ConnectedChannel channel = connectedChannels.findByIdAndOrganizationId(channelId, principal.orgId())
                .orElseThrow(() -> new NotFoundException("CHANNEL_NOT_FOUND", "채널을 찾을 수 없습니다."));

        if (channel.getProvider() != ChannelProvider.INSTAGRAM) {
            throw new IllegalArgumentException("Instagram 채널만 동기화할 수 있습니다.");
        }
        if (channel.getAccessToken() == null || channel.getAccessToken().isBlank()) {
            return ResponseEntity.badRequest().body(new InstagramChannelSyncResponse(
                    channel.getId(),
                    channel.getOrganizationId(),
                    channel.getProvider().name(),
                    channel.getExternalAccountId(),
                    channel.getAccountName(),
                    channel.getUsername(),
                    null,
                    null,
                    null,
                    null,
                    0,
                    List.of(),
                    null,
                    channel.getTokenExpiresAt(),
                    "저장된 Instagram access token이 없어 계정 동기화를 할 수 없습니다."
            ));
        }

        try {
            TokenUserResponse tokenUser = fetchTokenUser(channel.getAccessToken());
            DebugTokenEnvelope debugTokenEnvelope = fetchDebugToken(channel.getAccessToken());
            PageAccountsResponse pages = fetchManagedPages(channel.getAccessToken());
            List<PageAccount> pageList = pages.data() == null ? List.of() : pages.data();
            DebugTokenSummaryData tokenSummary = extractTargetIds(debugTokenEnvelope);

            if (pageList.isEmpty() && tokenSummary.pageId() != null) {
                PageDetailsResponse fallbackPage = fetchPageDetails(tokenSummary.pageId(), channel.getAccessToken());
                InstagramBusinessAccount fallbackIg = tokenSummary.instagramAccountId() != null
                        ? fetchInstagramAccountDetails(tokenSummary.instagramAccountId(), channel.getAccessToken())
                        : fallbackPage.instagramBusinessAccount();
                pageList = List.of(new PageAccount(
                        fallbackPage.id(),
                        fallbackPage.name(),
                        fallbackIg
                ));
            }

            List<InstagramChannelSyncResponse.PageSummary> pageSummaries = pageList.stream()
                    .map(page -> new InstagramChannelSyncResponse.PageSummary(
                            page.id(),
                            page.name(),
                            page.instagramBusinessAccount() != null ? page.instagramBusinessAccount().id() : null,
                            page.instagramBusinessAccount() != null ? page.instagramBusinessAccount().username() : null
                    ))
                    .toList();
            PageAccount page = pageList.isEmpty() ? null : pageList.get(0);
            InstagramBusinessAccount igAccount = page != null ? page.instagramBusinessAccount() : null;

            if (page != null) {
                channel.setAccountName(firstNonBlank(channel.getAccountName(), page.name(), "Instagram Business"));
            }
            if (igAccount != null) {
                channel.setExternalAccountId(firstNonBlank(igAccount.id(), channel.getExternalAccountId()));
                channel.setUsername(firstNonBlank(igAccount.username(), channel.getUsername()));
                channel.setAccountName(firstNonBlank(igAccount.name(), channel.getAccountName(), page != null ? page.name() : null, "Instagram Business"));
            }
            channel.setUpdatedAt(OffsetDateTime.now());
            connectedChannels.save(channel);

            return ResponseEntity.ok(new InstagramChannelSyncResponse(
                    channel.getId(),
                    channel.getOrganizationId(),
                    channel.getProvider().name(),
                    channel.getExternalAccountId(),
                    channel.getAccountName(),
                    channel.getUsername(),
                    tokenUser != null ? tokenUser.id() : null,
                    tokenUser != null ? tokenUser.name() : null,
                    page != null ? page.id() : null,
                    page != null ? page.name() : null,
                    pageList.size(),
                    pageSummaries,
                    toDebugTokenSummary(debugTokenEnvelope),
                    channel.getTokenExpiresAt(),
                    igAccount != null
                            ? "Instagram 비즈니스 계정 정보를 동기화했습니다."
                            : pageList.isEmpty()
                            ? "토큰은 유효하지만 조회 가능한 Facebook 페이지가 없습니다."
                            : "페이지 정보는 확인됐지만 연결된 Instagram 비즈니스 계정은 찾지 못했습니다."
            ));
        } catch (RestClientException ex) {
            return ResponseEntity.badRequest().body(new InstagramChannelSyncResponse(
                    channel.getId(),
                    channel.getOrganizationId(),
                    channel.getProvider().name(),
                    channel.getExternalAccountId(),
                    channel.getAccountName(),
                    channel.getUsername(),
                    null,
                    null,
                    null,
                    null,
                    0,
                    List.of(),
                    null,
                    channel.getTokenExpiresAt(),
                    "Instagram 계정 동기화에 실패했습니다: " + ex.getMessage()
            ));
        }
    }

    private OAuthTokenResponse exchangeInstagramCodeForToken(DmSenderProperties.Channel instagram, String code) {
        return dmRestClient.post()
                .uri(UriComponentsBuilder.fromUriString(instagram.getOauthTokenUrl())
                        .queryParam("client_id", instagram.getClientId())
                        .queryParam("client_secret", instagram.getClientSecret())
                        .queryParam("redirect_uri", instagram.getRedirectUri())
                        .queryParam("code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build()
                        .toUri())
                .retrieve()
                .body(OAuthTokenResponse.class);
    }

    private PageAccountsResponse fetchManagedPages(String accessToken) {
        String responseBody = dmRestClient.get()
                .uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/v23.0/me/accounts")
                        .queryParam("fields", "id,name,instagram_business_account{id,username,name}")
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUri())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(responseBody, PageAccountsResponse.class);
        } catch (Exception ex) {
            throw new RestClientException("Meta 페이지 응답 파싱에 실패했습니다.", ex);
        }
    }

    private PageDetailsResponse fetchPageDetails(String pageId, String accessToken) {
        String responseBody = dmRestClient.get()
                .uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/v23.0/" + pageId)
                        .queryParam("fields", "id,name,instagram_business_account{id,username,name}")
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUri())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(responseBody, PageDetailsResponse.class);
        } catch (Exception ex) {
            throw new RestClientException("Meta 페이지 상세 응답 파싱에 실패했습니다.", ex);
        }
    }

    private InstagramBusinessAccount fetchInstagramAccountDetails(String instagramAccountId, String accessToken) {
        String responseBody = dmRestClient.get()
                .uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/v23.0/" + instagramAccountId)
                        .queryParam("fields", "id,username,name")
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUri())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(responseBody, InstagramBusinessAccount.class);
        } catch (Exception ex) {
            throw new RestClientException("Meta Instagram 계정 상세 응답 파싱에 실패했습니다.", ex);
        }
    }

    private TokenUserResponse fetchTokenUser(String accessToken) {
        String responseBody = dmRestClient.get()
                .uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/v23.0/me")
                        .queryParam("fields", "id,name")
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUri())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(responseBody, TokenUserResponse.class);
        } catch (Exception ex) {
            throw new RestClientException("Meta 사용자 응답 파싱에 실패했습니다.", ex);
        }
    }

    private DebugTokenEnvelope fetchDebugToken(String accessToken) {
        DmSenderProperties.Channel instagram = dmSenderProperties.getInstagram();
        if (instagram.getClientId() == null || instagram.getClientId().isBlank()
                || instagram.getClientSecret() == null || instagram.getClientSecret().isBlank()) {
            return null;
        }

        String appAccessToken = instagram.getClientId() + "|" + instagram.getClientSecret();
        String responseBody = dmRestClient.get()
                .uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/debug_token")
                        .queryParam("input_token", accessToken)
                        .queryParam("access_token", appAccessToken)
                        .build()
                        .toUri())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(responseBody, DebugTokenEnvelope.class);
        } catch (Exception ex) {
            throw new RestClientException("Meta debug_token 응답 파싱에 실패했습니다.", ex);
        }
    }

    private InstagramChannelSyncResponse.DebugTokenSummary toDebugTokenSummary(DebugTokenEnvelope envelope) {
        if (envelope == null || envelope.data() == null) {
            return null;
        }
        DebugTokenData data = envelope.data();
        return new InstagramChannelSyncResponse.DebugTokenSummary(
                data.appId(),
                data.type(),
                data.application(),
                Boolean.TRUE.equals(data.isValid()),
                data.expiresAt(),
                data.scopes() == null ? List.of() : data.scopes(),
                data.userId(),
                data.raw()
        );
    }

    private DebugTokenSummaryData extractTargetIds(DebugTokenEnvelope envelope) {
        if (envelope == null || envelope.data() == null || envelope.data().granularScopes() == null) {
            return new DebugTokenSummaryData(null, null);
        }

        String pageId = null;
        String instagramAccountId = null;
        for (GranularScope scope : envelope.data().granularScopes()) {
            if (scope == null || scope.targetIds() == null || scope.targetIds().isEmpty()) {
                continue;
            }
            if ("pages_show_list".equals(scope.scope()) || "pages_read_engagement".equals(scope.scope())) {
                pageId = firstNonBlank(pageId, scope.targetIds().get(0));
            }
            if ("instagram_basic".equals(scope.scope()) || "instagram_manage_comments".equals(scope.scope())) {
                instagramAccountId = firstNonBlank(instagramAccountId, scope.targetIds().get(0));
            }
        }
        return new DebugTokenSummaryData(pageId, instagramAccountId);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<ConnectedChannelResponse> disconnect(@PathVariable UUID channelId,
                                                               Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        ConnectedChannel channel = connectedChannels.findById(channelId)
                .orElseThrow(() -> new NotFoundException("CHANNEL_NOT_FOUND", "채널을 찾을 수 없습니다."));

        if (!channel.getOrganizationId().equals(principal.orgId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        assertOwner(principal.userId(), principal.orgId());

        channel.setStatus(ConnectedChannelStatus.DISCONNECTED);
        channel.setWebhookSubscribed(false);
        channel.setDisconnectedAt(OffsetDateTime.now());
        channel.setUpdatedAt(OffsetDateTime.now());
        connectedChannels.save(channel);
        return ResponseEntity.ok(toResponse(channel));
    }

    private ConnectedChannelResponse toResponse(ConnectedChannel channel) {
        return new ConnectedChannelResponse(
                channel.getId(),
                channel.getOrganizationId(),
                channel.getProvider(),
                channel.getStatus(),
                channel.getExternalAccountId(),
                channel.getAccountName(),
                channel.getUsername(),
                channel.isWebhookSubscribed(),
                channel.getTokenExpiresAt(),
                channel.getConnectedAt(),
                channel.getDisconnectedAt()
        );
    }

    private void assertOwner(UUID userId, UUID orgId) {
        boolean isOwner = memberships.existsByUserIdAndOrganizationIdAndRole(
                userId,
                orgId,
                OrganizationMembership.OrgRole.OWNER
        );
        if (!isOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record OAuthTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token")
            String accessToken,
            @com.fasterxml.jackson.annotation.JsonProperty("token_type")
            String tokenType,
            @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
            Long expiresIn
    ) {
    }

    private record PageAccountsResponse(
            java.util.List<PageAccount> data,
            Map<String, Object> paging
    ) {
    }

    private record PageAccount(
            String id,
            String name,
            @com.fasterxml.jackson.annotation.JsonProperty("instagram_business_account")
            InstagramBusinessAccount instagramBusinessAccount
    ) {
    }

    private record InstagramBusinessAccount(
            String id,
            String username,
            String name
    ) {
    }

    private record TokenUserResponse(
            String id,
            String name
    ) {
    }

    private record DebugTokenEnvelope(
            DebugTokenData data
    ) {
    }

    private record DebugTokenData(
            @com.fasterxml.jackson.annotation.JsonProperty("app_id")
            String appId,
            String type,
            String application,
            @com.fasterxml.jackson.annotation.JsonProperty("is_valid")
            Boolean isValid,
            @com.fasterxml.jackson.annotation.JsonProperty("expires_at")
            Long expiresAt,
            List<String> scopes,
            @com.fasterxml.jackson.annotation.JsonProperty("user_id")
            String userId,
            @com.fasterxml.jackson.annotation.JsonProperty("granular_scopes")
            List<GranularScope> granularScopes,
            Map<String, Object> raw
    ) {
        public DebugTokenData {
            raw = raw == null ? new java.util.LinkedHashMap<>() : raw;
        }
    }

    private record GranularScope(
            String scope,
            @com.fasterxml.jackson.annotation.JsonProperty("target_ids")
            List<String> targetIds
    ) {
    }

    private record DebugTokenSummaryData(
            String pageId,
            String instagramAccountId
    ) {
    }

    private record PageDetailsResponse(
            String id,
            String name,
            @com.fasterxml.jackson.annotation.JsonProperty("instagram_business_account")
            InstagramBusinessAccount instagramBusinessAccount
    ) {
    }
}

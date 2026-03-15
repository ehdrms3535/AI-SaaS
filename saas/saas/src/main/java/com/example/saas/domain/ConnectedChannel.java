package com.example.saas.domain;

import com.example.saas.channel.ChannelProvider;
import com.example.saas.channel.ConnectedChannelStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "connected_channels")
public class ConnectedChannel {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChannelProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConnectedChannelStatus status;

    @Column(name = "external_account_id")
    private String externalAccountId;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "username")
    private String username;

    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "webhook_subscribed", nullable = false)
    private boolean webhookSubscribed;

    @Column(name = "oauth_state")
    private String oauthState;

    @Column(name = "connected_at")
    private OffsetDateTime connectedAt;

    @Column(name = "disconnected_at")
    private OffsetDateTime disconnectedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

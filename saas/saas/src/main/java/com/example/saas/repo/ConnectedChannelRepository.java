package com.example.saas.repo;

import com.example.saas.channel.ChannelProvider;
import com.example.saas.domain.ConnectedChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedChannelRepository extends JpaRepository<ConnectedChannel, UUID> {
    List<ConnectedChannel> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<ConnectedChannel> findFirstByOrganizationIdAndProviderOrderByCreatedAtDesc(UUID organizationId, ChannelProvider provider);
    Optional<ConnectedChannel> findFirstByProviderAndExternalAccountIdAndStatusOrderByCreatedAtDesc(
            ChannelProvider provider,
            String externalAccountId,
            com.example.saas.channel.ConnectedChannelStatus status
    );
    Optional<ConnectedChannel> findByOauthState(String oauthState);
    Optional<ConnectedChannel> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

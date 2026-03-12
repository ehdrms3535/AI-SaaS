package com.example.saas.repo;

import com.example.saas.domain.DmMessage;
import com.example.saas.domain.DmMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DmMessageRepository extends JpaRepository<DmMessage, UUID> {
    List<DmMessage> findByOrganizationIdOrderByReceivedAtDesc(UUID organizationId);
    List<DmMessage> findByOrganizationIdAndStatusOrderByReceivedAtDesc(UUID organizationId, DmMessageStatus status);
}

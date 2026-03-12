package com.example.saas.dm;

import com.example.saas.dm.dto.DmMessageResponse;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.domain.DmMessage;
import com.example.saas.domain.DmMessageStatus;
import com.example.saas.repo.DmMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DmMessageService {

    private final DmMessageRepository dmMessageRepository;
    private final DmAutoReservationService dmAutoReservationService;
    private final Clock clock;

    @Transactional
    public DmReservationResponse processAutoReserve(UUID orgId, UUID actorUserId, DmReservationRequest req) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        DmMessage message = new DmMessage();
        message.setId(UUID.randomUUID());
        message.setOrganizationId(orgId);
        message.setChannel("MANUAL_SIM");
        message.setSenderName(req.senderName());
        message.setSenderPhone(req.senderPhone());
        message.setCustomerHint(req.customerHint());
        message.setServiceHint(req.serviceHint());
        message.setMessageText(req.message());
        message.setStatus(DmMessageStatus.RECEIVED);
        message.setReceivedAt(now);
        message.setCreatedAt(now);
        dmMessageRepository.save(message);

        DmReservationResponse response = dmAutoReservationService.reserve(orgId, actorUserId, req);

        message.setStatus(response.success() ? DmMessageStatus.RESERVED : DmMessageStatus.NEEDS_REVIEW);
        message.setCustomerId(response.customerId());
        message.setReservationId(response.reservationId());
        message.setFailureReason(response.success() ? null : response.reply());
        message.setProcessedAt(OffsetDateTime.now(clock));
        dmMessageRepository.save(message);

        return response;
    }

    @Transactional(readOnly = true)
    public List<DmMessageResponse> list(UUID orgId, String status) {
        if (status == null || status.isBlank()) {
            return dmMessageRepository.findByOrganizationIdOrderByReceivedAtDesc(orgId).stream()
                    .map(DmMessageResponse::from)
                    .toList();
        }
        DmMessageStatus parsed = DmMessageStatus.valueOf(status.trim().toUpperCase());
        return dmMessageRepository.findByOrganizationIdAndStatusOrderByReceivedAtDesc(orgId, parsed).stream()
                .map(DmMessageResponse::from)
                .toList();
    }
}

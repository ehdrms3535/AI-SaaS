package com.example.saas.dm;

import com.example.saas.api.error.NotFoundException;
import com.example.saas.dm.dto.DmMessageResponse;
import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.InboundDmWebhookRequest;
import com.example.saas.dm.dto.ManualDmCancelRequest;
import com.example.saas.dm.dto.ManualDmReservationRequest;
import com.example.saas.dm.dto.ManualDmUpdateRequest;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.dm.dto.DmWebhookInboundResponse;
import com.example.saas.domain.DmMessage;
import com.example.saas.domain.DmMessageStatus;
import com.example.saas.domain.Customer;
import com.example.saas.repo.DmMessageRepository;
import com.example.saas.repo.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DmMessageService {

    private final DmMessageRepository dmMessageRepository;
    private final DmAutoReservationService dmAutoReservationService;
    private final CustomerRepository customerRepository;
    private final DmReplyTemplateService dmReplyTemplateService;
    private final RoutingDmChannelSender dmChannelSender;
    private final Clock clock;

    @Transactional
    public DmReservationResponse processAutoReserve(UUID orgId, UUID actorUserId, DmReservationRequest req) {
        return processAutoReserve(orgId, actorUserId, "MANUAL_SIM", null, req);
    }

    @Transactional
    public DmWebhookInboundResponse processWebhook(InboundDmWebhookRequest req) {
        UUID actorUserId = req.actorUserId() != null ? req.actorUserId() : UUID.randomUUID();
        DmReservationRequest reservationRequest = new DmReservationRequest(
                req.message(),
                req.customerHint(),
                req.senderName(),
                req.senderPhone(),
                req.serviceHint(),
                req.durationMinutes()
        );
        DmReservationResponse result = processAutoReserve(
                req.organizationId(),
                actorUserId,
                req.channel(),
                req.senderChannelId(),
                reservationRequest
        );
        DmChannelReplyPayload replyPayload = new DmChannelReplyPayload(
                req.organizationId(),
                req.channel(),
                req.senderChannelId(),
                resolveReplyText(req.organizationId(), result),
                dmReplyTemplateService.suggestionQuickReplies(result.suggestedStartTimes()),
                buildReplyMetadata(result)
        );
        return new DmWebhookInboundResponse(
                result,
                replyPayload,
                dmChannelSender.send(replyPayload)
        );
    }

    private DmReservationResponse processAutoReserve(UUID orgId,
                                                     UUID actorUserId,
                                                     String channel,
                                                     String senderChannelId,
                                                     DmReservationRequest req) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        DmMessage message = new DmMessage();
        message.setId(UUID.randomUUID());
        message.setOrganizationId(orgId);
        message.setChannel(channel);
        message.setSenderChannelId(senderChannelId);
        message.setSenderName(req.senderName());
        message.setSenderPhone(req.senderPhone());
        message.setCustomerHint(req.customerHint());
        message.setServiceHint(req.serviceHint());
        message.setMessageText(req.message());
        message.setIntent(dmAutoReservationService.inferIntent(req.message()));
        message.setStatus(DmMessageStatus.RECEIVED);
        message.setReceivedAt(now);
        message.setCreatedAt(now);
        dmMessageRepository.save(message);

        DmReservationResponse response = dmAutoReservationService.reserve(orgId, actorUserId, req);

        message.setStatus(response.success() ? DmMessageStatus.RESERVED : DmMessageStatus.NEEDS_REVIEW);
        message.setCustomerId(response.customerId());
        message.setReservationId(response.reservationId());
        message.setFailureReason(response.success() ? null : response.reply());
        message.setReplyText(response.success() ? response.reply() : dmReplyTemplateService.bookingNeedsReview());
        message.setParsedStartAt(response.startAt());
        message.setParsedEndAt(response.endAt());
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

    @Transactional(readOnly = true)
    public DmMessageResponse get(UUID orgId, UUID messageId) {
        DmMessage message = dmMessageRepository.findByIdAndOrganizationId(messageId, orgId)
                .orElseThrow(() -> new NotFoundException("DM_MESSAGE_NOT_FOUND", "DM 메시지를 찾을 수 없습니다."));
        return DmMessageResponse.from(message);
    }

    @Transactional
    public DmMessageResponse confirmReservation(UUID orgId, UUID actorUserId, UUID messageId, ManualDmReservationRequest req) {
        DmMessage message = dmMessageRepository.findByIdAndOrganizationId(messageId, orgId)
                .orElseThrow(() -> new NotFoundException("DM_MESSAGE_NOT_FOUND", "DM 메시지를 찾을 수 없습니다."));

        UUID reservationId = dmAutoReservationService.confirmManualReservation(
                orgId,
                actorUserId,
                req.customerId(),
                req.serviceId(),
                req.startAt(),
                req.endAt(),
                buildManualReviewNotes(message, req.notes())
        );

        message.setStatus(DmMessageStatus.RESERVED);
        message.setCustomerId(req.customerId());
        message.setReservationId(reservationId);
        message.setFailureReason(null);
        message.setReplyText(dmReplyTemplateService.bookingConfirmed(resolveCustomerName(orgId, req.customerId()), req.startAt()));
        message.setProcessedAt(OffsetDateTime.now(clock));
        dmMessageRepository.save(message);
        return DmMessageResponse.from(message);
    }

    @Transactional
    public DmMessageResponse cancelReservation(UUID orgId, UUID actorUserId, UUID messageId, ManualDmCancelRequest req) {
        DmMessage message = dmMessageRepository.findByIdAndOrganizationId(messageId, orgId)
                .orElseThrow(() -> new NotFoundException("DM_MESSAGE_NOT_FOUND", "DM 메시지를 찾을 수 없습니다."));
        dmAutoReservationService.cancelManualReservation(orgId, actorUserId, req.reservationId());
        message.setReservationId(req.reservationId());
        message.setFailureReason(null);
        message.setReplyText(dmReplyTemplateService.bookingCanceled(resolveCustomerName(orgId, message.getCustomerId())));
        message.setStatus(DmMessageStatus.RESERVED);
        message.setProcessedAt(OffsetDateTime.now(clock));
        dmMessageRepository.save(message);
        return DmMessageResponse.from(message);
    }

    @Transactional
    public DmMessageResponse updateReservation(UUID orgId, UUID actorUserId, UUID messageId, ManualDmUpdateRequest req) {
        DmMessage message = dmMessageRepository.findByIdAndOrganizationId(messageId, orgId)
                .orElseThrow(() -> new NotFoundException("DM_MESSAGE_NOT_FOUND", "DM 메시지를 찾을 수 없습니다."));
        dmAutoReservationService.updateManualReservation(
                orgId,
                actorUserId,
                req.reservationId(),
                req.customerId(),
                req.serviceId(),
                req.startAt(),
                req.endAt(),
                buildManualReviewNotes(message, req.notes())
        );
        message.setCustomerId(req.customerId());
        message.setReservationId(req.reservationId());
        message.setFailureReason(null);
        message.setReplyText(dmReplyTemplateService.bookingUpdated(resolveCustomerName(orgId, req.customerId()), req.startAt()));
        message.setParsedStartAt(req.startAt());
        message.setParsedEndAt(req.endAt());
        message.setStatus(DmMessageStatus.RESERVED);
        message.setProcessedAt(OffsetDateTime.now(clock));
        dmMessageRepository.save(message);
        return DmMessageResponse.from(message);
    }

    private String buildManualReviewNotes(DmMessage message, String notes) {
        String prefix = "[DM REVIEW] " + message.getMessageText();
        if (notes == null || notes.isBlank()) {
            return prefix;
        }
        return prefix + "\n" + notes.trim();
    }

    private String resolveCustomerName(UUID orgId, UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findByIdAndOrganizationId(customerId, orgId)
                .map(Customer::getName)
                .orElse(null);
    }

    private String resolveReplyText(UUID orgId, DmReservationResponse response) {
        if (response.success()) {
            return response.reply();
        }
        if (response.suggestedStartTimes() != null && !response.suggestedStartTimes().isEmpty()) {
            return response.reply();
        }
        return dmReplyTemplateService.bookingNeedsReview();
    }

    private Map<String, Object> buildReplyMetadata(DmReservationResponse result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("success", result.success());
        metadata.put("customerId", result.customerId());
        metadata.put("reservationId", result.reservationId());
        metadata.put("serviceId", result.serviceId());
        metadata.put("startAt", result.startAt());
        metadata.put("endAt", result.endAt());
        return metadata;
    }
}

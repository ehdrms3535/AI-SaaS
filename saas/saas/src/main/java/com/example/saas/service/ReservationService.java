package com.example.saas.service;

import com.example.saas.billing.PlanLimitService;
import com.example.saas.api.error.ConflictException;
import com.example.saas.api.error.NotFoundException;
import com.example.saas.api.error.ReservationConflictException;
import com.example.saas.domain.Reservation;
import com.example.saas.domain.ReservationSource;
import com.example.saas.domain.ReservationStatus;
import com.example.saas.repo.CustomerLookupRepository;
import com.example.saas.repo.ReservationLockRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceLookupRepository;
import com.example.saas.reservation.ReservationResponse;
import com.example.saas.reservation.ReservationUpdateRequest;
import com.example.saas.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final PlanLimitService planLimitService;
    private final CustomerLookupRepository customerLookupRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationLockRepository reservationLockRepository; // advisory lock용 (jdbc/native)
    private final ServiceLookupRepository serviceLookupRepository;
    private final Clock clock;

    @Transactional
    public UUID create(UUID orgId,
                       UUID customerId,
                       UUID serviceId,
                       OffsetDateTime startAt,
                       OffsetDateTime endAt,
                       UUID createdByUserId,
                       String notes) {
        return create(orgId, customerId, serviceId, startAt, endAt, createdByUserId, notes, ReservationSource.MANUAL);
    }

    @Transactional
    public UUID create(UUID orgId,
                       UUID customerId,
                       UUID serviceId,
                       OffsetDateTime startAt,
                       OffsetDateTime endAt,
                       UUID createdByUserId,
                       String notes,
                       ReservationSource source) {

        validateTime(startAt, endAt);
        validateCustomerAndService(orgId, customerId, serviceId);
        planLimitService.assertCanCreateReservation(orgId);

        // ✅ 0) 빈 구간 레이스까지 막는 트랜잭션 락 (org + time window 기준)
        String lockKey = "reservation:" + orgId + ":" + startAt.toInstant() + ":" + endAt.toInstant();
        reservationRepository.xactLock(lockKey);

        // 1) 겹침 체크 (이제 레이스가 사실상 사라짐)
        List<Reservation> overlaps =
                reservationRepository.lockOverlapsForUpdate(orgId, startAt, endAt);

        if (!overlaps.isEmpty()) {
            throw new ReservationConflictException("해당 시간대에 이미 예약이 존재합니다.");
        }

        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setOrganizationId(orgId);
        r.setCustomerId(customerId);
        r.setServiceId(serviceId);
        r.setStartAt(startAt);
        r.setEndAt(endAt);
        r.setCreatedByUserId(createdByUserId);
        r.setNotes(notes);

        r.setStatus(ReservationStatus.CONFIRMED); // 또는 BOOKED 같은 네 enum 값
        r.setSource(source == null ? ReservationSource.MANUAL : source);
        reservationRepository.save(r);
        return r.getId();
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByOrganization(UUID orgId) {
        return reservationRepository.findByOrganizationIdOrderByStartAtAsc(orgId);
    }

    private void validateTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt / endAt 은 필수입니다.");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
    }

    private void validateCustomerAndService(UUID orgId, UUID customerId, UUID serviceId) {
        if (!customerLookupRepository.existsByIdAndOrganizationId(customerId, orgId)) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다.");
        }
        if (serviceId != null && !serviceLookupRepository.existsByIdAndOrganizationId(serviceId, orgId)) {
            throw new NotFoundException("SERVICE_NOT_FOUND", "서비스를 찾을 수 없습니다.");
        }
    }

    @Transactional
    public ReservationResponse update(UUID reservationId, ReservationUpdateRequest req, JwtPrincipal principal) {
        UUID orgId = principal.orgId();

        // 1) 대상 예약 락
        Reservation r = reservationRepository.findByIdForUpdate(reservationId, orgId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."));

        if (r.getCanceledAt() != null) {
            throw new ConflictException("RESERVATION_CANCELED", "이미 취소된 예약입니다.");
        }

        // 2) 수정값 계산 (null이면 기존 유지)
        UUID newCustomerId = req.customerId() != null ? req.customerId() : r.getCustomerId();
        UUID newServiceId  = req.serviceId()  != null ? req.serviceId()  : r.getServiceId();
        OffsetDateTime newStart = req.startAt() != null ? req.startAt() : r.getStartAt();
        OffsetDateTime newEnd   = req.endAt()   != null ? req.endAt()   : r.getEndAt();
        String newNotes = req.notes() != null ? req.notes() : r.getNotes();

        validateTime(newStart, newEnd);
        validateCustomerAndService(orgId, newCustomerId, newServiceId);

        // 3) advisory lock (선택이지만 추천: 생성/수정 공통)
        // 키 설계는 너가 쓰는 방식 그대로 가져가도 됨
        String lockKey = "reservation:%s:%s:%s".formatted(orgId, newStart.toInstant(), newEnd.toInstant());
        reservationLockRepository.lockTx(lockKey);

        // 4) 겹침 체크 (자기 자신 제외) + FOR UPDATE
        boolean overlap = reservationRepository.existsOverlapForUpdate(orgId, reservationId, newStart, newEnd).isPresent();
        if (overlap) {
            throw new ConflictException("RESERVATION_CONFLICT", "해당 시간대에 이미 예약이 존재합니다.");
        }

        // 5) 업데이트
        r.setCustomerId(newCustomerId);
        r.setServiceId(newServiceId);
        r.setStartAt(newStart);
        r.setEndAt(newEnd);
        r.setNotes(newNotes);

        // save 생략 가능(영속 상태), 그래도 명시해도 OK
        reservationRepository.save(r);

        return ReservationResponse.from(r);
    }

    @Transactional
    public void cancel(UUID reservationId, JwtPrincipal principal) {
        UUID orgId = principal.orgId();

        Reservation r = reservationRepository.findByIdForUpdate(reservationId, orgId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."));

        if (r.getCanceledAt() != null) return; // idempotent

        r.setCanceledAt(OffsetDateTime.now(clock));
        r.setCanceledByUserId(principal.userId());

        reservationRepository.save(r);
    }

    @Transactional
    public ReservationResponse restore(UUID reservationId, JwtPrincipal principal) {
        UUID orgId = principal.orgId();

        Reservation r = reservationRepository.findByIdForUpdate(reservationId, orgId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."));

        if (r.getCanceledAt() == null) {
            throw new ConflictException("RESERVATION_NOT_CANCELED", "취소된 예약만 복구할 수 있습니다.");
        }

        validateCustomerAndService(orgId, r.getCustomerId(), r.getServiceId());

        String lockKey = "reservation:%s:%s:%s".formatted(orgId, r.getStartAt().toInstant(), r.getEndAt().toInstant());
        reservationLockRepository.lockTx(lockKey);

        boolean overlap = reservationRepository.existsOverlapForUpdate(orgId, reservationId, r.getStartAt(), r.getEndAt()).isPresent();
        if (overlap) {
            throw new ConflictException("RESERVATION_CONFLICT", "해당 시간대에 이미 예약이 존재합니다.");
        }

        r.setCanceledAt(null);
        r.setCanceledByUserId(null);
        reservationRepository.save(r);

        return ReservationResponse.from(r);
    }

}

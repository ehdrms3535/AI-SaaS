package com.example.saas.service;

import com.example.saas.api.error.ReservationConflictException;
import com.example.saas.domain.Reservation;
import com.example.saas.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    /**
     * 레거시 컨트롤러용 (slug 기반)
     */
    @Transactional
    public UUID create(UUID orgId,
                       UUID customerId,
                       UUID serviceId,
                       OffsetDateTime startAt,
                       OffsetDateTime endAt,
                       UUID createdByUserId,
                       String notes) {

        validateTime(startAt, endAt);

        // 1️⃣ FOR UPDATE로 겹침 체크
        List<Reservation> overlaps =
                reservationRepository.lockOverlapsForUpdate(orgId, startAt, endAt);

        if (!overlaps.isEmpty()) {
            throw new ReservationConflictException("해당 시간대에 이미 예약이 존재합니다.");
        }

        // 2️⃣ 저장
        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setOrganizationId(orgId);
        r.setCustomerId(customerId);
        r.setServiceId(serviceId);
        r.setStartAt(startAt);
        r.setEndAt(endAt);
        r.setCreatedByUserId(createdByUserId);
        r.setNotes(notes);

        reservationRepository.save(r);

        return r.getId();
    }

    /**
     * 조직별 예약 조회
     */
    @Transactional(readOnly = true)
    public List<Reservation> findByOrganization(UUID orgId) {
        return reservationRepository.findByOrganizationIdOrderByStartAtAsc(orgId);
    }

    /**
     * 시간 검증
     */
    private void validateTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt / endAt 은 필수입니다.");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
    }
}
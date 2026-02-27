package com.example.saas.service;

import com.example.saas.domain.Reservation;
import com.example.saas.domain.ReservationSource;
import com.example.saas.domain.ReservationStatus;
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

    @Transactional
    public UUID create(UUID orgId,
                       UUID customerId,
                       UUID serviceId,
                       OffsetDateTime startAt,
                       OffsetDateTime endAt,
                       UUID createdByUserId,
                       String notes) {

        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        // 1) 겹치는 예약 row를 잠그면서 조회
        List<UUID> overlaps = reservationRepository.lockOverlappingReservationIds(orgId, startAt, endAt);

        // 2) 하나라도 있으면 충돌
        if (!overlaps.isEmpty()) {
            throw new com.example.saas.api.error.ReservationConflictException("해당 시간대에 이미 예약이 존재합니다.");
        }

        // 3) 없으면 생성
        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setOrganizationId(orgId);
        r.setCustomerId(customerId);
        r.setServiceId(serviceId);
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setStartAt(startAt);
        r.setEndAt(endAt);
        r.setSource(ReservationSource.MANUAL);
        r.setNotes(notes);
        r.setCreatedByUserId(createdByUserId);

        reservationRepository.save(r);
        return r.getId();
    }
}
package com.example.saas.service;

import com.example.saas.billing.PlanLimitService;
import com.example.saas.api.error.ConflictException;
import com.example.saas.api.error.NotFoundException;
import com.example.saas.domain.Reservation;
import com.example.saas.repo.CustomerLookupRepository;
import com.example.saas.repo.ReservationLockRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceLookupRepository;
import com.example.saas.reservation.ReservationResponse;
import com.example.saas.reservation.ReservationUpdateRequest;
import com.example.saas.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private CustomerLookupRepository customerLookupRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationLockRepository reservationLockRepository;

    @Mock
    private ServiceLookupRepository serviceLookupRepository;

    @Mock
    private PlanLimitService planLimitService;

    private Clock clock;

    private ReservationService reservationService;

    private UUID orgId;
    private UUID reservationId;
    private UUID customerId;
    private UUID userId;
    private JwtPrincipal principal;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC);
        reservationService = new ReservationService(
                planLimitService,
                customerLookupRepository,
                reservationRepository,
                reservationLockRepository,
                serviceLookupRepository,
                clock
        );

        orgId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        principal = new JwtPrincipal(userId, "tester@test.local", orgId);
    }

    @Test
    void updateChangesReservationWhenActive() {
        Reservation reservation = activeReservation();
        OffsetDateTime newStart = OffsetDateTime.parse("2026-03-11T10:00:00Z");
        OffsetDateTime newEnd = OffsetDateTime.parse("2026-03-11T11:00:00Z");
        ReservationUpdateRequest req = new ReservationUpdateRequest(customerId, null, newStart, newEnd, "updated");

        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));
        when(customerLookupRepository.existsByIdAndOrganizationId(customerId, orgId)).thenReturn(true);
        when(reservationRepository.existsOverlapForUpdate(orgId, reservationId, newStart, newEnd)).thenReturn(Optional.empty());

        ReservationResponse response = reservationService.update(reservationId, req, principal);

        assertThat(response.startAt()).isEqualTo(newStart);
        assertThat(response.endAt()).isEqualTo(newEnd);
        assertThat(response.notes()).isEqualTo("updated");
        verify(reservationLockRepository).lockTx(any());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void updateRejectsCanceledReservation() {
        Reservation reservation = activeReservation();
        reservation.setCanceledAt(OffsetDateTime.parse("2026-03-11T09:00:00Z"));

        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.update(
                reservationId,
                new ReservationUpdateRequest(null, null, null, null, "x"),
                principal
        )).isInstanceOf(ConflictException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    @Test
    void cancelSetsCanceledFields() {
        Reservation reservation = activeReservation();
        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));

        reservationService.cancel(reservationId, principal);

        assertThat(reservation.getCanceledAt()).isEqualTo(OffsetDateTime.now(clock));
        assertThat(reservation.getCanceledByUserId()).isEqualTo(userId);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void restoreClearsCanceledFieldsWhenNoOverlap() {
        Reservation reservation = activeReservation();
        reservation.setCanceledAt(OffsetDateTime.parse("2026-03-11T09:30:00Z"));
        reservation.setCanceledByUserId(UUID.randomUUID());

        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));
        when(customerLookupRepository.existsByIdAndOrganizationId(customerId, orgId)).thenReturn(true);
        when(reservationRepository.existsOverlapForUpdate(orgId, reservationId, reservation.getStartAt(), reservation.getEndAt()))
                .thenReturn(Optional.empty());

        ReservationResponse response = reservationService.restore(reservationId, principal);

        assertThat(response.canceledAt()).isNull();
        assertThat(response.canceledByUserId()).isNull();
        assertThat(reservation.getCanceledAt()).isNull();
        assertThat(reservation.getCanceledByUserId()).isNull();
        verify(reservationLockRepository).lockTx(any());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void restoreRejectsNonCanceledReservation() {
        Reservation reservation = activeReservation();
        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.restore(reservationId, principal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("취소된 예약만 복구할 수 있습니다.");
    }

    @Test
    void restoreRejectsWhenTimeSlotAlreadyTaken() {
        Reservation reservation = activeReservation();
        reservation.setCanceledAt(OffsetDateTime.parse("2026-03-11T09:30:00Z"));
        when(reservationRepository.findByIdForUpdate(reservationId, orgId)).thenReturn(Optional.of(reservation));
        when(customerLookupRepository.existsByIdAndOrganizationId(customerId, orgId)).thenReturn(true);
        when(reservationRepository.existsOverlapForUpdate(orgId, reservationId, reservation.getStartAt(), reservation.getEndAt()))
                .thenReturn(Optional.of(1));

        assertThatThrownBy(() -> reservationService.restore(reservationId, principal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("해당 시간대에 이미 예약이 존재합니다.");
    }

    @Test
    void createRejectsCustomerFromAnotherOrganization() {
        when(customerLookupRepository.existsByIdAndOrganizationId(customerId, orgId)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.create(
                orgId,
                customerId,
                null,
                OffsetDateTime.parse("2026-03-11T10:00:00Z"),
                OffsetDateTime.parse("2026-03-11T11:00:00Z"),
                userId,
                "memo"
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("고객을 찾을 수 없습니다.");

        verify(reservationRepository, never()).save(any());
    }

    private Reservation activeReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setOrganizationId(orgId);
        reservation.setCustomerId(customerId);
        reservation.setStartAt(OffsetDateTime.parse("2026-03-11T08:00:00Z"));
        reservation.setEndAt(OffsetDateTime.parse("2026-03-11T09:00:00Z"));
        reservation.setNotes("before");
        reservation.setCreatedByUserId(userId);
        return reservation;
    }
}

package com.example.saas.api;

import com.example.saas.api.error.BadRequestException;
import com.example.saas.api.error.NotFoundException;
import com.example.saas.domain.Reservation;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.reservation.ReservationResponse;
import com.example.saas.security.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReservationQueryController {

    private final ReservationRepository reservationRepository;

    public ReservationQueryController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> list(
            @RequestParam(required = false) OffsetDateTime startAt,
            @RequestParam(required = false) OffsetDateTime endAt,
            @RequestParam(defaultValue = "false") boolean includeCanceled,
            Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        UUID orgId = p.orgId();

        if ((startAt == null) != (endAt == null)) {
            throw new BadRequestException("INVALID_TIME_RANGE", "startAt와 endAt은 함께 전달해야 합니다.");
        }

        List<Reservation> reservations;
        if (startAt != null) {
            reservations = includeCanceled
                    ? reservationRepository.findByOrganizationIdAndTimeRangeIncludingCanceled(orgId, startAt, endAt)
                    : reservationRepository.findByOrganizationIdAndTimeRange(orgId, startAt, endAt);
        } else {
            reservations = includeCanceled
                    ? reservationRepository.findByOrganizationIdOrderByStartAtAsc(orgId)
                    : reservationRepository.findByOrganizationIdAndCanceledAtIsNullOrderByStartAtAsc(orgId);
        }

        return reservations.stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@PathVariable UUID id, Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        UUID orgId = p.orgId();

        Reservation r = reservationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."));

        return ReservationResponse.from(r);
    }
}

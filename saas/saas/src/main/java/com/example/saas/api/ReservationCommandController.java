package com.example.saas.api;

import com.example.saas.reservation.ReservationResponse;
import com.example.saas.reservation.ReservationUpdateRequest;
import com.example.saas.security.JwtPrincipal;
import com.example.saas.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReservationCommandController {

    private final ReservationService reservationService;

    public ReservationCommandController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public record CreateReservationRequest(
            @NotNull UUID customerId,
            @NotNull UUID serviceId,
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            String notes
    ) {}

    @PostMapping("/reservations")
    public Map<String, Object> create(@RequestBody @Valid CreateReservationRequest req,
                                      Authentication auth) {

        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();

        UUID id = reservationService.create(
                p.orgId(),
                req.customerId(),
                req.serviceId(),
                req.startAt(),
                req.endAt(),
                p.userId(),     // 🔥 createdBy는 JWT에서
                req.notes()
        );

        return Map.of("id", id);
    }

    @PatchMapping("/reservations/{id}")
    public ReservationResponse update(@PathVariable UUID id,
                                      @RequestBody @Valid ReservationUpdateRequest req,
                                      Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return reservationService.update(id, req, p);
    }

    @DeleteMapping("/reservations/{id}")
    public Map<String, Object> cancel(@PathVariable UUID id, Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        reservationService.cancel(id, p);
        return Map.of("success", true);
    }

    @PostMapping("/reservations/{id}/restore")
    public ReservationResponse restore(@PathVariable UUID id, Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return reservationService.restore(id, p);
    }
}

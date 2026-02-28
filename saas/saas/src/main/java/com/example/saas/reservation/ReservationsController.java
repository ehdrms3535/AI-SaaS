package com.example.saas.reservation;

import com.example.saas.security.JwtPrincipal;
import com.example.saas.service.ReservationService;
import com.example.saas.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationsController {

    private final ReservationService reservationService;

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CreateReservationRequest req,
                                      Authentication auth) {

        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();

        UUID orgId = TenantContext.getOrgId();
        if (orgId == null) {
            throw new IllegalArgumentException("조직이 선택되지 않았습니다.");
        }

        UUID id = reservationService.create(
                orgId,
                req.customerId(),
                req.serviceId(),
                req.startAt(),
                req.endAt(),
                p.userId(),
                req.notes()
        );

        return Map.of("id", id);
    }
}
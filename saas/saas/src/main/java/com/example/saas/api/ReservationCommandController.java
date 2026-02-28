package com.example.saas.api;

import com.example.saas.domain.Organization;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.reservation.ReservationResponse;
import com.example.saas.security.JwtPrincipal;
import com.example.saas.service.ReservationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReservationCommandController {

    private final OrganizationRepository organizationRepository;
    private final ReservationService reservationService;

    public ReservationCommandController(OrganizationRepository organizationRepository,
                                        ReservationService reservationService) {
        this.organizationRepository = organizationRepository;
        this.reservationService = reservationService;
    }

    public record CreateReservationRequest(
            @NotNull UUID customerId,
            UUID serviceId,
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            String notes
    ) {}

    @PostMapping("/orgs/{slug}/reservations")
    public Map<String, Object> create(@PathVariable String slug,
                                      @RequestBody CreateReservationRequest req,
                                      Authentication auth) {

        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();

        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("org not found: " + slug));

        UUID id = reservationService.create(
                org.getId(),
                req.customerId(),
                req.serviceId(),
                req.startAt(),
                req.endAt(),
                p.userId(),     // 🔥 createdBy는 JWT에서
                req.notes()
        );

        return Map.of("id", id);
    }
}
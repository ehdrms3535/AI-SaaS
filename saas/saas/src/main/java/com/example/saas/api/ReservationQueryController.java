package com.example.saas.api;

import com.example.saas.domain.Organization;
import com.example.saas.domain.Reservation;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.repo.ReservationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationQueryController {

    private final OrganizationRepository organizationRepository;
    private final ReservationRepository reservationRepository;

    public ReservationQueryController(OrganizationRepository organizationRepository,
                                      ReservationRepository reservationRepository) {
        this.organizationRepository = organizationRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/orgs/{slug}/reservations")
    public List<Reservation> list(@PathVariable String slug) {
        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("org not found: " + slug));

        return reservationRepository.findByOrganizationIdOrderByStartAtAsc(org.getId());
    }
}
package com.example.saas.api;

import com.example.saas.billing.PlanLimitService;
import com.example.saas.api.error.NotFoundException;
import com.example.saas.repo.ServiceRepository;
import com.example.saas.security.JwtPrincipal;
import com.example.saas.serviceapi.dto.CreateServiceRequest;
import com.example.saas.serviceapi.dto.ServiceResponse;
import com.example.saas.serviceapi.dto.UpdateServiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository services;
    private final PlanLimitService planLimitService;

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@RequestBody @Valid CreateServiceRequest req,
                                                  Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        planLimitService.assertCanCreateService(principal.orgId());

        com.example.saas.domain.Service service = new com.example.saas.domain.Service();
        service.setId(UUID.randomUUID());
        service.setOrganizationId(principal.orgId());
        service.setName(req.name());
        service.setDurationMinutes(req.durationMinutes());
        service.setPrice(req.price());
        service.setActive(req.active() != null ? req.active() : Boolean.TRUE);

        services.save(service);
        return ResponseEntity.status(201).body(ServiceResponse.from(service));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> list(@RequestParam(required = false) String q,
                                                      Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        List<com.example.saas.domain.Service> found = (q == null || q.isBlank())
                ? services.findByOrganizationIdOrderByCreatedAtDesc(principal.orgId())
                : services.searchByOrganizationId(principal.orgId(), q.trim());

        return ResponseEntity.ok(found.stream().map(ServiceResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> get(@PathVariable UUID id, Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        com.example.saas.domain.Service service = services.findByIdAndOrganizationId(id, principal.orgId())
                .orElseThrow(() -> new NotFoundException("SERVICE_NOT_FOUND", "서비스를 찾을 수 없습니다."));

        return ResponseEntity.ok(ServiceResponse.from(service));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceResponse> update(@PathVariable UUID id,
                                                  @RequestBody @Valid UpdateServiceRequest req,
                                                  Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();

        com.example.saas.domain.Service service = services.findByIdAndOrganizationId(id, principal.orgId())
                .orElseThrow(() -> new NotFoundException("SERVICE_NOT_FOUND", "서비스를 찾을 수 없습니다."));

        if (req.name() != null) {
            service.setName(req.name());
        }
        if (req.durationMinutes() != null) {
            service.setDurationMinutes(req.durationMinutes());
        }
        if (req.price() != null) {
            service.setPrice(req.price());
        }
        if (req.active() != null) {
            service.setActive(req.active());
        }

        services.save(service);
        return ResponseEntity.ok(ServiceResponse.from(service));
    }
}

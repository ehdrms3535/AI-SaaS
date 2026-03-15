package com.example.saas.api;

import com.example.saas.api.error.NotFoundException;
import com.example.saas.billing.OrganizationPlan;
import com.example.saas.billing.PlanLimitService;
import com.example.saas.org.OrganizationMembership;
import com.example.saas.org.OrganizationMembershipRepository;
import com.example.saas.org.dto.CreateOrganizationRequest;
import com.example.saas.org.dto.OrganizationResponse;
import com.example.saas.org.dto.OrganizationScheduleResponse;
import com.example.saas.org.dto.UpdateOrganizationPlanRequest;
import com.example.saas.org.dto.UpdateOrganizationScheduleRequest;
import com.example.saas.org.dto.OrganizationWebhookResponse;
import com.example.saas.org.dto.UpdateOrganizationWebhookRequest;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.security.JwtPrincipal;
import com.example.saas.domain.Organization;
import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orgs")
public class OrganizationController {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final PlanLimitService planLimitService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@RequestBody @Valid CreateOrganizationRequest req,
                                                       Authentication authentication) {
        JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        String base = req.name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        org.setSlug(base + "-" + org.getId().toString().substring(0, 8));
        org.setName(req.name());
        try { org.setTimezone(java.time.ZoneId.systemDefault().toString()); } catch (Exception ignored) { org.setTimezone("UTC"); }
        org.setPlan(OrganizationPlan.FREE);
        org.setBusinessOpenTime(java.time.LocalTime.of(9, 0));
        org.setBusinessCloseTime(java.time.LocalTime.of(21, 0));
        org.setClosedWeekdays("SUNDAY");
        org.setDmWebhookEnabled(false);
        org.setDmWebhookSecret(UUID.randomUUID().toString().replace("-", ""));

        organizations.save(org);

        OrganizationMembership m = OrganizationMembership.builder()
                .id(UUID.randomUUID())
                .organizationId(org.getId())
                .userId(p.userId())
                .role(OrganizationMembership.OrgRole.OWNER)
                .build();
        memberships.save(m);

        var resp = toResponse(org);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> list(Authentication authentication) {
        JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();
        List<OrganizationMembership> ms = memberships.findAllByUserId(p.userId());
        var ids = ms.stream().map(OrganizationMembership::getOrganizationId).collect(Collectors.toSet());
        List<Organization> orgs = organizations.findAllById(ids);
        List<OrganizationResponse> out = orgs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @PatchMapping("/{orgId}/plan")
    public ResponseEntity<OrganizationResponse> updatePlan(@PathVariable UUID orgId,
                                                           @RequestBody @Valid UpdateOrganizationPlanRequest req,
                                                           Authentication authentication) {
        assertOwner(authentication, orgId);

        Organization org = organizations.findById(orgId)
                .orElseThrow(() -> new NotFoundException("ORG_NOT_FOUND", "조직을 찾을 수 없습니다."));
        org.setPlan(req.plan());
        organizations.save(org);

        return ResponseEntity.ok(toResponse(org));
    }

    @PatchMapping("/{orgId}/schedule")
    public ResponseEntity<OrganizationResponse> updateSchedule(@PathVariable UUID orgId,
                                                               @RequestBody @Valid UpdateOrganizationScheduleRequest req,
                                                               Authentication authentication) {
        assertOwner(authentication, orgId);

        if (!req.businessOpenTime().isBefore(req.businessCloseTime())) {
            throw new IllegalArgumentException("영업 시작 시간은 종료 시간보다 빨라야 합니다.");
        }

        Organization org = organizations.findById(orgId)
                .orElseThrow(() -> new NotFoundException("ORG_NOT_FOUND", "조직을 찾을 수 없습니다."));
        org.setBusinessOpenTime(req.businessOpenTime());
        org.setBusinessCloseTime(req.businessCloseTime());
        org.setClosedWeekdays(normalizeClosedWeekdays(req.closedWeekdays()));
        organizations.save(org);

        return ResponseEntity.ok(toResponse(org));
    }

    @PatchMapping("/{orgId}/webhook")
    public ResponseEntity<OrganizationResponse> updateWebhook(@PathVariable UUID orgId,
                                                              @RequestBody @Valid UpdateOrganizationWebhookRequest req,
                                                              Authentication authentication) {
        assertOwner(authentication, orgId);

        Organization org = organizations.findById(orgId)
                .orElseThrow(() -> new NotFoundException("ORG_NOT_FOUND", "조직을 찾을 수 없습니다."));
        org.setDmWebhookEnabled(req.enabled());
        if (req.secret() != null && !req.secret().isBlank()) {
            org.setDmWebhookSecret(req.secret().trim());
        }
        organizations.save(org);
        return ResponseEntity.ok(toResponse(org));
    }

    private OrganizationResponse toResponse(Organization org) {
        OrganizationPlan plan = org.getPlan() == null ? OrganizationPlan.FREE : org.getPlan();
        return new OrganizationResponse(
                org.getId(),
                org.getSlug(),
                org.getName(),
                org.getTimezone(),
                plan,
                planLimitService.getSnapshot(org.getId()),
                new OrganizationScheduleResponse(
                        org.getBusinessOpenTime(),
                        org.getBusinessCloseTime(),
                        parseClosedWeekdays(org.getClosedWeekdays())
                ),
                new OrganizationWebhookResponse(
                        org.isDmWebhookEnabled(),
                        org.getDmWebhookSecret()
                )
        );
    }

    private void assertOwner(Authentication authentication, UUID orgId) {
        JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();
        boolean isOwner = memberships.existsByUserIdAndOrganizationIdAndRole(
                p.userId(),
                orgId,
                OrganizationMembership.OrgRole.OWNER
        );
        if (!isOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private String normalizeClosedWeekdays(List<String> closedWeekdays) {
        return closedWeekdays.stream()
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> parseClosedWeekdays(String closedWeekdays) {
        if (closedWeekdays == null || closedWeekdays.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(closedWeekdays.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}

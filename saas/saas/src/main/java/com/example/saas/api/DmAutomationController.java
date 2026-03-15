package com.example.saas.api;

import com.example.saas.dm.DmMessageService;
import com.example.saas.dm.dto.DmMessageResponse;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.dm.dto.ManualDmCancelRequest;
import com.example.saas.dm.dto.ManualDmReservationRequest;
import com.example.saas.dm.dto.ManualDmUpdateRequest;
import com.example.saas.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dm")
public class DmAutomationController {

    private final DmMessageService dmMessageService;

    @PostMapping("/auto-reserve")
    public ResponseEntity<DmReservationResponse> autoReserve(@RequestBody @Valid DmReservationRequest req,
                                                             Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.processAutoReserve(principal.orgId(), principal.userId(), req));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<DmMessageResponse>> listMessages(@RequestParam(required = false) String status,
                                                                Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.list(principal.orgId(), status));
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<DmMessageResponse> getMessage(@PathVariable UUID id,
                                                        Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.get(principal.orgId(), id));
    }

    @PostMapping("/messages/{id}/confirm")
    public ResponseEntity<DmMessageResponse> confirmMessage(@PathVariable UUID id,
                                                            @RequestBody @Valid ManualDmReservationRequest req,
                                                            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.confirmReservation(principal.orgId(), principal.userId(), id, req));
    }

    @PostMapping("/messages/{id}/cancel")
    public ResponseEntity<DmMessageResponse> cancelMessage(@PathVariable UUID id,
                                                           @RequestBody @Valid ManualDmCancelRequest req,
                                                           Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.cancelReservation(principal.orgId(), principal.userId(), id, req));
    }

    @PostMapping("/messages/{id}/update")
    public ResponseEntity<DmMessageResponse> updateMessage(@PathVariable UUID id,
                                                           @RequestBody @Valid ManualDmUpdateRequest req,
                                                           Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(dmMessageService.updateReservation(principal.orgId(), principal.userId(), id, req));
    }
}

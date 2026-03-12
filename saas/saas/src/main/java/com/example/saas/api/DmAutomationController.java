package com.example.saas.api;

import com.example.saas.dm.DmMessageService;
import com.example.saas.dm.dto.DmMessageResponse;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
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

import java.util.List;

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
}

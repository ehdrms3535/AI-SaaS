package com.example.saas.auth;

import com.example.saas.auth.dto.*;
import com.example.saas.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Valid RefreshRequest req) {
        auth.logout(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserView> me(Authentication authentication) {
        JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(auth.me(p.userId()));
    }

    @PostMapping("/switch-org")
    public ResponseEntity<AuthResponse> switchOrg(@RequestBody @Valid SwitchOrgRequest req, Authentication authentication) {
        JwtPrincipal p = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(auth.switchOrg(p.userId(), req.orgId()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid com.example.saas.auth.dto.RegisterRequest req) {
        var resp = auth.register(req);
        return ResponseEntity.status(201).body(resp);
    }
}
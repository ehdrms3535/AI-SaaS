package com.example.saas.auth;

import com.example.saas.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
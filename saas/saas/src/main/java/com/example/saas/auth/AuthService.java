package com.example.saas.auth;

import com.example.saas.auth.dto.*;
import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.security.JwtTokenProvider;
import com.example.saas.user.User;
import com.example.saas.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // access: org은 TenantResolver가 memberships에서 결정할 수도 있으니 여기선 null로 발급해도 됨
        String access = jwt.createAccessToken(u.getId(), u.getEmail(), null);

        String refresh = jwt.createRefreshToken(u.getId());
        Instant refreshExp = jwt.getExpiration(refresh);

        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(u.getId())
                .tokenHash(sha256(refresh))
                .expiresAt(refreshExp)
                .createdAt(Instant.now())
                .build();

        refreshTokens.save(rt);

        return new AuthResponse(
                access,
                refresh,
                new AuthResponse.UserView(u.getId(), u.getEmail(), u.getName())
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String refresh = req.refreshToken();
        UUID userId = jwt.parseRefreshTokenSubject(refresh);

        RefreshToken rt = refreshTokens.findByTokenHash(sha256(refresh))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (rt.getRevokedAt() != null) throw new ApiException(ErrorCode.REFRESH_TOKEN_REVOKED);
        if (rt.getExpiresAt().isBefore(Instant.now())) throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);

        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        String newAccess = jwt.createAccessToken(u.getId(), u.getEmail(), null);

        return new AuthResponse(
                newAccess,
                refresh, // refresh는 그대로 유지(로테이션 원하면 여기서 새로 발급 + 기존 폐기)
                new AuthResponse.UserView(u.getId(), u.getEmail(), u.getName())
        );
    }

    @Transactional
    public void logout(RefreshRequest req) {
        RefreshToken rt = refreshTokens.findByTokenHash(sha256(req.refreshToken()))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        rt.setRevokedAt(Instant.now());
        refreshTokens.save(rt);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
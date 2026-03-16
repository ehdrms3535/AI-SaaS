package com.example.saas.auth;

import com.example.saas.auth.dto.*;
import com.example.saas.billing.OrganizationPlan;
import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.org.OrganizationMembershipRepository;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.org.OrganizationMembership;
import com.example.saas.repo.PasswordResetTokenRepository;
import com.example.saas.security.JwtTokenProvider;
import com.example.saas.domain.PasswordResetToken;
import com.example.saas.domain.User;
import com.example.saas.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final OrganizationMembershipRepository memberships; // ✅ 추가
    private final OrganizationRepository organizations;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        UUID orgId = memberships.findDefaultOrgIdByUserId(u.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN)); // 멤버십 없음

        String access = jwt.createAccessToken(u.getId(), u.getEmail(), orgId);

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

        // ✅ 교차검증
        if (!rt.getUserId().equals(userId)) throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);

        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        UUID orgId = memberships.findDefaultOrgIdByUserId(u.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

        String newAccess = jwt.createAccessToken(u.getId(), u.getEmail(), orgId);

        return new AuthResponse(
                newAccess,
                refresh,
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

    public AuthResponse.UserView me(UUID userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return new AuthResponse.UserView(u.getId(), u.getEmail(), u.getName());
    }

    @Transactional
    public AuthResponse switchOrg(UUID userId, UUID newOrgId) {
        // 멤버십 확인
        if (!memberships.existsByUserIdAndOrganizationId(userId, newOrgId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        String newAccess = jwt.createAccessToken(u.getId(), u.getEmail(), newOrgId);

        // refresh 토큰은 그대로 유지 (선택)
        return new AuthResponse(
                newAccess,
                null, // refresh는 null로
                new AuthResponse.UserView(u.getId(), u.getEmail(), u.getName())
        );
    }

    @Transactional
    public AuthResponse register(com.example.saas.auth.dto.RegisterRequest req) {
                // 이메일 중복 체크
                if (users.findByEmail(req.email()).isPresent()) {
                    throw new ApiException(ErrorCode.EMAIL_TAKEN);
                }

                User u = User.builder()
                        .id(UUID.randomUUID())
                        .email(req.email())
                        .passwordHash(encoder.encode(req.password()))
                        .name(req.name())
                        .build();

                users.save(u);

                // 기본 Organization 자동 생성
                                var org = new com.example.saas.domain.Organization();
                                org.setId(UUID.randomUUID());
                                String base = req.name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
                                org.setSlug(base + "-" + org.getId().toString().substring(0, 8));
                                // 필수 칼럼(예: name, timezone 등)을 채워 DB 제약 위반 방지
                                org.setName(req.name());
                                try {
                                        org.setTimezone(java.time.ZoneId.systemDefault().toString());
                                } catch (Exception ignored) {
                                        org.setTimezone("UTC");
                                }
                                org.setPlan(OrganizationPlan.FREE);
                organizations.save(org);

                // OrganizationMembership 생성 (OWNER)
                OrganizationMembership m = OrganizationMembership.builder()
                        .id(UUID.randomUUID())
                        .organizationId(org.getId())
                        .userId(u.getId())
                        .role(OrganizationMembership.OrgRole.OWNER)
                        .build();
                memberships.save(m);

                // 토큰 발급
                String access = jwt.createAccessToken(u.getId(), u.getEmail(), org.getId());
                String refresh = jwt.createRefreshToken(u.getId());

                var refreshExp = jwt.getExpiration(refresh);
                RefreshToken rt = RefreshToken.builder()
                        .id(UUID.randomUUID())
                        .userId(u.getId())
                        .tokenHash(sha256(refresh))
                        .expiresAt(refreshExp)
                        .createdAt(java.time.Instant.now())
                        .build();
                refreshTokens.save(rt);

                return new AuthResponse(
                        access,
                        refresh,
                        new AuthResponse.UserView(u.getId(), u.getEmail(), u.getName())
                );
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_NOT_FOUND));

        List<PasswordResetToken> activeTokens = passwordResetTokens.findByUserIdAndUsedAtIsNull(user.getId());
        Instant now = Instant.now();
        activeTokens.forEach(token -> token.setUsedAt(now));
        if (!activeTokens.isEmpty()) {
            passwordResetTokens.saveAll(activeTokens);
        }

        String plainToken = generateResetToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(sha256(plainToken))
                .createdAt(now)
                .expiresAt(now.plusSeconds(60 * 30))
                .build();
        passwordResetTokens.save(resetToken);

        String normalizedBaseUrl = req.baseUrl().replaceAll("/+$", "");
        if (!StringUtils.hasText(normalizedBaseUrl)) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
        String resetLink = normalizedBaseUrl + "/reset-password.html?token=" + plainToken;
        passwordResetMailService.sendResetLink(user.getEmail(), resetLink);

        return new PasswordResetRequestResponse(
                "재설정 링크를 이메일로 보냈습니다. 메일함을 확인해 주세요."
        );
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest req) {
        PasswordResetToken resetToken = getValidPasswordResetToken(req.token());

        if (resetToken.getUsedAt() != null) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user = users.findById(resetToken.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        user.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokens.save(resetToken);
    }

    @Transactional(readOnly = true)
    public PasswordResetValidateResponse validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = getValidPasswordResetToken(token);
        return new PasswordResetValidateResponse(true, resetToken.getExpiresAt());
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

    private static String generateResetToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private PasswordResetToken getValidPasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokens.findByTokenHash(sha256(token))
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (resetToken.getUsedAt() != null) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
        return resetToken;
    }
}

package com.example.saas.security;

import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId, String email, UUID orgId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTokenMinutes(), ChronoUnit.MINUTES);

        JwtBuilder b = Jwts.builder()
                .issuer(props.getIssuer())
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key());

        if (orgId != null) b.claim("org", orgId.toString());

        return b.compact();
    }

    public String createRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getRefreshTokenDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(userId.toString())
                .claim("typ", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key())
                .compact();
    }

    public JwtPrincipal parseAccessToken(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(c.getSubject());
            String email = c.get("email", String.class);
            String org = c.get("org", String.class);
            UUID orgId = (org == null ? null : UUID.fromString(org));
            return new JwtPrincipal(userId, email, orgId);

        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    public UUID parseRefreshTokenSubject(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String typ = c.get("typ", String.class);
            if (!"refresh".equals(typ)) throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);

            return UUID.fromString(c.getSubject());

        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    public Instant getExpiration(String token) {
        Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return c.getExpiration().toInstant();
    }
}
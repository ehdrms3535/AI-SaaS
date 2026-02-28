package com.example.saas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/api/auth/")
                || p.equals("/ping-db")
                || p.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        // ✅ 토큰 없으면 그냥 통과 (permitAll / dev open 모드에서도 막지 않음)
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            JwtPrincipal principal = jwt.parseAccessToken(token);

            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(req, res);

        } catch (Exception e) {
            // ✅ 토큰이 "있는데" 잘못된 경우만 401
            SecurityContextHolder.clearContext();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.setContentType("application/json");
            res.getWriter().write("""
                {"error":"UNAUTHORIZED","message":"Invalid or expired token"}
            """);
        }
    }
}
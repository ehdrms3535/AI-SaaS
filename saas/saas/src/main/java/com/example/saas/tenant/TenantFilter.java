package com.example.saas.tenant;

import com.example.saas.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/api/auth/")
                || p.equals("/")
                || p.equals("/index.html")
                || p.equals("/app.css")
                || p.equals("/app.js")
                || p.equals("/favicon.ico")
                || p.startsWith("/legal/")
                || p.equals("/ping-db")
                || p.equals("/api/channels/instagram/callback")
                || p.startsWith("/webhooks/dm/")
                || p.equals("/webhooks/meta/instagram")
                || p.equals("/webhooks/meta/data-deletion")
                || p.equals("/webhooks/meta/data-deletion/status")
                || p.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 인증이 없는 요청은 여기서 테넌트 세팅하지 않고 그냥 통과
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
                chain.doFilter(req, res);
                return;
            }

            UUID orgId = p.orgId();

            // ✅ 토큰에 orgId가 없으면 테넌트가 결정되지 않으므로 차단 (운영 안전)
            if (orgId == null) {
                deny(res, HttpServletResponse.SC_UNAUTHORIZED, "ORG_NOT_IN_TOKEN");
                return;
            }

            TenantContext.setOrgId(orgId);
            chain.doFilter(req, res);

        } finally {
            TenantContext.clear();
        }
    }

    private static void deny(HttpServletResponse res, int status, String code) throws IOException {
        res.setStatus(status);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("""
                {"error":"%s","message":"Tenant(orgId) is missing"} 
                """.formatted(code));
    }
}

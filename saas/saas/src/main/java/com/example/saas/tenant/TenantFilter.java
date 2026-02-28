package com.example.saas.tenant;

import com.example.saas.security.JwtPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver resolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/api/auth/") || p.equals("/ping-db");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof JwtPrincipal p) {
                UUID orgId = resolver.resolveOrgId(req, p);
                TenantContext.setOrgId(orgId);
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
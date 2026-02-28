package com.example.saas.tenant;

import com.example.saas.common.ApiException;
import com.example.saas.common.ErrorCode;
import com.example.saas.org.OrganizationMembershipRepository;
import com.example.saas.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    private final OrganizationMembershipRepository memberships;

    public UUID resolveOrgId(HttpServletRequest req, JwtPrincipal p) {
        String header = req.getHeader("X-ORG-ID");
        if (header != null && !header.isBlank()) {
            UUID orgId = UUID.fromString(header);
            // 사용자가 그 org 멤버인지 검증(중요!)
            if (!memberships.existsByUserIdAndOrganizationId(p.userId(), orgId)) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
            return orgId;
        }

        if (p.orgId() != null) return p.orgId();

        return memberships.findDefaultOrgIdByUserId(p.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.ORG_NOT_SELECTED));
    }
}
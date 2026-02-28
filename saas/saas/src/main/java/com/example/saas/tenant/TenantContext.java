package com.example.saas.tenant;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> ORG = new ThreadLocal<>();

    public static void setOrgId(UUID orgId) { ORG.set(orgId); }
    public static UUID getOrgId() { return ORG.get(); }
    public static void clear() { ORG.remove(); }

    private TenantContext() {}
}
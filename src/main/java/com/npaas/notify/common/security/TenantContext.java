package com.npaas.notify.common.security;

import java.util.Optional;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    static void setTenantSlug(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static Optional<String> getTenantSlug() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    static void clear() {
        CURRENT_TENANT.remove();
    }
}

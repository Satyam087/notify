package com.npaas.notify.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantAuthorizationService {

    public void requireTenant(String tenantSlug) {
        String authenticatedTenant = TenantContext.getTenantSlug()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing tenant authentication"));

        if (!authenticatedTenant.equals(tenantSlug)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key cannot access this tenant");
        }
    }
}

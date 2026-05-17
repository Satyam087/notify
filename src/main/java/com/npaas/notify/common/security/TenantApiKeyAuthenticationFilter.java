package com.npaas.notify.common.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Notify-Api-Key";

    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;

    public TenantApiKeyAuthenticationFilter(
            TenantApiKeyRepository tenantApiKeyRepository,
            ApiKeyHasher apiKeyHasher) {
        this.tenantApiKeyRepository = tenantApiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/events") && !path.startsWith("/api/v1/in-app-notifications");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeUnauthorized(response, "Missing API key");
            return;
        }

        String apiKeyHash = apiKeyHasher.sha256(apiKey);
        TenantApiKey tenantApiKey = tenantApiKeyRepository
            .findByKeyHashAndStatus(apiKeyHash, TenantApiKeyStatus.ACTIVE)
            .orElse(null);

        if (tenantApiKey == null) {
            writeUnauthorized(response, "Invalid API key");
            return;
        }

        try {
            TenantContext.setTenantSlug(tenantApiKey.getTenantSlug());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                tenantApiKey.getTenantSlug(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_NOTIFY_CLIENT"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
            {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s"}
            """.formatted(Instant.now(), message));
    }
}

package com.npaas.notify.common.security.admin;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.common.security.ApiKeyHasher;
import com.npaas.notify.common.security.TenantApiKey;
import com.npaas.notify.common.security.TenantApiKeyRepository;
import com.npaas.notify.tenants.Tenant;
import com.npaas.notify.tenants.TenantRepository;
import com.npaas.notify.tenants.TenantStatus;

@Service
public class TenantApiKeyAdminService {

    private static final String KEY_PREFIX = "notify_live_";
    private static final int RANDOM_BYTES = 32;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final int STORED_PREFIX_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();
    private final TenantRepository tenantRepository;
    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;

    public TenantApiKeyAdminService(
            TenantRepository tenantRepository,
            TenantApiKeyRepository tenantApiKeyRepository,
            ApiKeyHasher apiKeyHasher) {
        this.tenantRepository = tenantRepository;
        this.tenantApiKeyRepository = tenantApiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Transactional
    public GeneratedTenantApiKey createApiKey(String tenantSlug, String keyName) {
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
            .orElseThrow(() -> new IllegalArgumentException("Tenant does not exist: " + tenantSlug));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is not active: " + tenantSlug);
        }

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String rawKey = generateRawKey();
            String keyHash = apiKeyHasher.sha256(rawKey);

            if (tenantApiKeyRepository.existsByKeyHash(keyHash)) {
                continue;
            }

            TenantApiKey tenantApiKey = new TenantApiKey(
                UUID.randomUUID(),
                tenantSlug,
                keyName,
                rawKey.substring(0, STORED_PREFIX_LENGTH),
                keyHash
            );
            TenantApiKey savedKey = tenantApiKeyRepository.save(tenantApiKey);
            return new GeneratedTenantApiKey(savedKey.getId(), rawKey, savedKey.getKeyPrefix());
        }

        throw new IllegalStateException("Could not generate a unique API key");
    }

    private String generateRawKey() {
        byte[] randomBytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

package com.npaas.notify.common.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, UUID> {

    Optional<TenantApiKey> findByKeyHashAndStatus(String keyHash, TenantApiKeyStatus status);

    boolean existsByKeyHash(String keyHash);
}

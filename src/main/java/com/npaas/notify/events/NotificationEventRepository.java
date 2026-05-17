package com.npaas.notify.events;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Optional<NotificationEvent> findByTenantSlugAndIdempotencyKey(String tenantSlug, String idempotencyKey);
}

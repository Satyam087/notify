package com.npaas.notify.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Optional<NotificationEvent> findByTenantSlugAndIdempotencyKey(String tenantSlug, String idempotencyKey);

    List<NotificationEvent> findByStatusAndUpdatedAtBeforeOrderByCreatedAtAsc(
            NotificationEventStatus status,
            Instant updatedAt,
            Pageable pageable);
}

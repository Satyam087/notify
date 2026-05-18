package com.npaas.notify.jobs;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {

    boolean existsByEventIdAndChannel(UUID eventId, NotificationChannel channel);

    List<NotificationJob> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    List<NotificationJob> findByTenantSlugAndStatusOrderByUpdatedAtDesc(
            String tenantSlug,
            NotificationJobStatus status,
            Pageable pageable);

    List<NotificationJob> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationJobStatus status,
            Instant nextAttemptAt,
            Pageable pageable);

    List<NotificationJob> findByStatusAndNextAttemptAtIsNullOrderByCreatedAtAsc(
            NotificationJobStatus status,
            Pageable pageable);

    long countByEventIdAndStatusIn(UUID eventId, Collection<NotificationJobStatus> statuses);

    long countByEventId(UUID eventId);
}

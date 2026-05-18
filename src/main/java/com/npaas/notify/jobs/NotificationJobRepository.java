package com.npaas.notify.jobs;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {

    boolean existsByEventIdAndChannel(UUID eventId, NotificationChannel channel);

    List<NotificationJob> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from NotificationJob job where job.id = :id")
    java.util.Optional<NotificationJob> findByIdForUpdate(@Param("id") UUID id);

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

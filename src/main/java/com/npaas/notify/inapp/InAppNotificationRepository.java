package com.npaas.notify.inapp;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    boolean existsByJobId(UUID jobId);

    List<InAppNotification> findByTenantSlugAndRecipientUserIdOrderByCreatedAtDesc(
            String tenantSlug,
            String recipientUserId,
            Pageable pageable);

    long countByTenantSlugAndRecipientUserIdAndReadAtIsNull(String tenantSlug, String recipientUserId);

    List<InAppNotification> findByTenantSlugAndRecipientUserIdAndReadAtIsNull(
            String tenantSlug,
            String recipientUserId);
}

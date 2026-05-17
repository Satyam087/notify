package com.npaas.notify.templates;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.npaas.notify.jobs.NotificationChannel;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
            String tenantSlug,
            String eventType,
            NotificationChannel channel);
}

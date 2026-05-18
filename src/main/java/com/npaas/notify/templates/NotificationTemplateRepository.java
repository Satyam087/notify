package com.npaas.notify.templates;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.npaas.notify.jobs.NotificationChannel;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByTenantSlugOrderByEventTypeAscChannelAscTemplateKeyAsc(String tenantSlug);

    Optional<NotificationTemplate> findByTenantSlugAndTemplateKey(String tenantSlug, String templateKey);

    Optional<NotificationTemplate> findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
            String tenantSlug,
            String eventType,
            NotificationChannel channel);
}

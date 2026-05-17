package com.npaas.notify.rules;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {

    List<NotificationRule> findByTenantSlugAndEventTypeAndEnabledTrue(String tenantSlug, String eventType);
}

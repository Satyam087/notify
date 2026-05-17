package com.npaas.notify.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.QueuedNotificationEvent;
import com.npaas.notify.rules.NotificationRule;
import com.npaas.notify.rules.NotificationRuleRepository;
import com.npaas.notify.templates.NotificationTemplate;
import com.npaas.notify.templates.NotificationTemplateRepository;

@Service
public class NotificationJobService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;

    public NotificationJobService(
            NotificationJobRepository notificationJobRepository,
            NotificationRuleRepository notificationRuleRepository,
            NotificationTemplateRepository notificationTemplateRepository) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Transactional
    public void createInitialJobIfMissing(QueuedNotificationEvent event) {
        List<NotificationRule> rules = notificationRuleRepository
            .findByTenantSlugAndEventTypeAndEnabledTrue(event.tenantId(), event.eventType());

        for (NotificationRule rule : rules) {
            createJobIfMissing(event, rule.getChannel());
        }
    }

    private void createJobIfMissing(QueuedNotificationEvent event, NotificationChannel channel) {
        if (notificationJobRepository.existsByEventIdAndChannel(event.eventId(), channel)) {
            return;
        }

        NotificationTemplate template = notificationTemplateRepository
            .findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
                event.tenantId(),
                event.eventType(),
                channel
            )
            .orElse(null);

        if (template == null) {
            return;
        }

        try {
            NotificationJob job = new NotificationJob(
                UUID.randomUUID(),
                event.eventId(),
                template.getId(),
                event.tenantId(),
                channel,
                NotificationJobStatus.PENDING
            );
            notificationJobRepository.save(job);
        } catch (DataIntegrityViolationException ignored) {
            // A redelivered or concurrent message may race this insert. The unique
            // event/channel constraint keeps the worker idempotent.
        }
    }
}

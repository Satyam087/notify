package com.npaas.notify.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.QueuedNotificationEvent;
import com.npaas.notify.rules.NotificationRule;
import com.npaas.notify.rules.NotificationRuleRepository;

@Service
public class NotificationJobService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationRuleRepository notificationRuleRepository;

    public NotificationJobService(
            NotificationJobRepository notificationJobRepository,
            NotificationRuleRepository notificationRuleRepository) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationRuleRepository = notificationRuleRepository;
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
        try {
            NotificationJob job = new NotificationJob(
                UUID.randomUUID(),
                event.eventId(),
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

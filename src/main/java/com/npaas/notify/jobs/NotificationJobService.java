package com.npaas.notify.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.events.NotificationEventRepository;
import com.npaas.notify.events.QueuedNotificationEvent;
import com.npaas.notify.rules.NotificationRule;
import com.npaas.notify.rules.NotificationRuleRepository;
import com.npaas.notify.templates.RenderedTemplate;
import com.npaas.notify.templates.NotificationTemplate;
import com.npaas.notify.templates.NotificationTemplateRepository;
import com.npaas.notify.templates.TemplateRenderer;

@Service
public class NotificationJobService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final TemplateRenderer templateRenderer;
    private final NotificationJobWriter notificationJobWriter;

    public NotificationJobService(
            NotificationJobRepository notificationJobRepository,
            NotificationEventRepository notificationEventRepository,
            NotificationRuleRepository notificationRuleRepository,
            NotificationTemplateRepository notificationTemplateRepository,
            TemplateRenderer templateRenderer,
            NotificationJobWriter notificationJobWriter) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.templateRenderer = templateRenderer;
        this.notificationJobWriter = notificationJobWriter;
    }

    @Transactional
    public void createInitialJobIfMissing(QueuedNotificationEvent queuedEvent) {
        NotificationEvent event = notificationEventRepository
            .findById(queuedEvent.eventId())
            .orElse(null);

        if (event == null) {
            return;
        }

        List<NotificationRule> rules = notificationRuleRepository
            .findByTenantSlugAndEventTypeAndEnabledTrue(queuedEvent.tenantId(), queuedEvent.eventType());

        for (NotificationRule rule : rules) {
            createJobIfMissing(event, rule.getChannel());
        }
    }

    private void createJobIfMissing(NotificationEvent event, NotificationChannel channel) {
        if (notificationJobRepository.existsByEventIdAndChannel(event.getId(), channel)) {
            return;
        }

        NotificationTemplate template = notificationTemplateRepository
            .findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
                event.getTenantSlug(),
                event.getEventType(),
                channel
            )
            .orElse(null);

        if (template == null) {
            return;
        }

        RenderedTemplate renderedTemplate = templateRenderer.render(template, event.getPayload());

        try {
            NotificationJob job = new NotificationJob(
                UUID.randomUUID(),
                event.getId(),
                template.getId(),
                event.getTenantSlug(),
                channel,
                NotificationJobStatus.PENDING,
                renderedTemplate.subject(),
                renderedTemplate.body()
            );
            notificationJobWriter.saveNewJob(job);
        } catch (DataIntegrityViolationException ignored) {
            // A redelivered or concurrent message may race this insert. The unique
            // event/channel constraint keeps the worker idempotent. The insert runs
            // in its own transaction so this rollback cannot poison the whole
            // message-consumer transaction.
        }
    }
}

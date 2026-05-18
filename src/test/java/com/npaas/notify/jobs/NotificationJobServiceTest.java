package com.npaas.notify.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.events.NotificationEventRepository;
import com.npaas.notify.events.NotificationEventStatus;
import com.npaas.notify.events.QueuedNotificationEvent;
import com.npaas.notify.rules.NotificationRule;
import com.npaas.notify.rules.NotificationRuleRepository;
import com.npaas.notify.templates.NotificationTemplate;
import com.npaas.notify.templates.NotificationTemplateRepository;
import com.npaas.notify.templates.TemplateRenderer;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTest {

    @Mock
    private NotificationJobRepository notificationJobRepository;

    @Mock
    private NotificationEventRepository notificationEventRepository;

    @Mock
    private NotificationRuleRepository notificationRuleRepository;

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private NotificationJobWriter notificationJobWriter;

    private NotificationJobService notificationJobService;

    @BeforeEach
    void setUp() {
        notificationJobService = new NotificationJobService(
            notificationJobRepository,
            notificationEventRepository,
            notificationRuleRepository,
            notificationTemplateRepository,
            new TemplateRenderer(new ObjectMapper()),
            notificationJobWriter
        );
    }

    @Test
    void createsRenderedJobForMatchingRuleAndTemplate() {
        UUID eventId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(
            eventId,
            "campuscritique",
            "connect.requested",
            "connect-001",
            """
            {"userId":"admin-1","email":"admin@example.com"}
            """,
            """
            {"collegeName":"Newton ADYPU"}
            """,
            NotificationEventStatus.QUEUED
        );
        NotificationRule rule = rule(NotificationChannel.EMAIL);
        NotificationTemplate template = new NotificationTemplate(
            templateId,
            "campuscritique",
            "connect.requested",
            NotificationChannel.EMAIL,
            "connect_email",
            "New connect for {{collegeName}}",
            "Open request for {{collegeName}}.",
            true
        );

        when(notificationEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(notificationRuleRepository.findByTenantSlugAndEventTypeAndEnabledTrue(
            "campuscritique",
            "connect.requested"
        )).thenReturn(List.of(rule));
        when(notificationJobRepository.existsByEventIdAndChannel(eventId, NotificationChannel.EMAIL)).thenReturn(false);
        when(notificationTemplateRepository.findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
            "campuscritique",
            "connect.requested",
            NotificationChannel.EMAIL
        )).thenReturn(Optional.of(template));

        notificationJobService.createInitialJobIfMissing(
            new QueuedNotificationEvent(eventId, "campuscritique", "connect.requested", Instant.now())
        );

        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobWriter).saveNewJob(jobCaptor.capture());

        NotificationJob job = jobCaptor.getValue();
        assertThat(job.getEventId()).isEqualTo(eventId);
        assertThat(job.getTemplateId()).isEqualTo(templateId);
        assertThat(job.getTenantSlug()).isEqualTo("campuscritique");
        assertThat(job.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(job.getStatus()).isEqualTo(NotificationJobStatus.PENDING);
        assertThat(job.getRenderedSubject()).isEqualTo("New connect for Newton ADYPU");
        assertThat(job.getRenderedBody()).isEqualTo("Open request for Newton ADYPU.");
    }

    @Test
    void skipsJobCreationWhenJobAlreadyExistsForEventAndChannel() {
        UUID eventId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(
            eventId,
            "campuscritique",
            "connect.requested",
            "connect-001",
            "{}",
            "{}",
            NotificationEventStatus.QUEUED
        );

        when(notificationEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(notificationRuleRepository.findByTenantSlugAndEventTypeAndEnabledTrue(
            "campuscritique",
            "connect.requested"
        )).thenReturn(List.of(rule(NotificationChannel.IN_APP)));
        when(notificationJobRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP)).thenReturn(true);

        notificationJobService.createInitialJobIfMissing(
            new QueuedNotificationEvent(eventId, "campuscritique", "connect.requested", Instant.now())
        );

        verify(notificationTemplateRepository, never())
            .findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
                "campuscritique",
                "connect.requested",
                NotificationChannel.IN_APP
            );
        verify(notificationJobWriter, never()).saveNewJob(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void swallowsDuplicateJobInsertRace() {
        UUID eventId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent(
            eventId,
            "campuscritique",
            "connect.requested",
            "connect-001",
            "{}",
            "{}",
            NotificationEventStatus.QUEUED
        );
        NotificationTemplate template = new NotificationTemplate(
            templateId,
            "campuscritique",
            "connect.requested",
            NotificationChannel.IN_APP,
            "connect_in_app",
            "New connect",
            "Open request.",
            true
        );

        when(notificationEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(notificationRuleRepository.findByTenantSlugAndEventTypeAndEnabledTrue(
            "campuscritique",
            "connect.requested"
        )).thenReturn(List.of(rule(NotificationChannel.IN_APP)));
        when(notificationJobRepository.existsByEventIdAndChannel(eventId, NotificationChannel.IN_APP)).thenReturn(false);
        when(notificationTemplateRepository.findFirstByTenantSlugAndEventTypeAndChannelAndEnabledTrueOrderByCreatedAtDesc(
            "campuscritique",
            "connect.requested",
            NotificationChannel.IN_APP
        )).thenReturn(Optional.of(template));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
            .when(notificationJobWriter)
            .saveNewJob(org.mockito.ArgumentMatchers.any(NotificationJob.class));

        assertThatCode(() -> notificationJobService.createInitialJobIfMissing(
            new QueuedNotificationEvent(eventId, "campuscritique", "connect.requested", Instant.now())
        )).doesNotThrowAnyException();
    }

    private NotificationRule rule(NotificationChannel channel) {
        NotificationRule rule = newRule();
        ReflectionTestUtils.setField(rule, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(rule, "tenantSlug", "campuscritique");
        ReflectionTestUtils.setField(rule, "eventType", "connect.requested");
        ReflectionTestUtils.setField(rule, "channel", channel);
        ReflectionTestUtils.setField(rule, "enabled", true);
        return rule;
    }

    private NotificationRule newRule() {
        try {
            var constructor = NotificationRule.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create notification rule test fixture", exception);
        }
    }
}

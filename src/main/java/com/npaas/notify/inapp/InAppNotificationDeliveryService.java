package com.npaas.notify.inapp;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;

@Service
public class InAppNotificationDeliveryService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final ObjectMapper objectMapper;

    public InAppNotificationDeliveryService(
            InAppNotificationRepository inAppNotificationRepository,
            ObjectMapper objectMapper) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.objectMapper = objectMapper;
    }

    public void deliverIfInApp(NotificationJob job, NotificationEvent event) {
        if (job.getChannel() != NotificationChannel.IN_APP || inAppNotificationRepository.existsByJobId(job.getId())) {
            return;
        }

        Optional<String> recipientUserId = extractRecipientUserId(event.getRecipient());
        if (recipientUserId.isEmpty()) {
            return;
        }

        try {
            InAppNotification notification = new InAppNotification(
                UUID.randomUUID(),
                job.getTenantSlug(),
                job.getId(),
                event.getId(),
                recipientUserId.get(),
                job.getRenderedSubject(),
                job.getRenderedBody()
            );
            inAppNotificationRepository.save(notification);
            job.markSent();
        } catch (DataIntegrityViolationException ignored) {
            // The unique job constraint keeps in-app delivery idempotent if a message is redelivered.
        }
    }

    private Optional<String> extractRecipientUserId(String recipientJson) {
        JsonNode recipient = parseJson(recipientJson);
        JsonNode userId = recipient.get("userId");
        if (userId == null || userId.isNull() || !userId.isTextual() || userId.asText().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(userId.asText());
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return objectMapper.valueToTree(Map.of());
        }
    }
}

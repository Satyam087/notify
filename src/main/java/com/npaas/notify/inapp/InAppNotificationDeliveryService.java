package com.npaas.notify.inapp;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.delivery.DeliveryException;
import com.npaas.notify.delivery.DeliveryResult;
import com.npaas.notify.delivery.NotificationDeliveryHandler;
import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;

@Service
public class InAppNotificationDeliveryService implements NotificationDeliveryHandler {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final ObjectMapper objectMapper;

    public InAppNotificationDeliveryService(
            InAppNotificationRepository inAppNotificationRepository,
            ObjectMapper objectMapper) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryResult deliver(NotificationJob job, NotificationEvent event) {
        Optional<String> recipientUserId = extractRecipientUserId(event.getRecipient());
        if (recipientUserId.isEmpty()) {
            throw new DeliveryException("Missing recipient.userId for in-app notification", false);
        }

        if (inAppNotificationRepository.existsByJobId(job.getId())) {
            return DeliveryResult.delivered("in-app");
        }

        try {
            InAppNotification notification = new InAppNotification(
                UUID.randomUUID(),
                job.getTenantSlug(),
                job.getId(),
                event.getId(),
                recipientUserId.get(),
                job.getRenderedSubject(),
                job.getRenderedBody(),
                extractDeepLink(event.getPayload()).orElse(null)
            );
            inAppNotificationRepository.save(notification);
        } catch (DataIntegrityViolationException ignored) {
            // The unique job constraint keeps in-app delivery idempotent if a message is redelivered.
        }

        return DeliveryResult.delivered("in-app");
    }

    private Optional<String> extractRecipientUserId(String recipientJson) {
        JsonNode recipient = parseJson(recipientJson);
        JsonNode userId = recipient.get("userId");
        if (userId == null || userId.isNull() || !userId.isTextual() || userId.asText().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(userId.asText());
    }

    private Optional<String> extractDeepLink(String payloadJson) {
        JsonNode payload = parseJson(payloadJson);
        JsonNode deepLink = payload.get("deepLink");
        if (deepLink == null || deepLink.isNull() || !deepLink.isTextual() || deepLink.asText().isBlank()) {
            return Optional.empty();
        }

        String value = deepLink.asText().trim();
        if (value.length() > 500) {
            return Optional.of(value.substring(0, 500));
        }

        return Optional.of(value);
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return objectMapper.valueToTree(Map.of());
        }
    }
}

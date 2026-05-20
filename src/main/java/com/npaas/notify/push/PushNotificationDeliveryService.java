package com.npaas.notify.push;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.npaas.notify.delivery.DeliveryException;
import com.npaas.notify.delivery.DeliveryResult;
import com.npaas.notify.delivery.NotificationDeliveryHandler;
import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;

@Service
public class PushNotificationDeliveryService implements NotificationDeliveryHandler {

    private final PushSubscriptionService pushSubscriptionService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final boolean enabled;
    private final String provider;

    public PushNotificationDeliveryService(
            PushSubscriptionService pushSubscriptionService,
            ObjectMapper objectMapper,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider,
            @Value("${notify.push.enabled:false}") boolean enabled,
            @Value("${notify.push.provider:firebase}") String provider) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.objectMapper = objectMapper;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
        this.enabled = enabled;
        this.provider = provider == null ? "firebase" : provider.trim().toLowerCase();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public DeliveryResult deliver(NotificationJob job, NotificationEvent event) {
        if (!enabled) {
            return DeliveryResult.delivered("push:disabled");
        }
        if (!"firebase".equals(provider) && !"fcm".equals(provider)) {
            throw new DeliveryException("Unsupported push provider: " + provider, false);
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            throw new DeliveryException("Firebase messaging is not configured", true);
        }

        String recipientUserId = extractRecipientUserId(event.getRecipient())
            .orElseThrow(() -> new DeliveryException("Missing recipient.userId for push notification", false));
        List<PushSubscription> subscriptions = pushSubscriptionService
            .activeForRecipient(job.getTenantSlug(), recipientUserId);

        if (subscriptions.isEmpty()) {
            return DeliveryResult.delivered("firebase:none");
        }

        int delivered = 0;
        Map<String, String> payload = buildPayload(job, event);
        for (PushSubscription subscription : subscriptions) {
            delivered += deliverToSubscription(firebaseMessaging, subscription, payload);
        }

        return DeliveryResult.delivered("firebase:" + delivered);
    }

    private int deliverToSubscription(
            FirebaseMessaging firebaseMessaging,
            PushSubscription subscription,
            Map<String, String> payload) {
        String token = subscription.getFcmToken();
        if (token == null || token.isBlank()) {
            recordSubscriptionFailure(subscription, "Missing Firebase token", true);
            return 0;
        }

        Message message = Message.builder()
            .setToken(token)
            .putAllData(payload)
            .build();

        try {
            firebaseMessaging.send(message);
            recordSubscriptionSuccess(subscription);
            return 1;
        } catch (FirebaseMessagingException exception) {
            boolean deactivate = isInvalidToken(exception);
            recordSubscriptionFailure(subscription, safeMessage(exception), deactivate);
            if (deactivate) {
                return 0;
            }
            throw new DeliveryException("Firebase push delivery failed: " + safeMessage(exception), true);
        } catch (RuntimeException exception) {
            recordSubscriptionFailure(subscription, safeMessage(exception), false);
            throw new DeliveryException("Firebase push delivery failed: " + safeMessage(exception), true);
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        MessagingErrorCode code = exception.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private void recordSubscriptionSuccess(PushSubscription subscription) {
        try {
            pushSubscriptionService.recordSuccess(subscription.getId());
        } catch (RuntimeException ignored) {
            // Job-level delivery status must not be held hostage by subscription bookkeeping.
        }
    }

    private void recordSubscriptionFailure(PushSubscription subscription, String message, boolean deactivate) {
        try {
            pushSubscriptionService.recordFailure(subscription.getId(), message, deactivate);
        } catch (RuntimeException ignored) {
            // The delivery transaction records the provider attempt separately.
        }
    }

    private Map<String, String> buildPayload(NotificationJob job, NotificationEvent event) {
        JsonNode payload = parseJson(event.getPayload());
        return Map.of(
            "title", truncate(job.getRenderedSubject(), 120),
            "body", truncate(job.getRenderedBody(), 240),
            "deepLink", extractText(payload, "deepLink").orElse("/"),
            "eventType", event.getEventType()
        );
    }

    private Optional<String> extractRecipientUserId(String recipientJson) {
        JsonNode recipient = parseJson(recipientJson);
        return extractText(recipient, "userId");
    }

    private Optional<String> extractText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value.asText().trim());
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return objectMapper.valueToTree(Map.of());
        }
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null || value.isBlank() ? "Notification" : value.trim();
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, Math.min(normalized.length(), maxLength)).trim();
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return message.length() <= 240 ? message : message.substring(0, 240);
    }
}

package com.npaas.notify.push;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
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

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

@Service
public class PushNotificationDeliveryService implements NotificationDeliveryHandler {

    private final PushSubscriptionService pushSubscriptionService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;

    public PushNotificationDeliveryService(
            PushSubscriptionService pushSubscriptionService,
            ObjectMapper objectMapper,
            @Value("${notify.push.enabled:false}") boolean enabled,
            @Value("${notify.push.vapid.public-key:}") String vapidPublicKey,
            @Value("${notify.push.vapid.private-key:}") String vapidPrivateKey,
            @Value("${notify.push.vapid.subject:mailto:connect@campuscritique.in}") String vapidSubject) {
        this.pushSubscriptionService = pushSubscriptionService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.vapidSubject = vapidSubject;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public DeliveryResult deliver(NotificationJob job, NotificationEvent event) {
        if (!enabled) {
            return DeliveryResult.delivered("web-push:disabled");
        }

        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            throw new DeliveryException("Push VAPID keys are not configured", true);
        }

        String recipientUserId = extractRecipientUserId(event.getRecipient())
            .orElseThrow(() -> new DeliveryException("Missing recipient.userId for push notification", false));
        List<PushSubscription> subscriptions = pushSubscriptionService
            .activeForRecipient(job.getTenantSlug(), recipientUserId);

        if (subscriptions.isEmpty()) {
            return DeliveryResult.delivered("web-push:none");
        }

        int delivered = 0;
        PushService pushService = createPushService();
        String payload = buildPayload(job, event);

        for (PushSubscription subscription : subscriptions) {
            delivered += deliverToSubscription(pushService, subscription, payload);
        }

        return DeliveryResult.delivered("web-push:" + delivered);
    }

    private PushService createPushService() {
        try {
            return new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (Exception exception) {
            throw new DeliveryException("Push VAPID configuration is invalid", false);
        }
    }

    private int deliverToSubscription(PushService pushService, PushSubscription savedSubscription, String payload) {
        try {
            Subscription subscription = new Subscription(
                savedSubscription.getEndpoint(),
                new Subscription.Keys(savedSubscription.getP256dhKey(), savedSubscription.getAuthKey())
            );
            HttpResponse response = pushService.send(new Notification(subscription, payload));
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                pushSubscriptionService.recordSuccess(savedSubscription.getId());
                return 1;
            }

            boolean shouldDeactivate = statusCode == 404 || statusCode == 410;
            pushSubscriptionService.recordFailure(
                savedSubscription.getId(),
                "Push provider returned HTTP " + statusCode,
                shouldDeactivate
            );
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DeliveryException("Push delivery interrupted", true);
        } catch (Exception exception) {
            pushSubscriptionService.recordFailure(
                savedSubscription.getId(),
                "Push delivery failed: " + safeMessage(exception),
                false
            );
            return 0;
        }
    }

    private String buildPayload(NotificationJob job, NotificationEvent event) {
        JsonNode payload = parseJson(event.getPayload());
        Map<String, String> pushPayload = Map.of(
            "title", truncate(job.getRenderedSubject(), 120),
            "body", truncate(job.getRenderedBody(), 240),
            "deepLink", extractText(payload, "deepLink").orElse("/"),
            "eventType", event.getEventType()
        );

        try {
            return objectMapper.writeValueAsString(pushPayload);
        } catch (JsonProcessingException exception) {
            return "{\"title\":\"Notification\",\"body\":\"You have a new CampusCritique update.\",\"deepLink\":\"/\"}";
        }
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

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() <= 240 ? message : message.substring(0, 240);
    }
}

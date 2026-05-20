package com.npaas.notify.push;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.npaas.notify.common.security.TenantAuthorizationService;

@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final TenantAuthorizationService tenantAuthorizationService;

    public PushSubscriptionService(
            PushSubscriptionRepository pushSubscriptionRepository,
            TenantAuthorizationService tenantAuthorizationService) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @Transactional
    public PushSubscriptionResponse upsert(UpsertPushSubscriptionRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        PushSubscriptionPayload payload = normalizePayload(request);
        PushSubscription subscription = pushSubscriptionRepository
            .findByEndpoint(payload.endpoint())
            .orElseGet(() -> new PushSubscription(
                UUID.randomUUID(),
                request.tenantId(),
                request.userId(),
                payload.endpoint(),
                payload.provider(),
                payload.fcmToken(),
                payload.p256dh(),
                payload.auth(),
                normalizeUserAgent(request.userAgent())
            ));

        subscription.refresh(
            request.tenantId(),
            request.userId(),
            payload.endpoint(),
            payload.provider(),
            payload.fcmToken(),
            payload.p256dh(),
            payload.auth(),
            normalizeUserAgent(request.userAgent())
        );

        return PushSubscriptionResponse.from(pushSubscriptionRepository.save(subscription));
    }

    @Transactional
    public void deactivate(DeletePushSubscriptionRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        String endpoint = normalizeDeleteEndpoint(request);
        pushSubscriptionRepository.findByEndpoint(endpoint)
            .filter(subscription -> subscription.getTenantSlug().equals(request.tenantId()))
            .filter(subscription -> subscription.getRecipientUserId().equals(request.userId()))
            .ifPresent(subscription -> subscription.deactivate("User unsubscribed"));
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> activeForRecipient(String tenantId, String userId) {
        return pushSubscriptionRepository.findByTenantSlugAndRecipientUserIdAndActiveTrue(tenantId, userId);
    }

    @Transactional
    public void recordSuccess(UUID subscriptionId) {
        pushSubscriptionRepository.findById(subscriptionId).ifPresent(PushSubscription::recordSuccess);
    }

    @Transactional
    public void recordFailure(UUID subscriptionId, String message, boolean deactivate) {
        pushSubscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            if (deactivate) {
                subscription.deactivate(message);
            } else {
                subscription.recordFailure(message);
            }
        });
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }

        String trimmed = userAgent.trim();
        return trimmed.length() <= 512 ? trimmed : trimmed.substring(0, 512);
    }

    private PushSubscriptionPayload normalizePayload(UpsertPushSubscriptionRequest request) {
        String provider = normalizeProvider(request.provider());
        if ("FCM".equals(provider)) {
            String token = normalizeRequired(request.token(), "Missing FCM token");
            return new PushSubscriptionPayload(provider, "fcm:" + token, token, null, null);
        }

        String endpoint = normalizeRequired(request.endpoint(), "Missing push endpoint");
        PushSubscriptionKeysRequest keys = request.keys();
        if (keys == null || keys.p256dh() == null || keys.p256dh().isBlank()
                || keys.auth() == null || keys.auth().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing web push keys");
        }

        return new PushSubscriptionPayload(provider, endpoint, null, keys.p256dh().trim(), keys.auth().trim());
    }

    private String normalizeDeleteEndpoint(DeletePushSubscriptionRequest request) {
        if ("FCM".equals(normalizeProvider(request.provider()))) {
            return "fcm:" + normalizeRequired(request.token(), "Missing FCM token");
        }

        return normalizeRequired(request.endpoint(), "Missing push endpoint");
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "WEB_PUSH";
        }
        String normalized = provider.trim().toUpperCase();
        if (!"FCM".equals(normalized) && !"WEB_PUSH".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported push provider");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private record PushSubscriptionPayload(
            String provider,
            String endpoint,
            String fcmToken,
            String p256dh,
            String auth) {
    }
}

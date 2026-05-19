package com.npaas.notify.push;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        PushSubscription subscription = pushSubscriptionRepository
            .findByEndpoint(request.endpoint())
            .orElseGet(() -> new PushSubscription(
                UUID.randomUUID(),
                request.tenantId(),
                request.userId(),
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth(),
                normalizeUserAgent(request.userAgent())
            ));

        subscription.refresh(
            request.tenantId(),
            request.userId(),
            request.keys().p256dh(),
            request.keys().auth(),
            normalizeUserAgent(request.userAgent())
        );

        return PushSubscriptionResponse.from(pushSubscriptionRepository.save(subscription));
    }

    @Transactional
    public void deactivate(DeletePushSubscriptionRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        pushSubscriptionRepository.findByEndpoint(request.endpoint())
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
}

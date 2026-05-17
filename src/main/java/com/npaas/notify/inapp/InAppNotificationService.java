package com.npaas.notify.inapp;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.common.security.TenantAuthorizationService;

@Service
public class InAppNotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final InAppNotificationRepository inAppNotificationRepository;
    private final TenantAuthorizationService tenantAuthorizationService;

    public InAppNotificationService(
            InAppNotificationRepository inAppNotificationRepository,
            TenantAuthorizationService tenantAuthorizationService) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<InAppNotificationResponse> list(String tenantId, String userId, int limit) {
        Pageable pageable = PageRequest.of(0, clampLimit(limit));
        return inAppNotificationRepository
            .findByTenantSlugAndRecipientUserIdOrderByCreatedAtDesc(tenantId, userId, pageable)
            .stream()
            .map(InAppNotificationResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(String tenantId, String userId) {
        long unreadCount = inAppNotificationRepository
            .countByTenantSlugAndRecipientUserIdAndReadAtIsNull(tenantId, userId);
        return new UnreadCountResponse(unreadCount);
    }

    @Transactional
    public InAppNotificationResponse markRead(UUID notificationId) {
        InAppNotification notification = inAppNotificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        tenantAuthorizationService.requireTenant(notification.getTenantSlug());
        notification.markRead();
        return InAppNotificationResponse.from(notification);
    }

    @Transactional
    public UnreadCountResponse markAllRead(String tenantId, String userId) {
        List<InAppNotification> unreadNotifications = inAppNotificationRepository
            .findByTenantSlugAndRecipientUserIdAndReadAtIsNull(tenantId, userId);

        for (InAppNotification notification : unreadNotifications) {
            notification.markRead();
        }

        return new UnreadCountResponse(0);
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}

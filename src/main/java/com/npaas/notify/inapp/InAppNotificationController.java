package com.npaas.notify.inapp;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/in-app-notifications")
public class InAppNotificationController {

    private final InAppNotificationService inAppNotificationService;

    public InAppNotificationController(InAppNotificationService inAppNotificationService) {
        this.inAppNotificationService = inAppNotificationService;
    }

    @GetMapping
    public List<InAppNotificationResponse> list(
            @RequestParam @NotBlank String tenantId,
            @RequestParam @NotBlank String userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return inAppNotificationService.list(tenantId, userId, limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(
            @RequestParam @NotBlank String tenantId,
            @RequestParam @NotBlank String userId) {
        return inAppNotificationService.unreadCount(tenantId, userId);
    }

    @PatchMapping("/{id}/read")
    public InAppNotificationResponse markRead(@PathVariable UUID id) {
        return inAppNotificationService.markRead(id);
    }

    @PatchMapping("/read-all")
    public UnreadCountResponse markAllRead(
            @RequestParam @NotBlank String tenantId,
            @RequestParam @NotBlank String userId) {
        return inAppNotificationService.markAllRead(tenantId, userId);
    }
}

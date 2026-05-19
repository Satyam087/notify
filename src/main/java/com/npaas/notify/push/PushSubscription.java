package com.npaas.notify.push;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Column(name = "recipient_user_id", nullable = false, length = 180)
    private String recipientUserId;

    @Column(nullable = false, columnDefinition = "text")
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, columnDefinition = "text")
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, columnDefinition = "text")
    private String authKey;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushSubscription() {
    }

    public PushSubscription(UUID id, String tenantSlug, String recipientUserId, String endpoint,
            String p256dhKey, String authKey, String userAgent) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.recipientUserId = recipientUserId;
        this.endpoint = endpoint;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
        this.userAgent = userAgent;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dhKey() {
        return p256dhKey;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void refresh(String tenantSlug, String recipientUserId, String p256dhKey, String authKey, String userAgent) {
        this.tenantSlug = tenantSlug;
        this.recipientUserId = recipientUserId;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
        this.userAgent = userAgent;
        this.active = true;
        this.failedAttempts = 0;
        this.lastError = null;
        this.deactivatedAt = null;
    }

    public void recordSuccess() {
        this.lastSuccessAt = Instant.now();
        this.failedAttempts = 0;
        this.lastError = null;
    }

    public void recordFailure(String errorMessage) {
        this.failedAttempts += 1;
        this.lastError = errorMessage;
    }

    public void deactivate(String reason) {
        this.active = false;
        this.deactivatedAt = Instant.now();
        this.lastError = reason;
    }
}

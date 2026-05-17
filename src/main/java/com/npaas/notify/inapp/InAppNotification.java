package com.npaas.notify.inapp;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "in_app_notifications")
public class InAppNotification {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "recipient_user_id", nullable = false, length = 160)
    private String recipientUserId;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InAppNotification() {
    }

    public InAppNotification(UUID id, String tenantSlug, UUID jobId, UUID eventId, String recipientUserId,
            String title, String body) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.jobId = jobId;
        this.eventId = eventId;
        this.recipientUserId = recipientUserId;
        this.title = title;
        this.body = body;
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

    public UUID getJobId() {
        return jobId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}

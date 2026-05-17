package com.npaas.notify.events;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_events")
public class NotificationEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String recipient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationEventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationEvent() {
    }

    public NotificationEvent(UUID id, String tenantSlug, String eventType, String idempotencyKey, String recipient,
            String payload, NotificationEventStatus status) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.eventType = eventType;
        this.idempotencyKey = idempotencyKey;
        this.recipient = recipient;
        this.payload = payload;
        this.status = status;
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

    public String getEventType() {
        return eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getPayload() {
        return payload;
    }

    public NotificationEventStatus getStatus() {
        return status;
    }

    public void markQueued() {
        status = NotificationEventStatus.QUEUED;
    }

    public void markProcessing() {
        status = NotificationEventStatus.PROCESSING;
    }

    public void markCompleted() {
        status = NotificationEventStatus.COMPLETED;
    }

    public void markFailed() {
        status = NotificationEventStatus.FAILED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

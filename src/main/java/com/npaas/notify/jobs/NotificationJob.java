package com.npaas.notify.jobs;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_jobs")
public class NotificationJob {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationJobStatus status;

    @Column(name = "rendered_subject", nullable = false, length = 240)
    private String renderedSubject;

    @Column(name = "rendered_body", nullable = false, columnDefinition = "text")
    private String renderedBody;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationJob() {
    }

    public NotificationJob(UUID id, UUID eventId, UUID templateId, String tenantSlug, NotificationChannel channel,
            NotificationJobStatus status, String renderedSubject, String renderedBody) {
        this.id = id;
        this.eventId = eventId;
        this.templateId = templateId;
        this.tenantSlug = tenantSlug;
        this.channel = channel;
        this.status = status;
        this.renderedSubject = renderedSubject;
        this.renderedBody = renderedBody;
        this.attempts = 0;
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

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationJobStatus getStatus() {
        return status;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public String getRenderedBody() {
        return renderedBody;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        status = NotificationJobStatus.PROCESSING;
    }

    public void markSent() {
        status = NotificationJobStatus.SENT;
        nextAttemptAt = null;
    }

    public void scheduleRetry(Instant nextAttemptAt) {
        attempts += 1;
        status = NotificationJobStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markPending(Instant nextAttemptAt) {
        status = NotificationJobStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markFailed() {
        attempts += 1;
        status = NotificationJobStatus.FAILED;
        nextAttemptAt = null;
    }
}

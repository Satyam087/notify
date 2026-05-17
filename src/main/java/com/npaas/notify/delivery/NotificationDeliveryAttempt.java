package com.npaas.notify.delivery;

import java.time.Instant;
import java.util.UUID;

import com.npaas.notify.jobs.NotificationChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_delivery_attempts")
public class NotificationDeliveryAttempt {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationDeliveryAttemptStatus status;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(name = "provider_message_id", length = 240)
    private String providerMessageId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected NotificationDeliveryAttempt() {
    }

    private NotificationDeliveryAttempt(UUID id, UUID jobId, String tenantSlug, NotificationChannel channel,
            int attemptNumber, NotificationDeliveryAttemptStatus status, String provider, String providerMessageId,
            String errorMessage) {
        this.id = id;
        this.jobId = jobId;
        this.tenantSlug = tenantSlug;
        this.channel = channel;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
    }

    public static NotificationDeliveryAttempt success(UUID jobId, String tenantSlug, NotificationChannel channel,
            int attemptNumber, String provider, String providerMessageId) {
        return new NotificationDeliveryAttempt(UUID.randomUUID(), jobId, tenantSlug, channel, attemptNumber,
            NotificationDeliveryAttemptStatus.SUCCESS, provider, providerMessageId, null);
    }

    public static NotificationDeliveryAttempt failed(UUID jobId, String tenantSlug, NotificationChannel channel,
            int attemptNumber, String provider, String errorMessage) {
        return new NotificationDeliveryAttempt(UUID.randomUUID(), jobId, tenantSlug, channel, attemptNumber,
            NotificationDeliveryAttemptStatus.FAILED, provider, null, truncate(errorMessage));
    }

    @PrePersist
    void onCreate() {
        attemptedAt = Instant.now();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public NotificationDeliveryAttemptStatus getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}

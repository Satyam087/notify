package com.npaas.notify.templates;

import java.time.Instant;
import java.util.UUID;

import com.npaas.notify.jobs.NotificationChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

    @Id
    private UUID id;

    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "template_key", nullable = false, length = 160)
    private String templateKey;

    @Column(name = "subject_template", nullable = false, length = 240)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationTemplate() {
    }

    public NotificationTemplate(UUID id, String tenantSlug, String eventType, NotificationChannel channel,
            String templateKey, String subjectTemplate, String bodyTemplate, boolean enabled) {
        this.id = id;
        this.tenantSlug = tenantSlug;
        this.eventType = eventType;
        this.channel = channel;
        this.templateKey = templateKey;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.enabled = enabled;
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

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String eventType, NotificationChannel channel, String subjectTemplate, String bodyTemplate,
            boolean enabled) {
        this.eventType = eventType;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.enabled = enabled;
    }
}

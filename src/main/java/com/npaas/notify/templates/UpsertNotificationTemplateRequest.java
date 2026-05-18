package com.npaas.notify.templates;

import com.npaas.notify.jobs.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertNotificationTemplateRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,119}$") String eventType,
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 240) String subjectTemplate,
        @NotBlank @Size(max = 10000) String bodyTemplate,
        boolean enabled) {
}

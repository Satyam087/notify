package com.npaas.notify.events;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngestEventRequest(
        @NotBlank @Size(max = 80) String tenantId,
        @NotBlank @Size(max = 120) String eventType,
        @NotBlank @Size(max = 180) String idempotencyKey,
        @NotNull JsonNode recipient,
        @NotNull JsonNode payload) {
}

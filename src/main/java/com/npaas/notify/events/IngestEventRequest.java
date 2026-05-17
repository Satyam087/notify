package com.npaas.notify.events;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IngestEventRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,119}$") String eventType,
        @NotBlank @Size(max = 180) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,179}$") String idempotencyKey,
        @NotNull JsonNode recipient,
        @NotNull JsonNode payload) {
}

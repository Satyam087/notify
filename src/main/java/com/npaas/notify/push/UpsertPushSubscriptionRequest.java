package com.npaas.notify.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertPushSubscriptionRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
        @NotBlank @Size(max = 180) String userId,
        @NotBlank @Size(max = 2048) String endpoint,
        @NotNull @Valid PushSubscriptionKeysRequest keys,
        @Size(max = 512) String userAgent) {
}

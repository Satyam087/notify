package com.npaas.notify.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertPushSubscriptionRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
        @NotBlank @Size(max = 180) String userId,
        @Size(max = 30) String provider,
        @Size(max = 4096) String token,
        @Size(max = 4096) String endpoint,
        @Valid PushSubscriptionKeysRequest keys,
        @Size(max = 512) String userAgent) {
}

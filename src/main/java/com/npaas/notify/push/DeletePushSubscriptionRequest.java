package com.npaas.notify.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeletePushSubscriptionRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
        @NotBlank @Size(max = 180) String userId,
        @Size(max = 30) String provider,
        @Size(max = 4096) String token,
        @Size(max = 4096) String endpoint) {
}

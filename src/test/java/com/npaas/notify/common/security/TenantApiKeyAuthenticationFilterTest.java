package com.npaas.notify.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TenantApiKeyAuthenticationFilterTest {

    private final TenantApiKeyAuthenticationFilter filter = new TenantApiKeyAuthenticationFilter(
        mock(TenantApiKeyRepository.class),
        new ApiKeyHasher()
    );

    @Test
    void protectsEveryTenantScopedApiPath() {
        for (String path : new String[] {
            "/api/v1/events", "/api/v1/events/status", "/api/v1/in-app-notifications/x",
            "/api/v1/jobs/failed", "/api/v1/metrics", "/api/v1/push-subscriptions", "/api/v1/templates/key"
        }) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            assertThat(filter.shouldNotFilter(request)).as(path).isFalse();
        }
    }

    @Test
    void leavesPublicPathsAlone() {
        for (String path : new String[] {"/api/health-check", "/actuator/health", "/api/v1/metricsx"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            assertThat(filter.shouldNotFilter(request)).as(path).isTrue();
        }
    }
}

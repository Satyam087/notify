package com.npaas.notify.metrics;

import java.time.Instant;
import java.util.List;

public record MetricsResponse(
        String tenantId,
        Instant generatedAt,
        int days,
        DeliveryTotals tenant,
        List<ChannelMetrics> byChannel,
        List<DailyMetrics> byDay,
        PlatformMetrics platform) {
}

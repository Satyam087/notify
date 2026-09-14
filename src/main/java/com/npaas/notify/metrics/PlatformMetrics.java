package com.npaas.notify.metrics;

import java.util.List;

/**
 * Counts across every tenant. Only volumes, never content or recipients, and only
 * handed to tenants listed in {@code notify.metrics.platform-tenants}.
 */
public record PlatformMetrics(long tenants, long pushSubscriptions, DeliveryTotals totals, List<ChannelMetrics> byChannel) {
}

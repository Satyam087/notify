package com.npaas.notify.metrics;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {

    private static final String TOTALS_SQL = """
        SELECT
            (SELECT count(*) FROM notification_events e WHERE (:tenant IS NULL OR e.tenant_slug = :tenant)) AS events,
            (SELECT count(*) FROM notification_jobs j WHERE (:tenant IS NULL OR j.tenant_slug = :tenant)) AS jobs,
            (SELECT count(*) FROM notification_jobs j WHERE (:tenant IS NULL OR j.tenant_slug = :tenant) AND j.status = 'SENT') AS sent,
            (SELECT count(*) FROM notification_jobs j WHERE (:tenant IS NULL OR j.tenant_slug = :tenant) AND j.status = 'FAILED') AS failed,
            (SELECT count(*) FROM notification_jobs j WHERE (:tenant IS NULL OR j.tenant_slug = :tenant) AND j.status IN ('PENDING', 'PROCESSING')) AS pending,
            (SELECT count(*) FROM notification_delivery_attempts a WHERE (:tenant IS NULL OR a.tenant_slug = :tenant)) AS attempts,
            (SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY extract(epoch FROM (a.attempted_at - j.created_at)) * 1000)
               FROM notification_delivery_attempts a
               JOIN notification_jobs j ON j.id = a.job_id
              WHERE a.status = 'SUCCESS' AND (:tenant IS NULL OR a.tenant_slug = :tenant)) AS median_delivery_ms,
            (SELECT min(created_at) FROM notification_events e WHERE (:tenant IS NULL OR e.tenant_slug = :tenant)) AS first_event_at,
            (SELECT max(created_at) FROM notification_events e WHERE (:tenant IS NULL OR e.tenant_slug = :tenant)) AS last_event_at
        """;

    private static final String BY_CHANNEL_SQL = """
        SELECT channel,
               count(*) AS jobs,
               count(*) FILTER (WHERE status = 'SENT') AS sent,
               count(*) FILTER (WHERE status = 'FAILED') AS failed
          FROM notification_jobs
         WHERE (:tenant IS NULL OR tenant_slug = :tenant)
         GROUP BY channel
         ORDER BY channel
        """;

    private static final String BY_DAY_SQL = """
        SELECT (created_at AT TIME ZONE 'UTC')::date AS day,
               count(*) AS jobs,
               count(*) FILTER (WHERE status = 'SENT') AS sent,
               count(*) FILTER (WHERE status = 'FAILED') AS failed
          FROM notification_jobs
         WHERE tenant_slug = :tenant
           AND created_at >= now() - make_interval(days => :days)
         GROUP BY 1
         ORDER BY 1
        """;

    private static final String PLATFORM_COUNTS_SQL = """
        SELECT (SELECT count(*) FROM tenants WHERE status = 'ACTIVE') AS tenants,
               (SELECT count(*) FROM push_subscriptions) AS push_subscriptions
        """;

    private final JdbcClient jdbcClient;
    private final Set<String> platformTenants;

    public MetricsService(
            JdbcClient jdbcClient,
            @Value("${notify.metrics.platform-tenants:}") String platformTenantsProperty) {
        this.jdbcClient = jdbcClient;
        this.platformTenants = Arrays.stream(platformTenantsProperty.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public MetricsResponse metrics(String tenantId, int days) {
        DeliveryTotals tenant = totals(tenantId);

        List<ChannelMetrics> byChannel = byChannel(tenantId);

        List<DailyMetrics> byDay = jdbcClient.sql(BY_DAY_SQL)
            .param("tenant", tenantId)
            .param("days", days)
            .query((rs, rowNum) -> new DailyMetrics(
                rs.getDate("day").toLocalDate(),
                rs.getLong("jobs"),
                rs.getLong("sent"),
                rs.getLong("failed")))
            .list();

        PlatformMetrics platform = platformTenants.contains(tenantId) ? platform() : null;

        return new MetricsResponse(tenantId, Instant.now(), days, tenant, byChannel, byDay, platform);
    }

    private PlatformMetrics platform() {
        Map<String, Object> counts = jdbcClient.sql(PLATFORM_COUNTS_SQL).query().singleRow();
        return new PlatformMetrics(
            ((Number) counts.get("tenants")).longValue(),
            ((Number) counts.get("push_subscriptions")).longValue(),
            totals(null),
            byChannel(null));
    }

    private List<ChannelMetrics> byChannel(String tenantId) {
        return jdbcClient.sql(BY_CHANNEL_SQL)
            .param("tenant", tenantId, java.sql.Types.VARCHAR)
            .query((rs, rowNum) -> new ChannelMetrics(
                rs.getString("channel"),
                rs.getLong("jobs"),
                rs.getLong("sent"),
                rs.getLong("failed")))
            .list();
    }

    private DeliveryTotals totals(String tenantId) {
        return jdbcClient.sql(TOTALS_SQL)
            .param("tenant", tenantId, java.sql.Types.VARCHAR)
            .query((rs, rowNum) -> {
                long sent = rs.getLong("sent");
                long failed = rs.getLong("failed");
                double median = rs.getDouble("median_delivery_ms");
                boolean medianMissing = rs.wasNull();
                return new DeliveryTotals(
                    rs.getLong("events"),
                    rs.getLong("jobs"),
                    sent,
                    failed,
                    rs.getLong("pending"),
                    rs.getLong("attempts"),
                    DeliveryTotals.successRate(sent, failed),
                    medianMissing ? null : Math.round(median),
                    toInstant(rs.getTimestamp("first_event_at")),
                    toInstant(rs.getTimestamp("last_event_at")));
            })
            .single();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

package com.npaas.notify.metrics;

import java.time.Instant;

public record DeliveryTotals(
        long events,
        long jobs,
        long sent,
        long failed,
        long pending,
        long attempts,
        Double successRate,
        Long medianDeliveryMs,
        Instant firstEventAt,
        Instant lastEventAt) {

    /**
     * Share of finished jobs that were delivered. Null until at least one job has finished,
     * so a fresh tenant reports "no data" instead of a misleading 0 or 100.
     */
    static Double successRate(long sent, long failed) {
        long finished = sent + failed;
        if (finished == 0) {
            return null;
        }
        return Math.round((sent * 10000.0) / finished) / 10000.0;
    }
}

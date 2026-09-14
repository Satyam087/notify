package com.npaas.notify.metrics;

import java.time.LocalDate;

public record DailyMetrics(LocalDate day, long jobs, long sent, long failed) {
}

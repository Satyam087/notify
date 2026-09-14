package com.npaas.notify.metrics;

public record ChannelMetrics(String channel, long jobs, long sent, long failed) {
}

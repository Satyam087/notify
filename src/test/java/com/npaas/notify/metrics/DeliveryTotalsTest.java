package com.npaas.notify.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeliveryTotalsTest {

    @Test
    void successRateIsNullUntilAJobHasFinished() {
        assertThat(DeliveryTotals.successRate(0, 0)).isNull();
    }

    @Test
    void successRateIsSentOverFinishedRoundedToFourPlaces() {
        assertThat(DeliveryTotals.successRate(1321, 25)).isEqualTo(0.9814);
        assertThat(DeliveryTotals.successRate(3, 0)).isEqualTo(1.0);
        assertThat(DeliveryTotals.successRate(0, 2)).isEqualTo(0.0);
    }
}

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package common.metrics;

import io.prometheus.client.Counter;

public class RopMetrics {
    private static RopMetrics instance;

    private final Counter rop_count = Counter
            .build().name("rop_count_total").labelNames("period").help("Number of ROPs completed").register();

    private final Counter rop_duration = Counter
            .build().name("rop_duration_total").labelNames("period").help("Seconds taken to complete ROPs").register();

    private final Counter rop_failed = Counter
            .build().name("rop_failed_total").labelNames("period")
            .help("Number of ROPs where the duration exceeded the period").register();

    private final Counter rop_count_over_expected_avg = Counter
            .build().name("rop_count_over_expected_average").labelNames("period","expectedDuration")
            .help("Number of ROPs where the duration exceeded the expected duration in seconds for ROP").register();

    public static synchronized RopMetrics getInstance() {
        if (instance == null) {
            instance = new RopMetrics();
        }
        return instance;
    }

    public void reportRop(final int period, final long durationMSec) {
        final String name = "average" + String.valueOf(period) + "secRopDuration";
        final String periodAsStr = String.valueOf(period);
        final int ropExpectedPeriod = Integer.getInteger(name, period);
        final String expectedPeriodAsStr = String.valueOf(ropExpectedPeriod);
        double durationSec = (double)durationMSec / 1000;
        rop_count.labels(periodAsStr).inc();
        rop_duration.labels(periodAsStr).inc(durationSec);
        if (durationMSec > (period*1000)) {
            rop_failed.labels(periodAsStr).inc();
        }
        if (durationMSec > (ropExpectedPeriod*1000)) {
            rop_count_over_expected_avg.labels(periodAsStr,expectedPeriodAsStr).inc();
        }
    }

    public int getAverageRopDuration(final int period) {
        final String periodAsStr = String.valueOf(period);
        final double ropDurationAverage = rop_duration.labels(periodAsStr).get() / rop_count.labels(periodAsStr).get();
        return (int) Math.ceil(ropDurationAverage);
    }

    public int getFailedRopTotal(final int period) {
        final String periodAsStr = String.valueOf(period);
        return (int) rop_failed.labels(periodAsStr).get();
    }

    public int getRopCountOverExpectedAvg(final int period) {
        final String periodAsStr = String.valueOf(period);
        final String name = "average" + String.valueOf(period) + "secRopDuration";
        final int ropExpectedPeriod = Integer.getInteger(name, period);
        final String expectedPeriodAsStr = String.valueOf(ropExpectedPeriod);
        return (int) rop_count_over_expected_avg.labels(periodAsStr,expectedPeriodAsStr).get();
    }

    public int getRopTotal(final int period) {
        final String periodAsStr = String.valueOf(period);
        return (int) rop_count.labels(periodAsStr).get();
    }

    private RopMetrics() {

    }
}

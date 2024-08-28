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
package common.cniv;

import common.RnsWorkload;
import org.json.JSONArray;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ProgressReporterScheduler implements Runnable {
    private final static Logger logger = Logger.getLogger(ProgressReporterScheduler.class.getName());

    final long reportingPeriodMs;
    Map<Integer, List<RnsWorkload>> workload;
    final String reportType ;

    public ProgressReporterScheduler( final Map<Integer, List<RnsWorkload>> workload, final long reportingPeriodMs, final String reportType ) {
        this.reportingPeriodMs = reportingPeriodMs;
        this.workload = workload;
        this.reportType = reportType;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(reportingPeriodMs);
            } catch (InterruptedException ignored) {}
            final JSONArray benchmarkResult = new JSONArray();
            for (Integer ropPeriod : workload.keySet()) {
                try {
                    JSONArray benchmarkReport = GenerateReport.getInstance().generateRopReport(ropPeriod * 60, reportType);
                    benchmarkResult.putAll(benchmarkReport);
                } catch (IOException e) {}
            }

            if (Boolean.getBoolean("cnivAgentEnabled")) {
                try {
                    ReportSender.getInstance().sendReport(reportType, benchmarkResult);
                } catch (IOException e) {}
            }

        }
    }
}


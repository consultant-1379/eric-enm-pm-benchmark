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

import common.Log;
import common.metrics.RopMetrics;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GenerateReport {
    private static GenerateReport reporter;
    private final String finalResult= "Result";

    public static GenerateReport getInstance() {
        if (reporter == null) {
            reporter = new GenerateReport();
        }
        return reporter;
    }


    public JSONArray generateRopReport(int ropType, final String reportType) throws IOException {
        final JSONArray benchMarkResult = new JSONArray();
        benchMarkResult.put(getTotalRopReport(ropType,reportType));
        benchMarkResult.put(getRopReportOverExpectedAvg(ropType,reportType));
        benchMarkResult.put(getAverageRopDuration(ropType,reportType));
        Log.log("GenerateReporter");
        return benchMarkResult;
    }

    private JSONObject getTotalRopReport(int ropType,final String reportType) {
        final int achievedResult = RopMetrics.getInstance().getRopTotal(ropType);
        final String name = "Total" + String.valueOf(ropType) + "secRop";
        final String description = "The number of total " + ropType / 60 + "min ROPs for the benchmark";
        final String status = (reportType.contains(finalResult)) ? "PASS" : "RUNNING";
        return jsonReport(name, description, achievedResult + "", "NA", status,reportType);
    }

    private JSONObject getAverageRopDuration(int ropType,final String reportType) {
        final int achievedResult = RopMetrics.getInstance().getAverageRopDuration(ropType);
        final String name = "average" + String.valueOf(ropType) + "secRopDuration";
        final int expectedValue = Integer.getInteger(name, ropType);
        final String status = (achievedResult > expectedValue) ? "FAIL" : "PASS";
        final String description = " The average file-collection duration(sec) per ROP of the given type for the benchmark duration.";
        return jsonReport(name, description, achievedResult + "s", "<" + expectedValue + "s", status,reportType);
    }

    private JSONObject getRopReportOverExpectedAvg(int ropType, final String reportType) {
        final String expectedDurationName = "average" + String.valueOf(ropType) + "secRopDuration";
        final int expectedDuration = Integer.getInteger(expectedDurationName, ropType);
        final int achievedResult = RopMetrics.getInstance().getRopCountOverExpectedAvg(ropType);
        final String name = "Failed" + String.valueOf(ropType) + "secRops";
        final int expectedValue = Integer.getInteger(name, 0);
        final String status = (achievedResult > expectedValue) ? "FAIL" : "PASS";
        final String description = "The number of failed " + ropType / 60 + "min ROPs where the duration exceeded the expected "+expectedDuration+"s duration";
        return jsonReport(name, description, String.valueOf(achievedResult), String.valueOf(expectedValue), status, reportType);
    }

    private JSONObject jsonReport(final String name, final String description, final String achievedResult,
            final String expectedResult, final String status, final String reportType) {
        final JSONObject ropResport = new JSONObject();
        ropResport.put("name", name);
        ropResport.put("subRowDesc", description);
        ropResport.put("achievedResult", String.valueOf(achievedResult));
        ropResport.put("expectedResult", expectedResult);
        ropResport.put("status", status);
        Log.log(reportType+": " + ropResport);
        return ropResport;
    }

}
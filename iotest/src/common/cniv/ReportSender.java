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

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import common.Log;


public class ReportSender {
    private static ReportSender reporter;

    public static ReportSender getInstance() {
        if (reporter == null) {
            reporter = new ReportSender();
        }
        return reporter;
    }

    public void sendReport(final String reportType, final JSONArray benchMarkResult) throws IOException {
        final JSONObject resultsReport = new JSONObject();
        resultsReport.put("report", benchMarkResult);
        resultsReport.put("description", reportType+ " of ENM PM file collection benchmark");
        final String url = System.getProperty("cnivAgentURL");
        final String benchmarkGroup = System.getProperty("cnivGroup");
        final String benchmarkName = System.getProperty("cnivName");
        final CloseableHttpClient client = HttpClients.createDefault();
        final HttpPost httpPost = new HttpPost("http://" + url + "/result/" + benchmarkGroup + "/" + benchmarkName);

        final StringEntity entity = new StringEntity(resultsReport.toString());
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        final CloseableHttpResponse response = client.execute(httpPost);
        Log.log(reportType+": CNIV POST return code " + response.getCode());
        client.close();

    }

}
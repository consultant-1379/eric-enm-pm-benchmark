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

import java.io.IOException;

import common.Log;
import io.prometheus.client.exporter.HTTPServer;

public class Exporter {
    private static HTTPServer httpServer;

    public static void start() throws IOException {
        final int port = Integer.getInteger("metrics_port", 8888).intValue();
        if (port != -1) {
            Log.log("Starting metrics export on port " + port);
            httpServer = new HTTPServer.Builder()
                .withPort(port)
                .build();
        }
    }
}

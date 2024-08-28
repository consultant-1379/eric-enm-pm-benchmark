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
package common;

import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    public static void log(String msg) {
        System.out.println(new Date() + " " + Thread.currentThread().getName() +" " + msg);
    }

    public static void err(String msg) { System.err.println(new Date() + " " + msg); }

    public static void initTrace() {
        if (System.getProperty("loglevel") != null) {
            Level level = Level.parse(System.getProperty("loglevel"));

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(level);

            TraceFormatter tf = new TraceFormatter();
            for (Handler handler : rootLogger.getHandlers()) {
                handler.setLevel(level);
                handler.setFormatter(tf);
            }
        }
    }
}

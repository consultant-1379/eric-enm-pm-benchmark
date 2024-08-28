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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TraceFormatter extends Formatter {
    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");  //use time and date format
    private static final String lineSep = System.getProperty("line.separator");

    public String format(LogRecord record) {
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "root";
        }
        StringBuilder output = new StringBuilder()
                .append(df.format(new Date(record.getMillis())))
                .append(loggerName)
                .append(" [").append(Thread.currentThread().getName()).append("] ")
                .append(formatMessage(record))
                .append(lineSep);
        return output.toString();

    }

}

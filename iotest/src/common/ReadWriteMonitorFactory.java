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

public class ReadWriteMonitorFactory {
    public static IReadWriteMonitor create() {
        int period = Integer.getInteger("rwmonitor", 0);
        if (period > 0) {
            ReadWriteMonitor rwMon = new ReadWriteMonitor(period * 1000L);
            new Thread(rwMon, "RW Monitor").start();
            return rwMon;
        } else {
            return new NoopReadWriteMonitor();
        }
    }
}

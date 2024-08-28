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

public class FileSystemOpMonitorFactory {
    public static IFileSystemOpMonitor create() {
        int fsMonInterval = Integer.getInteger("fsmonitor", 0);
        if (fsMonInterval > 0) {
            FileSystemOpMonitor fsm = new FileSystemOpMonitor(fsMonInterval * 1000L);
            new Thread(fsm, "FileSystemOpMonitor").start();
            return fsm;
        } else {
            return new NoopFileSystemOpMonitor();
        }

    }
}

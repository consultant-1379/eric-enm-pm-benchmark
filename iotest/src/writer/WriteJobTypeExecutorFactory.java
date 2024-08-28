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
package writer;

import common.FilesJob;
import common.IFileSystemOpMonitor;
import common.IJobTypeExecutorFactory;
import common.IReadWriteMonitor;
import common.IRopMonitor;

public class WriteJobTypeExecutorFactory implements IJobTypeExecutorFactory {
    final IReadWriteMonitor rwMonitor;
    final IFileSystemOpMonitor fsMonitor;

    public WriteJobTypeExecutorFactory(final IReadWriteMonitor rwMonitor, final IFileSystemOpMonitor fsMonitor) {
        this.rwMonitor = rwMonitor;
        this.fsMonitor = fsMonitor;
    }

    @Override
    public Runnable createExecutor(final FilesJob job, final IRopMonitor ropMonitor) {
        return new FilesWriter(
            job.node,
            BandwidthManager.getInstance().getThrottle(job.node),
            job.files,
            ropMonitor,
            fsMonitor,
            rwMonitor,
            job.priority
        );
    }

}

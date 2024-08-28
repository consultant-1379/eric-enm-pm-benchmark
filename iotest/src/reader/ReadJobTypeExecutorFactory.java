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
package reader;

import common.FilesJob;
import common.IJobTypeExecutorFactory;
import common.IReadWriteMonitor;
import common.IRopMonitor;

public class ReadJobTypeExecutorFactory implements IJobTypeExecutorFactory {
    final IReadWriteMonitor rwMonitor;

    public ReadJobTypeExecutorFactory(final IReadWriteMonitor rwMonitor) {
        this.rwMonitor = rwMonitor;
    }

    @Override
    public Runnable createExecutor(final FilesJob job, final IRopMonitor ropMonitor) {
        return new FilesReader(
            job.node,
            job.files,
            ropMonitor,
            rwMonitor,
            job.priority
        );
    }

}

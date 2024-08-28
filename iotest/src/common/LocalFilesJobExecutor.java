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

import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalFilesJobExecutor implements IFilesJobsExecutor {
    final PriorityBlockingQueue<Runnable> normQ = new PriorityBlockingQueue<>();
    final ThreadPoolExecutor es;
    public LocalFilesJobExecutor() {
        int normPoolSize = Integer.getInteger("lowThreads", 200);
        Log.log("Configuring normal pool with " + normPoolSize + " threads");
        es = new ThreadPoolExecutor(normPoolSize, normPoolSize, 0, TimeUnit.SECONDS, normQ);
        WorkMonitor.getInstance().addTPE(es);
    }

    @Override
    public void executeFileJobs(final List<FilesJob> jobs, final IRopMonitor ropMonitor) {
        final JobExecutorFactory factory = JobExecutorFactory.getInstance();
        for ( final FilesJob job : jobs) {
            es.execute(factory.getTypeExecutor(job, ropMonitor));
        }
        ropMonitor.awaitCompletion();
    }

    @Override
    public void shutDown() {
        es.shutdownNow();
    }

}

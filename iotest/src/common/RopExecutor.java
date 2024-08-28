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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class RopExecutor implements Runnable {
    private final static Logger logger = Logger.getLogger(RopScheduler.class.getName());

    class RopData {
        final RopMonitor ropMonitor;
        final List<FilesJob> jobs;

        RopData(final RopMonitor ropMonitor, final List<FilesJob> jobs) {
            this.ropMonitor = ropMonitor;
            this.jobs = jobs;
        }
    }

    BlockingQueue<RopData> ropQ = new LinkedBlockingQueue<RopData>();
    final IFilesJobsExecutor es;

    RopExecutor(IFilesJobsExecutor es) {
        this.es = es;
    }

    public void execute(final RopMonitor ropMonitor, final List<FilesJob> jobs) {
        final RopData ropData = new RopData(ropMonitor, jobs);
        logger.log(Level.FINER, "execute: ropData={0} ", new Object[] { ropData });
        ropQ.add(ropData);
    }

    public void run() {
        try {
            boolean keepGoing = true;
            while (keepGoing) {
                RopData ropData = ropQ.take();
                logger.log(Level.FINER, "run: ropData={0} ", new Object[] { ropData });
                if (ropData != null) {
                    Log.log("RopExecutor: Executing " + ropData.jobs.size() + " jobs for " + ropData.ropMonitor.desc + " backlog " + ropQ.size());
                    es.executeFileJobs(ropData.jobs, ropData.ropMonitor);
                    ropData.ropMonitor.awaitCompletion();
                } else {
                    keepGoing = false;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
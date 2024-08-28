package common.rmi;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import common.FilesJob;
import common.LocalFilesJobExecutor;
import common.Log;
import common.RopMonitor;

public class WorkerRemoteFilesJobExecutor implements IRemoteFilesJobsExecutor {
    final LocalFilesJobExecutor es;
    final CountDownLatch shutdownCalled = new CountDownLatch(1);

    public WorkerRemoteFilesJobExecutor(final LocalFilesJobExecutor es) {
        this.es = es;
    }

    @Override
    public void executeFileJobs(List<FilesJob> jobs, IRemoteRopMonitor ropMonitor, final String desc) throws RemoteException {
        final RopMonitor localRopMonitor = new RopMonitor(desc);
        localRopMonitor.setNumJobs(jobs.size());

        es.executeFileJobs(jobs, localRopMonitor);
        localRopMonitor.awaitCompletion();

        ropMonitor.jobsCompleted(
                jobs.size(),
                localRopMonitor.getFiles(),
                localRopMonitor.getKB(),
                localRopMonitor.getOpenTime(),
                localRopMonitor.getCloseTime(),
                localRopMonitor.getIoTime()
        );
    }

    @Override
    public void shutdown() throws RemoteException {
        Log.log("shutdown called");
        es.shutDown();
        shutdownCalled.countDown();
    }

    public void awaitShutdown() throws InterruptedException {
        while (shutdownCalled.getCount() > 0) {
            shutdownCalled.await();
        }
    }
}

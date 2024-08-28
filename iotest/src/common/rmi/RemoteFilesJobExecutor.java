package common.rmi;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.FilesJob;
import common.IFilesJobsExecutor;
import common.IRopMonitor;
import common.Log;
import common.RnsWorkload;

public class RemoteFilesJobExecutor implements IFilesJobsExecutor, IJobController {
    private final static Logger logger = Logger.getLogger(RemoteFilesJobExecutor.class.getName());

    final Map<Integer, List<RnsWorkload>> workload;
    final Map<String, Integer> blockSizes;
    final ExecutorService es = Executors.newCachedThreadPool();
    final Map<String, IRemoteFilesJobsExecutor> stickyMap = new HashMap<>();

    final IRemoteFilesJobsExecutor workers[];
    int workersRegistered = 0;
    int nextWorker = 0;

    public RemoteFilesJobExecutor(
            final Map<Integer, List<RnsWorkload>> workload,
            final Map<String, Integer> blockSizes,
            int numWorkers) {
        this.workload = workload;
        this.blockSizes = blockSizes;
        workers = new IRemoteFilesJobsExecutor[numWorkers];
    }

    @Override
    public void executeFileJobs(List<FilesJob> jobs, IRopMonitor ropMonitor) {
        final Map<IRemoteFilesJobsExecutor, List<FilesJob>> batches = new HashMap<>();
        synchronized (stickyMap) {
            for (final IRemoteFilesJobsExecutor worker : workers) {
                batches.put(worker, new LinkedList<FilesJob>());
            }

            for (final FilesJob job : jobs) {
                IRemoteFilesJobsExecutor worker = stickyMap.get(job.node);
                if (worker == null) {
                    worker = workers[nextWorker];
                    nextWorker++;
                    if (nextWorker >= workers.length) {
                        nextWorker = 0;
                    }
                    stickyMap.put(job.node, worker);
                }
                batches.get(worker).add(job);
            }
        }
        logger.log(Level.FINEST, "executeFileJobs: batches {0}", batches);

        ropMonitor.jobStarted();

        final RemoteRopMonitor monitor = new RemoteRopMonitor(ropMonitor);
        try {
            final IRemoteRopMonitor stub = (IRemoteRopMonitor) UnicastRemoteObject.exportObject(monitor, 0);

            for (final Map.Entry<IRemoteFilesJobsExecutor, List<FilesJob>> entry : batches.entrySet()) {
                final Runnable remoteCall = new Runnable() {
                    public void run() {
                        try {
                            logger.log(Level.FINE, "executeFileJobs: calling {0}", entry.getKey());
                            entry.getKey().executeFileJobs(
                                    entry.getValue(),
                                    stub,
                                    ropMonitor.getDescription());
                        } catch (RemoteException e) {
//                            e.printStackTrace();
                            Log.err("WriterConnectionError: "+e.getMessage());
                            if (e instanceof UnmarshalException){
                                Log.err("StorageError: Check the storage space");
                            }
                            else{
                                Log.err("WriterConnectionError: Check the writer pods logs for error");
                            }
                            shutDownWorker();
                            System.exit(1);
                        }
                    }
                };
                es.submit(remoteCall);
            }

            final Runnable unexport = new Runnable() {
                public void run() {
                    ropMonitor.awaitCompletion();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        UnicastRemoteObject.unexportObject(monitor, true);
                    } catch (NoSuchObjectException e) {
                        e.printStackTrace();
                    }
                }
            };
            es.submit(unexport);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "executeFileJobs", e);
            e.printStackTrace();
        }

        ropMonitor.awaitCompletion();
    }

    @Override
    public void registerWorker(IRemoteFilesJobsExecutor worker) {
        logger.log(Level.FINE, "registerWorker: workersRegistered={0} worker={1}",
                new Object[] { Integer.valueOf(workersRegistered), worker });
        synchronized (this.workers) {
            workers[workersRegistered] = worker;
            workersRegistered++;
            workers.notify();
        }
    }

    @Override
    public Map<Integer, List<RnsWorkload>> getWorkLoad() {
        return workload;
    }

    @Override
    public Map<String, Integer> getBlockSizes() throws RemoteException {
        return blockSizes;
    }

    public void awaitWorkers() {
        synchronized (this.workers) {
            while (workersRegistered < workers.length) {
                try {
                    workers.wait();
                } catch (InterruptedException ignored) {
                }
                Log.log("Writers Registered: " + workersRegistered);
            }
        }
    }

    @Override
    public void shutDown() {
        for (final IRemoteFilesJobsExecutor worker : workers) {
            try {
                worker.shutdown();
            } catch (RemoteException e) {
                logger.log(
                    Level.WARNING,
                    "shudown call failed for {}: {}",
                    new Object[] { worker, e.getMessage() }
                );
            }
        }
    }

    public void shutDownWorker() {
        for (final IRemoteFilesJobsExecutor worker : workers) {
            try {
                worker.shutdown();
            } catch (RemoteException e) {}
        }
    }
}

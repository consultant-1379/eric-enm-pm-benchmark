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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import common.FileSystemOpMonitorFactory;
import common.FilesJob;
import common.IFileSystemOpMonitor;
import common.IFilesJobsExecutor;
import common.IReadWriteMonitor;
import common.JobExecutorFactory;
import common.LocalFilesJobExecutor;
import common.Log;
import common.PathMapper;
import common.ReadWriteMonitorFactory;
import common.RnsFilter;
import common.RnsWorkload;
import common.RopScheduler;
import common.WorkMonitor;
import common.WorkloadReader;
import common.cniv.GenerateReport;
import common.cniv.ReportSender;
import common.metrics.Exporter;
import common.rmi.IJobController;
import common.rmi.IRemoteFilesJobsExecutor;
import common.rmi.RemoteFilesJobExecutor;
import common.rmi.WorkerRemoteFilesJobExecutor;
import common.cniv.ProgressReporterScheduler;
import org.json.JSONArray;

public class IoWriter {
    public static int blockSize = Integer.getInteger("blockSize", 8);
    public static Map<String, Integer> blockSizes;

    public static String rootDir;

    private enum Mode { COMBI, CTRL, WRITER };   // Change mode name from WORKER to WRITER.

    // private final static Logger LOGGER = Logger.getLogger(IoTest.class
    // .getName());

    public static void main(String args[]) {
        try {
            new IoWriter().run(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private void startControl(Mode mode, final String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: IoTest mode outputDir workLoadFile");
            return;
        }

        PathMapper.getInstance().setRootDirs(args[1]);

        File wlFile = new File(args[2]);
        if (!wlFile.canRead()) {
            System.out.println("Cannot read workload " + args[2]);
        }
        WorkloadReader wlr = new WorkloadReader(args[2]);
        Map<Integer, List<RnsWorkload>> workload = wlr.parseWorkload();
        Map<String, Integer> retention = wlr.parseRetention();
        blockSizes = wlr.parseWriteSize();


        filterWorkload(workload);

        IFilesJobsExecutor es = null;
        IFileSystemOpMonitor fsMonitor = null;
        if ( mode.equals(Mode.COMBI) ) {
            configBandwidthManger(workload);
            new Thread(BandwidthManager.getInstance(), "BandwidthManager").start();

            es = new LocalFilesJobExecutor();

            final IReadWriteMonitor rwMonitor = ReadWriteMonitorFactory.create();
            fsMonitor = FileSystemOpMonitorFactory.create();

            JobExecutorFactory.getInstance().registerJobExecutorFactory(
                    FilesJob.Type.WRITE, new WriteJobTypeExecutorFactory(rwMonitor, fsMonitor));
        } else if (mode.equals(Mode.CTRL)) {
            if ( args.length < 4 ) {
                System.out.println("Usage: IoTest mode outputDir workLoadFile numWorkers");
                return;
            }

            final RemoteFilesJobExecutor remoteCtrl = new RemoteFilesJobExecutor(
                    workload, blockSizes, Integer.parseInt(args[3]));

            final IJobController stub = (IJobController)UnicastRemoteObject.exportObject(remoteCtrl, 0);
            final Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            registry.bind("ctrl", stub);

            remoteCtrl.awaitWorkers();

            fsMonitor = FileSystemOpMonitorFactory.create();
            es = remoteCtrl;
        }

        FileDeleterScan deleterScan = null;
        Map<Integer, RopScheduler> collectors = null;
        if ( mode.equals(Mode.COMBI) || mode.equals(Mode.CTRL)) {
            collectors = new HashMap<Integer, RopScheduler>();
            for (Integer ropPeriod : workload.keySet()) {
                WriteScheduler ws = new WriteScheduler(es, workload.get(ropPeriod), ropPeriod.intValue() * 60);
                collectors.put(ropPeriod, ws);
                new Thread(ws).start();
            }

            int numDelThreads = Integer.getInteger("deleteThreads", 1);
            int deletePeriod = Integer.getInteger("deletePeriod", 3600 * 6);
            Log.log("Configuring File Delete with " + numDelThreads + " threads and period of " + deletePeriod + " secs");
            ThreadPoolExecutor deleteTPE = new ThreadPoolExecutor(numDelThreads, numDelThreads, 0, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
            deleterScan = new FileDeleterScan(retention, deletePeriod, deleteTPE, fsMonitor);
            WorkMonitor.getInstance().addTPE(deleteTPE);
            if (deletePeriod > 0) {
                new Thread(deleterScan).start();
            }

            FileDeleterScheduler deleteFLS = new FileDeleterScheduler(
                    retention, 900, deleteTPE, fsMonitor);
            new Thread(deleteFLS, "FileDeleterScheduler").start();
        }

        new Thread(WorkMonitor.getInstance()).start();

        Exporter.start();

        int testDurationInMin = Integer.getInteger("iotest.length", 0);
        if (testDurationInMin == 0) {
            LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
            String line;
            do {
                System.out.println("Enter Command>>");
                line = in.readLine();
                if (line.equals("d s")) {
                    deleterScan.setState(FileDeleterScan.State.Paused);
                } else if (line.equals("d r")) {
                    deleterScan.setState(FileDeleterScan.State.Running);
                } else if (line.startsWith("c ")) {
                    String fields[] = line.split(" ");
                    if (fields.length == 3) {
                        Integer ropPeriod = Integer.valueOf(fields[1]);
                        RopScheduler c = collectors.get(ropPeriod);
                        if (c != null) {
                            if (fields[2].equals("s"))
                                c.setState(RopScheduler.State.Paused);
                            else if (fields[2].equals("r"))
                                c.setState(RopScheduler.State.Running);
                        }
                    }
                }
            } while (!line.equals("exit"));
        } else if (testDurationInMin > 0) {
            System.in.close();
            long testDurationMs = testDurationInMin * 60000L;
            Log.log("IoTest: will shutdown @ " + new Date(System.currentTimeMillis() + testDurationMs));

            int reportingPeriodInMin = Integer.getInteger("reportingPeriodInMin", 60);
            long reportingPeriodMs = reportingPeriodInMin * 60000L;

            if (Boolean.getBoolean("cnivAgentEnabled")) {
                ProgressReporterScheduler reportSchedule = new ProgressReporterScheduler(workload, reportingPeriodMs, "ProgressReporter");
                new Thread(reportSchedule, "ProgressReporterScheduler").start();
            }

            Thread.sleep(testDurationMs);
            final JSONArray benchmarkResult = new JSONArray();
            for (Integer ropPeriod : workload.keySet()) {
                JSONArray benchmarkReport = GenerateReport.getInstance().generateRopReport(ropPeriod * 60,"ResultsReporter");
                benchmarkResult.putAll(benchmarkReport);
            }
            if (Boolean.getBoolean("cnivAgentEnabled"))
                ReportSender.getInstance().sendReport("ResultsReporter", benchmarkResult);

            Log.log("IoTest: Shutting down");
        } else {
            System.in.close();
            Log.log("IoTest: infinite wait");
            Object waitObject = new Object();
            synchronized (waitObject) {
                waitObject.wait();
            }
        }

        es.shutDown();
    }

    private void startWorker(final String args[]) throws Exception {
        IJobController ctrl = null;
        while (ctrl == null) {
            Log.log("Connecting to controller");
            try {
                final Registry registry = LocateRegistry.getRegistry(args[1]);
//                Log.log("Writer Registry print size "+ args.length);
//                Log.log("Writer Registry value "+ args[0]);
//                Log.log("Writer Registry value 1 "+ args[1]);
                ctrl = (IJobController)registry.lookup("ctrl");
            } catch (IOException e) {
                Thread.sleep(10000);
            }
        }

        final Map<Integer, List<RnsWorkload>> workload = ctrl.getWorkLoad();
        configBandwidthManger(workload);
        new Thread(BandwidthManager.getInstance(), "BandwidthManager").start();

        IoWriter.blockSizes = ctrl.getBlockSizes();

        final LocalFilesJobExecutor es = new LocalFilesJobExecutor();
        IFileSystemOpMonitor fsMonitor = FileSystemOpMonitorFactory.create();
        final IReadWriteMonitor rwMonitor = ReadWriteMonitorFactory.create();

        JobExecutorFactory.getInstance().registerJobExecutorFactory(
                FilesJob.Type.WRITE, new WriteJobTypeExecutorFactory(rwMonitor, fsMonitor));

        final WorkerRemoteFilesJobExecutor worker = new WorkerRemoteFilesJobExecutor(es);
        final IRemoteFilesJobsExecutor stub = (IRemoteFilesJobsExecutor)UnicastRemoteObject.exportObject(worker, 0);
        ctrl.registerWorker(stub);

        worker.awaitShutdown();
    }

    private void run(String args[]) throws Exception {

        Log.initTrace();

        Mode mode = null;
        try {
            mode = Mode.valueOf(args[0]);
        } catch (IllegalArgumentException iae) {
            System.out.println("Invalid valid for mode " + args[0] + ", valid values are COMBI, CTRL or WORKER");
        }

        if ( mode.equals(Mode.COMBI) || mode.equals(Mode.CTRL)) {
            startControl(mode, args);
        } else if ( mode.equals(Mode.WRITER) ) {
            startWorker(args);
        }
    }

    private BandwidthManager configBandwidthManger(Map<Integer, List<RnsWorkload>> workload) {

        BandwidthManager bwMgr = BandwidthManager.getInstance();

        for (Integer key : workload.keySet()) {
            List<RnsWorkload> ropWorkLoad = workload.get(key);
            for (RnsWorkload rns : ropWorkLoad) {
                if (rns.rncFileList != null) {
                    final String node = String.format("RNC%05d", rns.id);
                    bwMgr.addThrottle(node, rns.rncBandWidth);
                }

                for (int nodeBIndex = 1; nodeBIndex <= rns.numOfNodes; nodeBIndex++) {
                    final String node = String.format("RNS%05dNODEB%05d", rns.id, nodeBIndex);
                    bwMgr.addThrottle(node, rns.nodeBbandWidth);
                }
            }
        }

        return bwMgr;
    }

    private void filterWorkload(Map<Integer, List<RnsWorkload>> ropWorkloads) {
        for (List<RnsWorkload> workload : ropWorkloads.values()) {
            for (Iterator<RnsWorkload> iter = workload.iterator(); iter.hasNext();) {
                RnsWorkload rns = iter.next();
                if (RnsFilter.getInstance().check(rns.id) == false) {
                    Log.log("filterWorkload: Removing RNS " + rns.id);
                    iter.remove();
                }
            }
        }
    }
}

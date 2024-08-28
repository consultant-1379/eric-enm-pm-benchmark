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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.FileWorkLoad;
import common.FilesJob;
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
import common.cniv.ProgressReporterScheduler;
import org.json.JSONArray;

public class IoReader {
    private enum Mode { COMBI, CTRL, WRITER };

    public static void main(String args[]) {
        try {
            new IoReader().run(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private void run(String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: IoTest mode outputDir workLoadFile");
            return;
        }

        Mode mode = null;
        try {
            mode = Mode.valueOf(args[0]);
        } catch (IllegalArgumentException iae) {
            System.out.println("Invalid valid for mode " + args[0] + ", valid values are COMBI, CTRL or WRITER");
        }

        PathMapper.getInstance().setRootDirs(args[1]);

        Log.initTrace();

        File wlFile = new File(args[2]);
        if (!wlFile.canRead()) {
            System.out.println("Cannot read workload " + args[2]);
        }
        WorkloadReader wlr = new WorkloadReader(args[2]);
        Map<Integer, List<RnsWorkload>> workload = wlr.parseWorkload();
        filterWorkload(workload);

        IFilesJobsExecutor es = null;
        if ( mode.equals(Mode.COMBI) ) {
            es = new LocalFilesJobExecutor();
            final IReadWriteMonitor rwMonitor = ReadWriteMonitorFactory.create();
            JobExecutorFactory.getInstance().registerJobExecutorFactory(
                    FilesJob.Type.READ, new ReadJobTypeExecutorFactory(rwMonitor));
        }

        new Thread(WorkMonitor.getInstance()).start();
        Exporter.start();

        Map<Integer, RopScheduler> collectors = new HashMap<Integer, RopScheduler>();
        int offset = Integer.getInteger("offset", 0);
        for (Integer ropPeriod : workload.keySet()) {
            ReadScheduler s = new ReadScheduler(es, workload.get(ropPeriod),
                    (ropPeriod.intValue() * 60) + offset);
            collectors.put(ropPeriod, s);
            new Thread(s).start();
        }

        int testDurationInMin = Integer.getInteger("iotest.length", 1000000);

        int reportingPeriodInMin = Integer.getInteger("reportingPeriodInMin", 60);
        long reportingPeriodMs = reportingPeriodInMin * 60000L;

        if (Boolean.getBoolean("cnivAgentEnabled")) {
            ProgressReporterScheduler reportSchedule = new ProgressReporterScheduler(workload, reportingPeriodMs, "ProgressReporter");
            new Thread(reportSchedule, "ProgressReporterScheduler").start();
        }

        try {
            Thread.sleep(testDurationInMin * 60000L);
        } catch ( InterruptedException ignored ) {}
        final JSONArray benchmarkResult = new JSONArray();
        for (Integer ropPeriod : workload.keySet()) {
            JSONArray benchmarkReport = GenerateReport.getInstance().generateRopReport(ropPeriod * 60, "ResultsReporter");
            benchmarkResult.putAll(benchmarkReport);
        }
        if (Boolean.getBoolean("cnivAgentEnabled"))
            ReportSender.getInstance().sendReport("ResultsReporter", benchmarkResult);

        Log.log("IoTest: Shutting down");
        System.exit(0);
    }

    private void filterWorkload(Map<Integer, List<RnsWorkload>> ropWorkloads) {
        boolean processNodeB = Boolean.getBoolean("processNodeB");
        String fileTypesArr[] = System.getProperty("fileTypes", "XML,GPEH").split(",");
        String ropsArr[] = System.getProperty("rops", "1,15").split(",");

        Log.log("IoReader: processNodeB " + processNodeB + " fileTypes=" + Arrays.toString(fileTypesArr) +
                ", rops=" + Arrays.toString(ropsArr));

        HashSet<String> fileTypes = new HashSet<String>();
        for (String fileType : fileTypesArr) {
            fileTypes.add(fileType);
        }

        // Remove rop periods that aren't being used;
        HashSet<Integer> rops = new HashSet<Integer>();
        for (String rop : ropsArr)
            rops.add(Integer.valueOf(rop));
        for (Iterator<Integer> itr = ropWorkloads.keySet().iterator(); itr.hasNext();) {
            Integer wlROP = itr.next();
            if (!rops.contains(wlROP)) {
                Log.log("IoReader: removing ROP " + wlROP);
                itr.remove();
            }
        }

        for (List<RnsWorkload> workload : ropWorkloads.values()) {
            for (Iterator<RnsWorkload> iter = workload.iterator(); iter.hasNext();) {
                RnsWorkload rns = iter.next();
                if (RnsFilter.getInstance().check(rns.id) == false) {
                    Log.log("Removing RNS " + rns.id);
                    iter.remove();
                } else {
                    if (rns.rncFileList != null) {
                        filterFileList(rns.rncFileList, fileTypes);
                    }

                    if (rns.numOfNodes > 0 && rns.nodeBFileList != null) {
                        if (processNodeB) {
                            filterFileList(rns.nodeBFileList, fileTypes);
                        } else {
                            rns.numOfNodes = 0;
                            rns.nodeBFileList = null;
                        }
                    }
                }
            }
        }
    }

    private void filterFileList(List<FileWorkLoad> fileList, Set<String> fileTypes) {

        for (Iterator<FileWorkLoad> flr = fileList.iterator(); flr.hasNext();) {
            FileWorkLoad fwl = flr.next();
            if (fileTypes.contains(fwl.type) != true)
                flr.remove();
        }
    }
}

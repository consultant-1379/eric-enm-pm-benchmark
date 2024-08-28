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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import common.IFileSystemOpMonitor;
import common.IFileSystemOpMonitor.Operation;
import common.Log;
import common.PathMapper;

class FileDeleterScan implements Runnable {

    public enum DeleteMode {
        Time, Rops
    };

    static DeleteMode mode = DeleteMode.valueOf(System.getProperty("deleteMode", "Rops"));

    Map<String, Integer> config;
    int period;
    SimpleDateFormat df = new SimpleDateFormat("HHmmddMMyyyy"); //use time and date format

    ThreadPoolExecutor tpe;

    public enum State {
        Running, Paused, Stopped
    };

    State currentState = State.Running;

    IFileSystemOpMonitor fsMonitor;

    FileDeleterScan(Map<String, Integer> config, int period, ThreadPoolExecutor tpe, IFileSystemOpMonitor fsMonitor) {
        this.config = config;
        this.period = period;
        this.tpe = tpe;
        this.fsMonitor = fsMonitor;
    }

    public void setState(State newState) {
        currentState = newState;
    }

    public void run() {
        long now = System.currentTimeMillis();
        long currentMinute = now - (now % (60 * 1000));
        long nextMinute = currentMinute + (60 * 1000);
        long nextTime = nextMinute;

        final Set<String> typeSet = new HashSet<String>();
        for (final String configKey : config.keySet()) {
            typeSet.add(configKey.split(":")[0]);
        }
        final List<String> typeList = new ArrayList<>(typeSet.size());
        typeList.addAll(typeSet);

        try {
            while (!currentState.equals(State.Stopped)) {
                long delay = nextTime - System.currentTimeMillis();
                if (delay > 0) {
                    Thread.sleep(delay);

                    if (currentState.equals(State.Running)) {
                        Log.log("FileDeleterScan: Start");
                        int counts[] = { 0, 0 };
                        long cycleStart = System.currentTimeMillis();
                        for (final String type : typeList) {
                            processType(type, counts);
                        }
                        long cycleDuration = System.currentTimeMillis() - cycleStart;
                        Log.log("FileDeleterScan: End duration " + cycleDuration + " filesTotal " + counts[0]
                                + " filesDeleted " + counts[1]);
                    } else {
                        Log.log("FileDeleterScan: Skipping cycle state " + currentState);
                    }
                } else {
                    Log.log("FileDeleterScan: Skipping cycle delay " + delay);
                }

                nextTime += (period * 1000);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

    }

    private void processType(final String type, int[] counts) throws Exception {
        long t1 = System.currentTimeMillis();
        Map<String, List<File>> meDirs = new HashMap<>();
        fsMonitor.begin(Operation.ReadDir);
        for (final String rootDir : PathMapper.getInstance().getRootDirs()) {
            final File typeDir = new File(rootDir + "/" + type);
            if (typeDir.exists()) {
                for (final File dir : typeDir.listFiles()) {
                    final String me = dir.getName();
                    List<File> meDirList = meDirs.get(me);
                    if (meDirList == null) {
                        meDirList = new LinkedList<>();
                        meDirs.put(me, meDirList);
                    }
                    meDirList.add(dir);
                }
            }
        }
        fsMonitor.end(Operation.ReadDir);
        long t2 = System.currentTimeMillis();

        CountDownLatch cdl = new CountDownLatch(meDirs.size());
        // System.out.println("cdl.count=" + cdl.getCount());
        DeleteRun dr = new DeleteRun(config, type, cdl);
        for (final List<File> meDirList : meDirs.values()) {
            tpe.execute(new DirectoryProcessor(meDirList, dr));
        }
        cdl.await();

        long t3 = System.currentTimeMillis();

        long duration = t3 - t1;
        Log.log("FileDeleterScan: Completed " + type + " filesTotal " + dr.filesTotal + " filesDeleted "
                + dr.filesDeleted
                + " duration " + duration + " tree " + (t2 - t1) + " mescan " + dr.scanTime + " deltime "
                + dr.deleteTime);

        counts[0] += dr.filesTotal;
        counts[1] += dr.filesDeleted;
    }

    class DeleteRun {
        Map<String, Integer> config;
        private String type;
        CountDownLatch cdl;

        Map<String, Boolean> ropState = new HashMap<String, Boolean>();
        int filesDeleted = 0;
        int filesTotal = 0;
        long scanTime = 0;
        long deleteTime = 0;

        DeleteRun(Map<String, Integer> config, String type, CountDownLatch cdl) {
            this.config = config;
            this.type = type;
            this.cdl = cdl;

        }

        void dirCompleted(int filesTotal, int filesDeleted, long scanTime, long deleteTime) {
            this.filesTotal += filesTotal;
            this.filesDeleted += filesDeleted;
            this.scanTime += scanTime;
            this.deleteTime += deleteTime;

            // System.out.println("dirCompleted cdl.count=" + cdl.getCount());

            cdl.countDown();
        }
    }

    class DirectoryProcessor implements Runnable {
        final List<File> meDirList;
        final DeleteRun dr;

        DirectoryProcessor(final List<File> meDirList, final DeleteRun dr) {
            this.meDirList = meDirList;
            this.dr = dr;
        }

        private int deleteByTime(final List<File> files) throws Exception {
            int filesDeleted = 0;
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.startsWith("A")) {
                    String ropStart = fileName.substring(1, 13);
                    String ropPeriod = "15";
                    if (fileName.substring(13, 14).equals("_")) {
                        ropPeriod = fileName.substring(14, 16);
                    }
                    String ropTag = ropStart + ":" + ropPeriod;
                    Boolean deleteFile;
                    synchronized (dr.ropState) {
                        deleteFile = dr.ropState.get(ropTag);
                        if (deleteFile == null) {
                            Date ropDate = df.parse(ropStart);
                            long oldestROP = System.currentTimeMillis() -
                                    (dr.config.get(dr.type + ":" + ropPeriod) * 60 * 1000);
                            deleteFile = Boolean.valueOf(ropDate.getTime() < oldestROP);
                            // System.out.println(fileName + " " + dr.type + " " + new Date(oldestROP) + " "
                            // + deleteFile );
                            dr.ropState.put(ropTag, deleteFile);
                        }
                    }
                    if (deleteFile.booleanValue()) {
                        fsMonitor.begin(Operation.Delete);
                        file.delete();
                        fsMonitor.end(Operation.Delete);
                        filesDeleted++;
                    }

                }
            }
            return filesDeleted;
        }

        private int deleteByRop(final List<File> files) throws Exception {
            int filesDeleted = 0;

            final Map<String, Integer> ropLimits = new HashMap<>();
            final Map<String, Map<String, List<File>>> ropsByPeriod = new HashMap<>();

            for (File file : files) {
                String fileName = file.getName();
                if (fileName.startsWith("A")) {
                    final String rop = fileName.substring(0, fileName.lastIndexOf("."));
                    final String ropPeriod = fileName.substring(14, 16);

                    Map<String, List<File>> rops = ropsByPeriod.get(ropPeriod);
                    if (rops == null) {
                        rops = new HashMap<>();
                        ropsByPeriod.put(ropPeriod, rops);
                    }

                    List<File> filesInRop = rops.get(rop);
                    if (filesInRop == null) {
                        filesInRop = new LinkedList<File>();
                        rops.put(rop, filesInRop);
                    }
                    filesInRop.add(file);

                    Integer ropLimit = ropLimits.get(ropPeriod);
                    if (ropLimit == null) {
                        Integer cfgValue = dr.config.get(dr.type + ":" + ropPeriod);
                        if (cfgValue == null) {
                            throw new Exception("Could not get config value for type=" + dr.type + " ropPeriod="
                                    + ropPeriod + " config=" + dr.config + " file=" + file.getAbsolutePath());
                        }
                        ropLimit = Integer.valueOf(cfgValue.intValue() + (cfgValue.intValue() / 5));
                        ropLimits.put(ropPeriod, ropLimit);
                    }
                }
            }

            for (Map.Entry<String, Map<String, List<File>>> entry : ropsByPeriod.entrySet()) {
                final int ropsToKeep = ropLimits.get(entry.getKey());
                final Map<String, List<File>> rops = entry.getValue();

                if (rops.size() > ropsToKeep) {
                    List<String> sortedKeys = new ArrayList<String>(rops.keySet());
                    Collections.sort(sortedKeys);
                    for (int ropIndex = 0; ropIndex < sortedKeys.size() - ropsToKeep; ropIndex++) {
                        for (File file : rops.get(sortedKeys.get(ropIndex))) {
                            fsMonitor.begin(Operation.Delete);
                            file.delete();
                            fsMonitor.end(Operation.Delete);
                            filesDeleted++;
                        }

                    }
                }
            }

            return filesDeleted;
        }

        public void run() {
            try {
                long t1 = System.currentTimeMillis();
                fsMonitor.begin(Operation.ReadDir);
                List<File> files = new LinkedList<>();
                for (final File meDir : meDirList) {
                    File filesInDir[] = meDir.listFiles();
                    files.addAll(Arrays.asList(filesInDir));
                }
                fsMonitor.end(Operation.ReadDir);
                int filesTotal = files.size();
                long t2 = System.currentTimeMillis();
                int filesDeleted = 0;
                if (mode.equals(DeleteMode.Time))
                    filesDeleted = deleteByTime(files);
                else
                    filesDeleted = deleteByRop(files);
                long t3 = System.currentTimeMillis();
                dr.dirCompleted(filesTotal, filesDeleted, t2 - t1, t3 - t2);
            } catch (Throwable t) {
                System.out.println("Failed to process dir " + meDirList);
                t.printStackTrace();
                System.exit(1);
            }
        }
    }

}

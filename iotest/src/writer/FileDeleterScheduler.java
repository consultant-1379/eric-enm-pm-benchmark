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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.FileLookupService;
import common.IFileSystemOpMonitor;
import common.IFileSystemOpMonitor.Operation;
import common.Log;

public class FileDeleterScheduler implements Runnable {
    private final static Logger logger = Logger.getLogger(FileDeleterScheduler.class.getName());

    final Map<String, Integer> config;
    final int deletePeriod;
    final ThreadPoolExecutor tpe;
    final IFileSystemOpMonitor fsMonitor;

    final int batchSize = 20000;

    FileDeleterScheduler(final Map<String, Integer> config, final int deletePeriod, final ThreadPoolExecutor tpe,
                   final IFileSystemOpMonitor fsMonitor) {
        this.config = config;
        this.deletePeriod = deletePeriod;
        this.tpe = tpe;
        this.fsMonitor = fsMonitor;
    }

    @Override
    public void run() {
        int xmlCount = 0;

        while (true) {
            for (final Map.Entry<String, Integer> entry : config.entrySet()) {
                final String type = entry.getKey().split(":")[0];
                final int ropPeriod = Integer.valueOf(entry.getKey().split(":")[1]) * 60;
                final int retention = entry.getValue();

                boolean doRop = true;
                if (type.equals("XML") && ropPeriod > 60) {
                    xmlCount++;
                    if (xmlCount < 4) {
                        doRop = false;
                    } else {
                        xmlCount = 0;
                    }
                }

                if (doRop) {
                    try {
                        processRop(type, ropPeriod, retention);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(deletePeriod * 1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void processRop(final String type, final int ropPeriod, int retention) throws Exception {
        final List<String> rops = FileLookupService.getInstance().listRop(type, ropPeriod);
// To sort the rops list correctly by time and date
        Collections.sort(rops, new Comparator<String>() {
            SimpleDateFormat df = new SimpleDateFormat("HHmmddMMyyyy");
            @Override
            public int compare(String o1, String o2) {
                try {
                    return df.parse(o1).compareTo(df.parse(o2));
                } catch (ParseException e) {
                    Log.err("error: rops format is wrong");
                    throw new IllegalArgumentException(e);
                }
            }});
        logger.log(Level.FINER, "processRop: rops={0}", new Object[] { rops });

        final int numRopsToDelete = rops.size() - retention;
        if (numRopsToDelete > 0) {
            Log.log("FileDeleterScheduler: " + type + ":" + ropPeriod + " numRopsToDelete=" + numRopsToDelete);

            final String[] ropsToDelete = new String[numRopsToDelete];
            Iterator<String> ropItr = rops.iterator();
            for (int i = 0; i < numRopsToDelete; i++) {
                ropsToDelete[i] = ropItr.next();
            }
            processRopsToDelete(type, ropPeriod, ropsToDelete);
        }
    }

    private void processRopsToDelete(final String type, final int ropPeriod, final String[] ropsToDelete)
            throws Exception {
        logger.log(
                Level.FINE, "processRopsToDelete: type={0} ropPeriod={1} ropsToDelete={2}",
                new Object[] { type, Integer.valueOf(ropPeriod), ropsToDelete }
        );

        final long startTime = System.currentTimeMillis();
        final List<List<String>> batches = new LinkedList<>();
        List<String> currentBatch = null;
        int totalFiles = 0;
        for (final String ropToDelete : ropsToDelete) {
            final List<String> filesInRop = FileLookupService.getInstance().readRop(type, ropPeriod, ropToDelete);
            totalFiles += filesInRop.size();
            for (final String file : filesInRop) {
                if (currentBatch == null || currentBatch.size() >= batchSize) {
                    currentBatch = new ArrayList<>(batchSize);
                    batches.add(currentBatch);
                }
                currentBatch.add(file);
            }
        }

        final CountDownLatch tasksInBatchRunning = new CountDownLatch(batches.size());
        for (final List<String> batch : batches) {
            final Runnable task = new Runnable() {
                public void run() {
                    for (final String file : batch) {
                        try {
                            fsMonitor.begin(Operation.Delete);
                            Files.delete(Paths.get(file));
                            fsMonitor.end(Operation.Delete);
                        } catch (IOException e) {
                            Log.log("Failed to delete " + file);
                        }
                    }
                    tasksInBatchRunning.countDown();
                }
            };
            tpe.submit(task);
        }

        final int totalFilesFinal = totalFiles;
        final Runnable updateFLS = new Runnable() {
            public void run() {
                try {
                    tasksInBatchRunning.await();
                } catch (InterruptedException ignored) {
                }

                long endTime = System.currentTimeMillis();

                for (final String rop : ropsToDelete) {
                    try {
                        FileLookupService.getInstance().removeRop(type, ropPeriod, rop);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                long duration = endTime - startTime;
                Log.log("FileDeleterScheduler: " + type + ":" + ropPeriod + " Deleted " + totalFilesFinal + " files in "
                        + duration + "msec");
            }
        };
        tpe.submit(updateFLS);
    }

}
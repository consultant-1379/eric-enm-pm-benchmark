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

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FileSystemOpMonitor implements IFileSystemOpMonitor, Runnable {
    class Counts {
        public AtomicInteger active = new AtomicInteger();
        public AtomicInteger peak = new AtomicInteger();
        public AtomicInteger count = new AtomicInteger();
    }

    Map<Operation, Counts> countsPerOp = new EnumMap<Operation, Counts>(Operation.class);
    AtomicInteger totalPeak = new AtomicInteger();
    private final long interval;

    public FileSystemOpMonitor(long interval) {
        this.interval = interval;

        for (Operation op : Operation.values()) {
            countsPerOp.put(op, new Counts());
        }
    }

    public void begin(Operation op) {
        Counts counts = countsPerOp.get(op);
        int current = counts.active.incrementAndGet();

        int peak = counts.peak.get();
        if (current > peak) {
            counts.peak.compareAndSet(peak, current);
        }

        counts.count.incrementAndGet();

        int total = 0;
        for (Counts opCount : countsPerOp.values())
            total += opCount.active.get();
        int tPeak = totalPeak.get();
        if (total > tPeak)
            totalPeak.compareAndSet(tPeak, total);
    }

    public void end(Operation op) {
        Counts counts = countsPerOp.get(op);
        counts.active.decrementAndGet();
    }

    public void run() {
        StringBuffer header = new StringBuffer("FSO_Monitor:");
        for (Operation op : Operation.values()) {
            header.append(" " + op.toString());
        }
        Log.log(header.toString());

        while (true) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {
            }
            StringBuffer sb = new StringBuffer();
            int totalCount = 0;
            for (Operation op : Operation.values()) {
                Counts counts = countsPerOp.get(op);
                int active = counts.active.get();
                int peak = counts.peak.getAndSet(0);
                int count = counts.count.getAndSet(0);
                totalCount += count;
                sb.append(" [ " + active + " " + peak + " " + count + " ]");
            }
            int overallPeak = totalPeak.getAndSet(0);
            if (totalCount > 0) {
                sb.append(" [ " + overallPeak + " " + totalCount + " ]");
                Log.log("FSO_Monitor: " + sb.toString());
            }
        }
    }

}

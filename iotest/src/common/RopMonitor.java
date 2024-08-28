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

import common.metrics.RopMetrics;

public class RopMonitor implements IRopMonitor {
    final String desc;
    long start = 0;
    int files = 0;
    int kb = 0;
    long openTime = 0;
    long closeTime = 0;
    long minOpenTime = Long.MAX_VALUE;
    long maxOpenTime = 0;
    long minCloseTime = Long.MAX_VALUE;
    long maxCloseTime = 0;
    long ioTime = 0;
    long minIOTime = Long.MAX_VALUE;
    long maxIOTime = 0;

    int jobsRemaining = 0;

    public RopMonitor(String desc) {
        this.desc = desc;
    }

    public void setNumJobs(int numJobs) {
        jobsRemaining = numJobs;
    }

    public int getJobsRemaining() {
        return jobsRemaining;
    }

    public int getFiles() {
        return files;
    }

    public int getKB() {
        return kb;
    }

    public long getOpenTime() {
        return openTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public long getIoTime() { return ioTime; }

    public String getDescription() {
        return desc;
    }

    public synchronized void jobStarted() {
        if (start == 0)
            start = System.currentTimeMillis();
    }

    public synchronized void jobCompleted(String nodeName, int files, int kb, long openTime, long closeTime, long jobTime) {
        this.jobsCompleted(1, files, kb, openTime, closeTime, jobTime);
    }

    public synchronized void jobsCompleted(int jobs, int files, int kb, long openTime, long closeTime, long ioTime) {
        this.files += files;
        this.kb += kb;
        this.openTime += openTime;
        this.closeTime += closeTime;
        this.ioTime += ioTime;

        jobsRemaining-=jobs;

        if (jobsRemaining <= 0) {
            this.notifyAll();
            long now = System.currentTimeMillis();

            final long duration = now - start;

            float durationInSec = (float)duration/1000L;

            final String[] descParts = desc.split(":");
            RopMetrics.getInstance().reportRop(
                    Integer.parseInt(descParts[1]),
                    duration
            );

            String msg = "RopMonitor " + desc + " ";

            Log.log(msg + "Total duration = " + durationInSec + " sec, no. of files = " + this.files + ", size = "
                    + (this.kb / 1024) + " mb, openTime " + (this.openTime / 1000000) + " closeTime " + (this. closeTime / 1000000) + ", speed = " + ((this.kb/1024)/(durationInSec ))+ " mbps");

            Log.log(msg + "avgOpenTime = " + ((float)this.openTime/this.files/1000000) + "ms, maxOpenTime = " + ((float)this.maxOpenTime/1000000)
                    + "ms, minOpenTime = " + ((float)this.minOpenTime/1000000) + "ms");

            Log.log(msg + "avgCloseTime = " + ((float)this.closeTime/this.files/1000000) + "ms, maxCloseTime = " + ((float)this.maxCloseTime/1000000)
                    + "ms, minCloseTime = " + ((float)this.minCloseTime/1000000) + "ms");

            Log.log(msg + "avgR/Wtime = " + ((float)this.ioTime/this.files/1000000) + "ms, maxR/Wtime = " + ((float)this.maxIOTime/1000000)
                    + "ms, minR/Wtime = " + ((float)this.minIOTime/1000000) + "ms");
        }
    }

    public synchronized void setOutliers(long maxOpenTime, long minOpenTime, long maxCloseTime, long minCloseTime, long maxIOtime, long minIOtime) {
        if (this.maxOpenTime < maxOpenTime) {
            this.maxOpenTime = maxOpenTime;
        }
        if (this.minOpenTime > minOpenTime) {
            this.minOpenTime = minOpenTime;
        }
        if (this.maxCloseTime < maxCloseTime) {
            this.maxCloseTime = maxCloseTime;
        }
        if (this.minCloseTime > minCloseTime) {
            this.minCloseTime = minCloseTime;
        }
        if (this.maxIOTime < maxIOtime) {
            this.maxIOTime = maxIOtime;
        }
        if (this.minIOTime > minIOtime) {
            this.minIOTime = minIOtime;
        }
    }

    public synchronized void awaitCompletion() {
        while (jobsRemaining > 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}

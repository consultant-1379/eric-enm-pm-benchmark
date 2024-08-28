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

import java.io.FileInputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.FileInfo;
import common.IReadWriteMonitor;
import common.IRopMonitor;

class FilesReader implements Runnable,Comparable<FilesReader> {
    final String nodeName;
    final List<FileInfo> files;
    IRopMonitor ropMonitor;
    IReadWriteMonitor rwMonitor;
    Integer priority;

    private final static int readSize = Integer.getInteger("readsize", 8);
    private final static Logger LOGGER = Logger.getLogger(FilesReader.class.getName());

    FilesReader(String nodeName, List<FileInfo> files, IRopMonitor ropMonitor, IReadWriteMonitor rwMonitor,Integer priority) {
        this.nodeName = nodeName;
        this.files = files;
        this.ropMonitor = ropMonitor;
        this.rwMonitor = rwMonitor;
        this.priority = priority;
    }
    @Override
    public int compareTo(FilesReader o) {
        return this.priority.compareTo(o.priority);
    }
    public void run() {
        long startTime = System.currentTimeMillis();

        ropMonitor.jobStarted();

        byte[] block = null;
        long totalOpenTime = 0;
        long totalCloseTime = 0;
        long totalReadTime = 0;
        int totalFiles = 0;
        int totalKB = 0;

        long maxOpenTime = 0;
        long minOpenTime = Long.MAX_VALUE;
        long maxCloseTime = 0;
        long minCloseTime = Long.MAX_VALUE;
        long maxReadTime = 0;
        long minReadTime = Long.MAX_VALUE;

        try {
            for (FileInfo fi : files) {
                if (LOGGER.isLoggable(Level.FINER))
                    LOGGER.finer("Processing " + fi.filePath);
                int ioSize = getIoSize(fi) * 1024;
                if (block == null || block.length != ioSize) {
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.finest("Allocating block size " + ioSize);
                    block = new byte[ioSize];
                }

                long t1 = System.nanoTime();
                FileInputStream in = new FileInputStream(fi.filePath);
                long t2 = System.nanoTime();
                long openTime = t2 - t1;
                totalOpenTime += openTime;
                if (maxOpenTime < openTime)
                    maxOpenTime = openTime;
                if (minOpenTime > openTime)
                    minOpenTime = openTime;

                int totalBytesToRead = in.available();
                int totalBytesRead = 0;
                long readTime = 0;
                while (totalBytesRead < totalBytesToRead) {
                    int bytesToRead = ioSize;
                    if ((totalBytesToRead - totalBytesRead) < ioSize)
                        bytesToRead = totalBytesToRead - totalBytesRead;

                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.finest("Reading " + bytesToRead);
                    long t5 = System.nanoTime();
                    in.read(block, 0, bytesToRead);
                    long t6 = System.nanoTime();
                    readTime += t6 - t5;
                    rwMonitor.read(bytesToRead);
                    totalBytesRead += bytesToRead;
                }
                totalReadTime += readTime;

                if (maxReadTime < readTime)
                    maxReadTime = readTime;
                if (minReadTime > readTime)
                    minReadTime = readTime;

                long t3 = System.nanoTime();
                in.close();
                long t4 = System.nanoTime();
                long closeTime = t4 - t3;
                totalCloseTime += closeTime;
                if (maxCloseTime < closeTime)
                    maxCloseTime = closeTime;
                if (minCloseTime > closeTime)
                    minCloseTime = closeTime;

                totalKB += totalBytesRead / 1024;
                totalFiles++;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        if (totalFiles > 1 && LOGGER.isLoggable(Level.FINE)) {
            int durationMS = (int) (endTime - startTime);
            int ratebit_sec = (totalKB * 1024 * 8 * 1000) / durationMS;
            LOGGER.fine("run Done " + nodeName + " MB " + (totalKB / 1024) + " kbit/s" + (ratebit_sec / 1000));
        }

        ropMonitor.jobCompleted(nodeName, totalFiles, totalKB, totalOpenTime, totalCloseTime, totalReadTime);
        ropMonitor.setOutliers(maxOpenTime, minOpenTime, maxCloseTime, minCloseTime, maxReadTime, minReadTime);
    }

    private int getIoSize(FileInfo fi) {
        return FilesReader.readSize;
    }
}
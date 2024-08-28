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
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.FileInfo;
import common.IFileSystemOpMonitor;
import common.IFileSystemOpMonitor.Operation;
import common.IReadWriteMonitor;
import common.IRopMonitor;
import common.Log;

class FilesWriter implements Runnable,Comparable<FilesWriter> {
    final String nodeName;
    final IBandwidthThrottle bwt;
    final List<FileInfo> files;
    IRopMonitor ropMonitor;
    IFileSystemOpMonitor fsMonitor;
    IReadWriteMonitor rwMonitor;
    Integer priority;

    private static final Logger LOGGER = Logger.getLogger(FilesWriter.class.getName());

    FilesWriter(String nodeName, IBandwidthThrottle bwt, List<FileInfo> files, IRopMonitor ropMonitor,
                IFileSystemOpMonitor fsMonitor, IReadWriteMonitor rwMonitor,Integer priority) {
        this.nodeName = nodeName;
        this.bwt = bwt;
        this.files = files;
        this.ropMonitor = ropMonitor;
        this.fsMonitor = fsMonitor;
        this.rwMonitor = rwMonitor;
        this.priority = priority;
    }
    @Override
    public int compareTo(FilesWriter o) {
        return this.priority.compareTo(o.priority);
    }
    public void run() {
        long startTime = System.currentTimeMillis();

        ropMonitor.jobStarted();
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.finest("Processing " +ropMonitor.getDescription());
        byte[] block = null;
        long totalOpenTime = 0;
        long totalCloseTime = 0;
        long totalWriteTime = 0;
        int totalFiles = 0;
        int totalKB = 0;

        long maxOpenTime = 0;
        long minOpenTime = Long.MAX_VALUE;
        long maxCloseTime = 0;
        long minCloseTime = Long.MAX_VALUE;
        long maxWriteTime = 0;
        long minWriteTime = Long.MAX_VALUE;

        // Directories containing the files, used to see if we've already
        // checked if the directory exists
        final Set<String> dirs = new HashSet<>();

        try {
            for (FileInfo ftw : files) {
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.finest("Processing " + ftw.filePath);

                int writeSize = getWriteSize(ftw) * 1024;
                if (block == null || block.length != writeSize) {
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.finest("Allocating block size " + writeSize);
                    block = new byte[writeSize];
                }

                // Check if the directory containing the file exists
                final File file = new File(ftw.filePath);
                final String directory = file.getParent();
                if ( ! dirs.contains(directory) ) {
                    final File dirObj = new File(directory);
                    if (!dirObj.exists()) {
                        if (!dirObj.mkdirs()) {
                            throw new Exception("Failed to create " + directory);
                        }
                    }
                    dirs.add(directory);
                }

                fsMonitor.begin(Operation.Open);
                long t1 = System.nanoTime();
                // Instead or writing the file we put this info in a message bus
                // RateLimit rl =(RateLimit) bwt;
                // int interval=rl.amountPerInterval;
                // add to queue filepath, filesize, ratelimit, blocksize
                FileOutputStream out = new FileOutputStream(ftw.filePath);
                long t2 = System.nanoTime();
                fsMonitor.end(Operation.Open);
                long openTime = t2 - t1;
                totalOpenTime += openTime;
                if (maxOpenTime < openTime)
                    maxOpenTime = openTime;
                if (minOpenTime > openTime)
                    minOpenTime = openTime;

                int totalBytesToWrite = ftw.size * 1024;
                int totalBytesWritten = 0;
                long writeTime = 0;
                while (totalBytesWritten < totalBytesToWrite) {
                    int bytesToWrite = writeSize;
                    if ((totalBytesToWrite - totalBytesWritten) < writeSize)
                        bytesToWrite = totalBytesToWrite - totalBytesWritten;
                    bwt.get(bytesToWrite);

                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.finest("Writting " + bytesToWrite);
                    fsMonitor.begin(Operation.Write);
                    long t5 = System.nanoTime();
                    out.write(block, 0, bytesToWrite);
                    long t6 = System.nanoTime();
                    fsMonitor.end(Operation.Write);
                    writeTime += t6 - t5;
                    rwMonitor.write(bytesToWrite);
                    totalBytesWritten += bytesToWrite;
                }
                totalWriteTime += writeTime;
                if (maxWriteTime < writeTime)
                    maxWriteTime = writeTime;
                if (minWriteTime > writeTime)
                    minWriteTime = writeTime;

                fsMonitor.begin(Operation.Close);
                long t3 = System.nanoTime();
                out.close();
                long t4 = System.nanoTime();
                fsMonitor.end(Operation.Close);
                long closeTime = t4 - t3;
                totalCloseTime += closeTime;
                if (maxCloseTime < closeTime)
                    maxCloseTime = closeTime;
                if (minCloseTime > closeTime)
                    minCloseTime = closeTime;

                totalKB += totalBytesWritten / 1024;
                totalFiles++;
            }
        } catch (Throwable t) {
            File file = new File (files.get(0).filePath);
            Log.err("StorageError: " + t.getMessage() + " Folder: " + file.getParentFile());

            System.exit(1);
        }

        long endTime = System.currentTimeMillis();
        if (totalFiles > 1 && LOGGER.isLoggable(Level.FINE)) {
            int durationSec = (int) ((endTime - startTime) / 1000);
            int ratebit_sec = (totalKB * 1024 * 8) / durationSec;
            LOGGER.fine("run Done " + nodeName + " MB " + (totalKB / 1024) + " kbit/s " + (ratebit_sec / 1000));
        }

        ropMonitor.jobCompleted(nodeName, totalFiles, totalKB, totalOpenTime, totalCloseTime, totalWriteTime);
        ropMonitor.setOutliers(maxOpenTime, minOpenTime, maxCloseTime, minCloseTime, maxWriteTime, minWriteTime);
    }

    private int getWriteSize(FileInfo ftw) {
        Integer writeSize = IoWriter.blockSizes.get(ftw.type);
        if (writeSize != null)
            return writeSize.intValue();
        else
            return IoWriter.blockSize;
    }
}
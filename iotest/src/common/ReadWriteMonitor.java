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

import java.util.concurrent.atomic.AtomicLong;

public class ReadWriteMonitor implements IReadWriteMonitor, Runnable {
    private final long period;
    private AtomicLong readBytes = new AtomicLong();
    private AtomicLong writeBytes = new AtomicLong();

    public ReadWriteMonitor(long period) {
        this.period = period;
    }

    public void run() {
        try {
            long now = System.currentTimeMillis();
            long currentMinute = now - (now % (60 * 1000));
            long nextMinute = currentMinute + (60 * 1000);
            Thread.sleep(nextMinute - now);

            while (true) {
                Thread.sleep(period);
                long read = readBytes.getAndSet(0);
                long write = writeBytes.getAndSet(0);
                if (read > 0 || write > 0) {
                    Log.log(String.format("RWMon: r %5d %5d w %5d %5d",
                            (read / (1024 * 1024)), ((read * 8) / (period * 1000)),
                            (write / (1024 * 1024)), ((write * 8) / (period * 1000))));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void read(int bytes) {
        readBytes.addAndGet(bytes);
    }

    public void write(int bytes) {
        writeBytes.addAndGet(bytes);
    }

}

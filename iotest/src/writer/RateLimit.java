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

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateLimit implements IBandwidthThrottle {
    private Semaphore remaining;
    public final int amountPerInterval;
    private final static Logger LOGGER = Logger.getLogger(RateLimit.class.getName());
    private final String key;

    public RateLimit(String key, int amountPerInterval) {
        this.key = key;
        this.amountPerInterval = amountPerInterval;
        remaining = new Semaphore(this.amountPerInterval, true);
    }

    public synchronized void get(int amount) {
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.finest("get: key=" + key + ", amount=" + amount + ", remaining=" + remaining.availablePermits());
        try {
            remaining.acquire(amount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.finest("get: key=" + key + ", returning");
    }

    public void refresh() {
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.finest("refresh: key=" + key + ", waiting=" + remaining.hasQueuedThreads() + ", remaining="
                    + remaining.availablePermits());

        if (remaining.hasQueuedThreads()) {
            remaining.release(amountPerInterval);
        } else {
            remaining.drainPermits();
            remaining.release(amountPerInterval);
        }
    }
}

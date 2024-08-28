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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BandwidthManager implements Runnable {
    private static BandwidthManager instance;

    private long interval = 1000L;
    private List<RateLimit> limits = new ArrayList<RateLimit>();
    private Map<String, IBandwidthThrottle> map = new HashMap<String, IBandwidthThrottle>();
    private boolean unlimitedBw = Boolean.getBoolean("unlimitedbw");

    public static synchronized BandwidthManager getInstance() {
        if ( instance == null ) {
            instance = new BandwidthManager();
        }
        return instance;
    }
    public IBandwidthThrottle addThrottle(String key, int rate_kbits) {
        int bytesPerInterval = (int) ((rate_kbits * 1000 / 8) / (interval / 1000));
        IBandwidthThrottle ibt = map.get(key);
        if (ibt == null) {
            if (bytesPerInterval > 0 && !unlimitedBw) {
                RateLimit rl = new RateLimit(key, bytesPerInterval);
                limits.add(rl);
                ibt = rl;
            } else {
                ibt = new UnlimittedRate();
            }
            map.put(key, ibt);
        } else {
            if (ibt instanceof RateLimit) {
                RateLimit rl = (RateLimit) ibt;
                if (rl.amountPerInterval != bytesPerInterval) {
                    throw new RuntimeException("different bandwidth for " + key);
                }
            } else if (bytesPerInterval != 0 && !unlimitedBw)
                throw new RuntimeException("different bandwidth for " + key);
        }

        return ibt;
    }

    public IBandwidthThrottle getThrottle(String key) {
        return map.get(key);
    }

    public void run() {
        while (true) {
            for (RateLimit rl : limits) {
                rl.refresh();
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private BandwidthManager(){
    }
}

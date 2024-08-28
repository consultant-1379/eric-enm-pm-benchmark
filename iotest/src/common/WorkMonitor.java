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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class WorkMonitor implements Runnable {
    private static WorkMonitor instance;

    final List<ThreadPoolExecutor> es = new LinkedList<>();

    public static synchronized WorkMonitor getInstance() {
        if ( instance == null ) {
            instance = new WorkMonitor();
        }
        return instance;
    }

    public void addTPE(final ThreadPoolExecutor tpe) {
        es.add(tpe);
    }

    public void run() {
        boolean keepGoing = true;
        long completed[] = new long[es.size()];

        try {
            while (keepGoing) {
                String msg = "WorkMonitor:";
                int total = 0;
                int i = 0;
                for (final ThreadPoolExecutor tpe : es) {
                    long currentCompl = tpe.getCompletedTaskCount();
                    int done = (int) (currentCompl - completed[i]);
                    int active = tpe.getActiveCount();
                    int qsize = tpe.getQueue().size();
                    msg = "WorkMonitor: No. of jobs done=" + done + ", No. of active jobs=" + active + ", No. of jobs in queue=" + qsize;
                    completed[i] = currentCompl;

                    total += done + active + qsize;
                    i++;
                }
                if (total > 0)
                    Log.log(msg);

                Thread.sleep(5 * 1000);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private WorkMonitor() {
    }
}
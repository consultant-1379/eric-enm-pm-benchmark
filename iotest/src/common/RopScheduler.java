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

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;

public abstract class RopScheduler implements Runnable {
    private final static Logger schlogger = Logger.getLogger(RopScheduler.class.getName());


    final IFilesJobsExecutor es;
    final protected List<RnsWorkload> workload;
    final protected int period;
    final int offset;

    public enum NodeState {
        Active, Idle
    };

    public final String periodTag;

    public enum State {
        Running, Paused, Stopped
    };

    State currentState = State.Running;

    protected RopScheduler(IFilesJobsExecutor es, List<RnsWorkload> workload, int peroid, int offset) {
        this.es = es;
        this.workload = workload;
        this.period = peroid;
        this.offset = offset;
        this.periodTag = String.format("%02d", (this.period / 60));
    }

    public void setState(State newState) {
        currentState = newState;
    }

    private long doRop(long nextROP, SimpleDateFormat df, RopExecutor ropExec) throws Exception {
        final String rop = df.format(nextROP + (offset * period * 1000));
        long now = System.currentTimeMillis();
        long delay = nextROP - now;
        nextROP += (period * 1000);

        if (delay <= 0) {
            Log.log("RopScheduler: Skipping " + rop + ":" + period + " delay " + delay);
            return nextROP;
        }

        schlogger.log(Level.FINER, "processRop: periodTag={0} now={1} delay={2}", new Object[] { this.periodTag, Long.valueOf(now), Long.valueOf(delay) });
        //Log.log("processRop: periodTag="+this.periodTag+" now="+Long.valueOf(now)+" delay="+Long.valueOf(delay));
        Thread.sleep(delay);
        long wakeTime = System.currentTimeMillis();
        long sleptTime = wakeTime - now;
        schlogger.log(Level.FINER, "processRop: periodTag={0} wakeTime={1} sleptTime={2}", new Object[] { this.periodTag, Long.valueOf(wakeTime), Long.valueOf(sleptTime) });
        //Log.log("processRop: periodTag="+this.periodTag+" wakeTime="+Long.valueOf(wakeTime)+" sleptTime="+Long.valueOf(sleptTime));
        if (!currentState.equals(State.Running)) {
            return nextROP;
        }

        final List<FilesJob> jobs = generateJobs("A" + rop);
        Log.log("RopScheduler: Total number of FilesJob = " + jobs.size());
        schlogger.log(Level.FINER, "processRop: periodTag={0} jobs.size={1}", new Object[] { this.periodTag, jobs.size() });
        //Log.log("processRop: periodTag="+this.periodTag+", jobs.size="+jobs.size());
        if (jobs.size() > 0) {
            final RopMonitor ropMonitor = new RopMonitor(rop + ":" + period);
            ropMonitor.setNumJobs(jobs.size());
            schlogger.log(Level.FINER, "processRop: periodTag={0} calling preExec", new Object[] { this.periodTag });
            //Log.log("processRop: periodTag="+this.periodTag+" calling preExec");
            preExec(rop, ropMonitor, jobs);
            ropExec.execute(ropMonitor, jobs);
        }

        return nextROP;
    }

    public void run() {
        RopExecutor ropExec = new RopExecutor(es);
        new Thread(ropExec).start();

        SimpleDateFormat df = new SimpleDateFormat("HHmmddMMyyyy"); //use time and date format

        long nextROP = 0L;
        long now = System.currentTimeMillis();
        if (this.period == 60) {
            long currentMinute = now - (now % (60 * 1000));
            long nextMinute = currentMinute + (60 * 1000);
            nextROP = nextMinute;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.set(Calendar.MINUTE, 5);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            while (cal.getTimeInMillis() <= now) {
                cal.setTimeInMillis(cal.getTimeInMillis() + (this.period * 1000));
            }
            nextROP = cal.getTimeInMillis();
        }
        Log.log("RopScheduler: First ROP " + df.format(nextROP) + ":" + period);

        try {
            while (!currentState.equals(State.Stopped)) {
                nextROP = doRop(nextROP, df, ropExec);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public void preExec(final String rop, final RopMonitor ropMonitor, final List<FilesJob> jobs) {
    }

    public abstract FilesJob generateJob(String rootDir, String node, String nodeDir, String rop,
                                         List<FileWorkLoad> fileWorkLoadList) throws Exception;

    protected List<FilesJob> generateJobs(String rop) throws Exception {
        final List<FilesJob> jobs = new LinkedList<>();
        final PathMapper pathMapper = PathMapper.getInstance();
        // Schedule RNCs first
        final String dirNameFormat = "SubNetwork=AAAAA,SubNetwork=BBBBB,SubNetwork=RNS%05d,MeContext=%s,ManagedElement=%s";

        for (RnsWorkload rns : workload) {
            if (rns.rncFileList != null && rns.rncFileList.size() > 0) {
                final String node = String.format("RNC%05d", rns.id);
                final String nodeDir = String.format(dirNameFormat, rns.id, node, node);
                jobs.add(generateJob(pathMapper.getNextRootDir(), node, nodeDir, rop, rns.rncFileList));
            }
        }

        // Now do the nodeBs
        for (RnsWorkload rns : workload) {
            if (rns.nodeBFileList != null && rns.nodeBFileList.size() > 0) {
                for (int nodeBIndex = 1; nodeBIndex <= rns.numOfNodes; nodeBIndex++) {
                    final String node = String.format("RNS%05dNODEB%05d", rns.id, nodeBIndex);
                    final String nodeDir = String.format(dirNameFormat, rns.id, node, node);
                    jobs.add(generateJob(pathMapper.getNextRootDir(), node, nodeDir, rop, rns.nodeBFileList));
                }
            }
        }

        return jobs;
    }
}

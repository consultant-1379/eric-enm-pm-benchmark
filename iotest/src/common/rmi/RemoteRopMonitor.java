package common.rmi;

import java.rmi.RemoteException;

import common.IRopMonitor;

public class RemoteRopMonitor implements IRemoteRopMonitor {
    final IRopMonitor ropMonitor;

    public RemoteRopMonitor(final IRopMonitor ropMonitor) {
        this.ropMonitor = ropMonitor;
    }

    public void jobsCompleted(int jobs, int files, int kb, long openTime, long closeTime, long jobTime) throws RemoteException {
        this.ropMonitor.jobsCompleted(jobs, files, kb, openTime, closeTime, jobTime);
    }
}

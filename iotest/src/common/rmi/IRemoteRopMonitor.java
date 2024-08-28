package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemoteRopMonitor extends Remote {
    void jobsCompleted(int jobs, int files, int kb, long openTime, long closeTime, long jobTime) throws RemoteException;
}

package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import common.FilesJob;

public interface IRemoteFilesJobsExecutor extends Remote {
    public void executeFileJobs(final List<FilesJob> jobs, final IRemoteRopMonitor ropMonitor, final String desc) throws RemoteException;

    public void shutdown() throws RemoteException;
}

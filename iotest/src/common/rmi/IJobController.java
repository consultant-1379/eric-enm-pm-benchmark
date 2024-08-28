package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import common.RnsWorkload;

public interface IJobController extends Remote {
    void registerWorker(IRemoteFilesJobsExecutor worker) throws RemoteException;

    Map<Integer, List<RnsWorkload>> getWorkLoad() throws RemoteException;

    Map<String, Integer> getBlockSizes() throws RemoteException;
}

package ngat.rcs.tms;

import java.rmi.*;

public interface TaskSequenceListener extends Remote {

    public void taskInitialized(Task task) throws RemoteException;

    public void taskStarted(Task task) throws RemoteException;

    public void taskCompleted(Task task) throws RemoteException;

    public void taskFailed(Task task, String reason) throws RemoteException;

    public void taskAborted(Task task, String reason) throws RemoteException;

}
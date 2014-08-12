package ngat.rcs.ops;

import java.rmi.*;

public interface OperationsEventListener extends Remote {

    //public void modeChanged(String oldMode, String newMode) throws RemoteException;

    public void operationsEventNotification(OperationsEvent oe) throws RemoteException;
    
}
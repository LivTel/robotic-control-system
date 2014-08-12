package ngat.rcs.ops;

import java.rmi.*;

/** Operations monitor.*/
public interface OperationsMonitor extends Remote {

    /**
     *  Registers an instance of OperationsEventListenerfor notification of any OperationsEvents.
     *  If the listener is already registered this method should return silently.
     * @param l An instance of OperationsEventListener.
     * @throws RemoteException
     */
    public void addOperationsEventListener(OperationsEventListener l)  throws RemoteException;

    /**
     * Remove the specified listener from the list of registered statusListeners. If the listener is not registered this
     * method should return silently.
     * @param l An instance of OperationsEventListener.
     * @throws RemoteException
     */
    public void removeOperationsEventListener(OperationsEventListener l)  throws RemoteException;

}
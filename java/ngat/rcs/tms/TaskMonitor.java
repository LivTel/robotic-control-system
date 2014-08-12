/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface TaskMonitor extends Remote {
	
	/**
     *  Registers an instance of TaskLifecycleListener for notification of any TaskEvents.
     *  If the listener is already registered this method should return silently.
     * @param l An instance of TaskLifecycleListener.
     * @throws RemoteException
     */
    public void addTaskEventListener(TaskLifecycleListener l)  throws RemoteException;

    /**
     * Remove the specified listener from the list of registered statusListeners. If the listener is not registered this
     * method should return silently.
     * @param l An instance of TaskLifecycleListener.
     * @throws RemoteException
     */
    public void removeTaskEventListener(TaskLifecycleListener l)  throws RemoteException;

}

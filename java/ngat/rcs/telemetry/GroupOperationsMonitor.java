/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface GroupOperationsMonitor extends Remote {

	/** Add a GroupOperationsListener to the list of statusListeners.
	 * @param l An instance of GroupOperationsListener.
	 * @throws RemoteException
	 */
	public void addGroupOperationsListener(GroupOperationsListener l) throws RemoteException;
	
	/** Remove a GroupOperationsListener from the list of statusListeners.
	 * @param l An instance of GroupOperationsListener.
	 * @throws RemoteException
	 */
	public void removeGroupOperationsListener(GroupOperationsListener l) throws RemoteException;
	
}

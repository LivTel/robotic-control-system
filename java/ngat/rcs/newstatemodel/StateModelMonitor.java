/**
 * 
 */
package ngat.rcs.newstatemodel;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface StateModelMonitor extends Remote {

	/** Add a listener to the list of StateChangeListeners.
	 * @param scl
	 * @throws RemoteException
	 */
	public void addStateChangeListener(StateChangeListener scl)  throws RemoteException;
	
	/** Remove a listener from the list of StateChangeListeners.
	 * @param scl
	 * @throws RemoteException
	 */
	public void removeStateChangeListener(StateChangeListener scl)  throws RemoteException;
	
	
}

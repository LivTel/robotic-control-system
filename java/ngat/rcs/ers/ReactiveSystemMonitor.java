/**
 * 
 */
package ngat.rcs.ers;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface ReactiveSystemMonitor extends Remote {

	/** Add a new ReactiveSystemUpdateListener to the list of listeners.
	 * @param l The listener to add (if not already on list).
	 * @throws RemoteException
	 */
	public void addReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException;
	
	/** Remove a ReactiveSystemUpdateListener from the list of listeners.
	 * @param l The listener to remove (if present).
	 * @throws RemoteException
	 */
	public void removeReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException;
	

}

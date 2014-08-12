package ngat.rcs.newstatemodel;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 
 */

/**
 * @author snf
 *
 */
public interface ControlActionListener extends Remote {

	/** Request handler to perform the specified action.
	 * 
	 * @param ca A control action to perform.
	 * @throws RemoteException
	 */
	public void performAction(ControlAction ca) throws RemoteException;
	
}

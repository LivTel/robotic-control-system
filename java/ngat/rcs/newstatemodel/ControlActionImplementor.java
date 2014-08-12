/**
 * 
 */
package ngat.rcs.newstatemodel;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author snf
 *
 */
public interface ControlActionImplementor extends Remote {
	
	/** Request implementor to perform the specified action.
	 * 
	 * @param ca A control action to perform.
	 * @param handler A ControlActionResponseHandler to accept the response callback.
	 * @throws RemoteException
	 */
	public void performAction(ControlAction ca, ControlActionResponseHandler handler) throws RemoteException;
	
}

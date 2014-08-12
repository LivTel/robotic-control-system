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
public interface ControlActionResponseHandler extends Remote{
	
	/** Reply from implementor to indicate failure of the control action request.*/
	public void controlActionFailed(String message) throws RemoteException;
	
	/** Reply from implementor to indicate successful completion of the control action request.*/
	public void controlActionSuccess() throws RemoteException;
	
	
}

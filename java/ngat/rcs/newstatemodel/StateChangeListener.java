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
public interface StateChangeListener extends Remote {

	public void stateChanged(IState oldState, IState newState) throws RemoteException;
	
}

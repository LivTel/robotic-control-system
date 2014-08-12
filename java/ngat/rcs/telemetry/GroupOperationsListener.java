/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.phase2.IExecutionFailureContext;
import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public interface GroupOperationsListener extends Remote {

	/** Notification that group has been selected for execution.
	 * @param group The group selected.
	 * @throws RemoteException
	 */
	public void groupSelected(GroupItem group) throws RemoteException;
	
	/** Notification that group has completed execution.
	 * @param group The group that has completed.
	 * @param error Indication of successful completion. Non-null if failed.
	 * @throws RemoteException
	 */
	public void groupCompleted(GroupItem group, IExecutionFailureContext error) throws RemoteException;
	
	//public void groupUpdate(GroupItem group) throws RemoteException;
}

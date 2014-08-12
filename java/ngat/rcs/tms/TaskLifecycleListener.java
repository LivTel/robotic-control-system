/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.rcs.tms.events.TaskLifecycleEvent;

/**
 * @author eng
 *
 */
public interface TaskLifecycleListener extends Remote {

	public void taskLifecycleEventNotification(TaskLifecycleEvent event) throws RemoteException;
	
}

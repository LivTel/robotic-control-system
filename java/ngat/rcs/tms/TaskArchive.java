/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ngat.rcs.tms.events.TaskLifecycleEvent;

/**
 * @author eng
 *
 */
public interface TaskArchive extends Remote {
	
	/** Request for archived status information.
	 * @param t1 Start time for archive search.
	 * @param t2 End time for archive search.
	 * @return Archived status information between search time limits.
	 * @throws RemoteException
	 */
	public List<TaskLifecycleEvent> getTaskLifecycleHistory(long t1, long t2) throws RemoteException;
	
}

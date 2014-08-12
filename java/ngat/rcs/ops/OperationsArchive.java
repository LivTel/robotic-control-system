/**
 * 
 */
package ngat.rcs.ops;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author eng
 *
 */
public interface OperationsArchive extends Remote {
	
		
		/** Request for archived status information.
		 * @param t1 Start time for archive search.
		 * @param t2 End time for archive search.
		 * @return Archived status information between search time limits.
		 * @throws RemoteException
		 */
		public List<OperationsEvent> getOperationsHistory(long t1, long t2) throws RemoteException;
		
}

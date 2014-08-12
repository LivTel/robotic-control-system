package ngat.rcs.newstatemodel;

import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * 
 */

/** Classes which wish to be notified of changes in environmental conditions should implement this interface
 * and register with the appropriate EnvironmentalMonitor.
 * @author snf
 *
 */
public interface EnvironmentalChangeListener extends Remote {

	/** Handle the environmental change notified by the event.
	 * @param cev An EnvironmentChangeEvent.
	 * @throws RemoteException If anything goes wrong.
	 */
	public void environmentChanged(EnvironmentChangeEvent cev) throws RemoteException;
	
}

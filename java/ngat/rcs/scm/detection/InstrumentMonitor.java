/**
 * 
 */
package ngat.rcs.scm.detection;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public interface InstrumentMonitor extends Remote {
	
	/** Add an object as an AutoguiderStatusListener.
	 * @param asl The object to add to the list of statusListeners.
	 * @throws RemoteException
	 */
	public void addInstrumentStatusListener(InstrumentStatusListener isl) throws RemoteException;

	/** Remove an object as an AutoguiderStatusListener.
	 * @param asl The object to remove from the list of statusListeners.
	 * @throws RemoteException
	 */
	public void removeInstrumentStatusListener(InstrumentStatusListener isl) throws RemoteException;

	/** Force an instrument lost trigger event.
	 * @throws RemoteException
	 */
	public void triggerInstrumentLost(InstrumentDescriptor instId) throws RemoteException;
	
}

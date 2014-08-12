/**
 * 
 */
package ngat.rcs.ers;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface ReactiveSystemUpdateListener extends Remote {

	public void filterUpdated(String filterName, long time, Number updateValue, Number filterOutputValue) throws RemoteException;
	
	public void criterionUpdated(String critName, long time, boolean critOutputValue) throws RemoteException;
	
	public void ruleUpdated(String ruleName, long time, boolean ruleOutputValue) throws RemoteException;
	
}

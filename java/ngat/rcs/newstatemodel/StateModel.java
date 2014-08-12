package ngat.rcs.newstatemodel;

import java.rmi.*;

/**
 * 
 */

/**
 * @author snf
 *
 */
public interface StateModel extends EnvironmentalChangeListener, StateModelMonitor {

	/**
	 * Add a control action listener to the list of subscribers.
	 * @param cal An instance of ControlActionListener.
	 * @throws RemoteException
	 */
	public void addControlActionListener(ControlActionListener cal) throws RemoteException;
	
	/**
	 * Remove a control action listener from the list of subscribers.
	 * @param cal An instance of ControlActionListener.
	 * @throws RemoteException
	 */
	public void removeControlActionListener(ControlActionListener cal) throws RemoteException;
	
	/**
	 * Add a control action implementor to the list of subscribers.
	 * @param cal An instance of ControlActionImplementor.
	 * @throws RemoteException
	 */
	public void addControlActionImplementor(ControlActionImplementor cal) throws RemoteException;
	/**
	 * Remove a control action implementor from the list of subscribers.
	 * @param cal An instance of ControlActionImplementor.
	 * @throws RemoteException
	 */
	public void removeControlActionImplementor(ControlActionImplementor cal) throws RemoteException;
	
	public void addStateChangeListener(StateChangeListener scl)  throws RemoteException;
	
	public void removeStateChangeListener(StateChangeListener scl)  throws RemoteException;
	
	public IState getCurrentState() throws RemoteException; 
	
}

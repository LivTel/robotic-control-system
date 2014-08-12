/**
 * 
 */
package ngat.rcs.newstatemodel.test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.rcs.newstatemodel.IState;
import ngat.rcs.newstatemodel.StateChangeListener;
import ngat.rcs.newstatemodel.StateModel;

/**
 * @author eng
 *
 */
public class TestStateModelUpdateHandler extends UnicastRemoteObject implements StateChangeListener {

	protected TestStateModelUpdateHandler() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String args[]) {
		
		try {
			
			TestStateModelUpdateHandler tsmu = new TestStateModelUpdateHandler();
			
			StateModel sm = (StateModel)Naming.lookup("rmi://ltsim1/StateModel");
			System.err.println("Located remote state model: "+sm);
			
			sm.addStateChangeListener(tsmu);
			
			while (true) {
				try{Thread.sleep(60000L);}catch (Exception e) {}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.newstatemodel.StateChangeListener#stateChanged(ngat.rcs.newstatemodel.IState, ngat.rcs.newstatemodel.IState)
	 */
	public void stateChanged(IState oldState, IState newState)
			throws RemoteException {
		
		System.err.println("The current state of the system is: "+newState);

	}

}

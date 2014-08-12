/**
 * 
 */
package ngat.rcs.scm.test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.tcm.AutoguiderMonitor;
import ngat.tcm.AutoguiderMonitorStateListener;

/**
 * @author eng
 *
 */
public class TestAgMonitorStateListener extends UnicastRemoteObject implements AutoguiderMonitorStateListener {

	protected TestAgMonitorStateListener() throws RemoteException {
		super();		
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.detection.AutoguiderMonitorStateListener#autoguiderMonitorEnabled(boolean)
	 */
	public void autoguiderMonitorEnabled(boolean enabled) throws RemoteException {
		System.err.println("AgMStateChange: Monitor enabled: "+enabled);

	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.detection.AutoguiderMonitorStateListener#autoguiderMonitorWasReset()
	 */
	public void autoguiderMonitorWasReset() throws RemoteException {
		System.err.println("AgMStateChange: Monitor was reset");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			TestAgMonitorStateListener agsl = new TestAgMonitorStateListener();
			String host = args[0];
			AutoguiderMonitor agmon = (AutoguiderMonitor)Naming.lookup("rmi://"+host+"/AutoguiderMonitor");
			agmon.addAutoguiderMonitorStateListener(agsl);
			
			// loop forever and ever and ever...
			while (true){try{Thread.sleep(60000);}catch(InterruptedException ix) {}}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

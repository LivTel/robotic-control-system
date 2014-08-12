/**
 * 
 */
package ngat.rcs.scm.test;

import java.rmi.Naming;

import ngat.tcm.AutoguiderMonitor;

/**
 * @author eng
 *
 */
public class SendAgLockLostTrigger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String host = args[0];
		try {
			AutoguiderMonitor agmon = (AutoguiderMonitor)Naming.lookup("rmi://"+host+"/AutoguiderMonitor");
			System.err.println("Found agmon: "+agmon);
			agmon.triggerLockLost();
			System.err.println("Agmon lock lost trigger sent");			
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

}

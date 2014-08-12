/**
 * 
 */
package ngat.rcs.scm.test;

import java.rmi.Naming;

import ngat.tcm.TrackingMonitor;


/**
 * @author eng
 *
 */
public class SendTrackingLostTrigger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String host = args[0];
		try {
			TrackingMonitor tmon = (TrackingMonitor)Naming.lookup("rmi://"+host+"/TrackingMonitor");
			System.err.println("Found trkmon: "+tmon);
			tmon.triggerTrackingLost();
			System.err.println("Trkmon tracking lost trigger sent");			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

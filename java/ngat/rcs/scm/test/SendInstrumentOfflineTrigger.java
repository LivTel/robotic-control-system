/**
 * 
 */
package ngat.rcs.scm.test;

import java.rmi.Naming;

import ngat.icm.InstrumentDescriptor;
import ngat.rcs.scm.detection.InstrumentMonitor;

/**
 * @author eng
 *
 */
public class SendInstrumentOfflineTrigger {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String host = args[0];
		String instName = args[1];
		InstrumentDescriptor iid = new InstrumentDescriptor(instName);
		try {
			InstrumentMonitor imon = (InstrumentMonitor)Naming.lookup("rmi://"+host+"/InstrumentMonitor");
			System.err.println("Found instmon: "+imon);
			imon.triggerInstrumentLost(iid);
			System.err.println("Instmon "+instName+" lost trigger sent");			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

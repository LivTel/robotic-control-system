/**
 * 
 */
package ngat.rcs.test;

import ngat.message.ISS_INST.CONFIG;
import ngat.phase2.THORConfig;
import ngat.phase2.THORDetector;

/**
 * @author eng
 *
 */
public class SendThorConfig {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		CONFIG config = new CONFIG("test");
		
		THORConfig tc = new THORConfig("tt");
		tc.setEmGain(5);
		THORDetector td = new THORDetector();
		td.setXBin(1);
		td.setYBin(1);
		td.clearAllWindows();
		
		tc.setDetector(0, td);
		config.setConfig(tc);
		
		try {
			SendCommand sender = new SendCommand(host, port);			
			sender.sendCommand(config);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

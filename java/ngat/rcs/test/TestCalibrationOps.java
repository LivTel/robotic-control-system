/**
 * 
 */
package ngat.rcs.test;

import java.io.File;

import ngat.rcs.calib.CalibrationControlAgent;

/** Test the calibration control agent (CAL).
 * @author eng
 *
 */
public class TestCalibrationOps {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		try {
		
		CalibrationControlAgent cal = (CalibrationControlAgent)CalibrationControlAgent.getInstance();
		cal.configure(new File(args[0]));
		
		// waht time is it 
		long time = System.currentTimeMillis();
		boolean wc = cal.wantsControl(time);
		System.err.printf("CAL wants control at: %tF %tT", time, time);
		
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

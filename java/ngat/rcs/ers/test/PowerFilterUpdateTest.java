/**
 * 
 */
package ngat.rcs.ers.test;

import ngat.rcs.ers.PowerCycleFilter;

/**
 * @author eng
 *
 */
public class PowerFilterUpdateTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
		
			double hours = 10.5*3600.0*1000.0;
			
			PowerCycleFilter pcf = new PowerCycleFilter("PWR_TEST");
			pcf.setRebootOffset((long)hours);
			
			long start = System.currentTimeMillis();
			long time = start;
			while (time < start +24*3600*1000L) {
				
				Number update = pcf.filterUpdate(time, null);
				
				double uval = update.doubleValue();
				
				System.err.printf("%tF %tT %4.2f \n", time, time, uval);
				
				time += 10*1000L; // 10 sec update
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

/**
 * 
 */
package ngat.rcs.calib.test;

import java.util.Calendar;
import java.util.Date;

/**
 * @author eng
 *
 */
public class TelfocusTimingTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		long time = System.currentTimeMillis();
		long start = time;
		while (time < start +24*3600*1000L) {
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(time));
			int h = cal.get(Calendar.HOUR_OF_DAY);
			int m = cal.get(Calendar.MINUTE);

			// TEMP change to 5 am
			boolean round_midnight_and_m_le_10 = (   ((h == 23) && (m >= 50)) || 
													 ((h == 0) && ((m <= 10) || (m >= 50))) ||
						                             ((h == 1) && (m < 10)));
			
			System.err.printf("%tT : %s\n", time, (round_midnight_and_m_le_10 ? "RUN":""));
			time += 60*1000L;
		}
		
	}

}

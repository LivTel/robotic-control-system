/**
 * 
 */
package ngat.rcs.ers;

import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import ngat.astrometry.ISite;
import ngat.rcs.newstatemodel.PowerCycleStatus;

/**
 * @author eng
 * 
 */
public class PowerCycleFilter extends DiscreteFilter {

	/** The time after the onset of reboot start time in which we need to make the reboot decision.
	 * This interval should not be so long that we could reboot and be up and running again before the
	 * interval has expired in which case we might reboot again immediately. The interval should not
	 * be so short that we might not have gathered enough evidence to make the decision before it changes
	 * back to RUN.
	 */
	private static final long REBOOT_NOTICE_INTERVAL = 3*60*1000L;
	
	private static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/** Time of day offset to reboot since previous midnight */
	private long rebootOffset;

	public PowerCycleFilter(String name) {
		super(name);
	}

	public long getRebootOffset() {
		return rebootOffset;
	}

	public void setRebootOffset(long rebootOffset) {
		this.rebootOffset = rebootOffset;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.newenv.Filter#filterUpdate(long, java.lang.Number)
	 */
	public int processUpdate(long time, int value) {
		try {
			// calculate the time of day for midnight

			//TimeZone.setDefault(UTC);
			
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(UTC);
			cal.setTimeInMillis(time);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			// this is the last available midnight prior to time.
			long midnight = cal.getTimeInMillis();

			long rebootTime = midnight + rebootOffset;
			
			//System.err.printf("PCF: Compute: Next reboot starts: %tF %tT \n",rebootTime, rebootTime);
			//System.err.println("PCF: Compute: Next rebooting in: "+(rebootTime-System.currentTimeMillis())/1000+"s");

			// we must reboot within a short interval of reboot time
			if (time > rebootTime && time < rebootTime + REBOOT_NOTICE_INTERVAL)
				return PowerCycleStatus.POWER_OFF;
			else
				return PowerCycleStatus.POWER_ON;
		} catch (Exception e) {
			e.printStackTrace();
			return 0; // UNKNOWN
		}
	}
	
	public String toString() {
		return "PowerCycleFilter: " + getFilterName() + ", using:"+sourceDescription;
	}
}

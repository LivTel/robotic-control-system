/**
 * 
 */
package ngat.rcs.newstatemodel;

import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class PowerCycleStatus implements StatusCategory {

	/** Status value indicating the RCS should continue to run.*/
	public static final int POWER_ON = 1;
	
	/** Status value indicating the RCS should power-down (restart).*/
	public static final int POWER_OFF = 2;
	
	/** Status time stamp.*/
	private long timeStamp;
	
	/** Current power state.*/
	private int state;
	
	/**
	 * @param timeStamp
	 */
	public PowerCycleStatus(long timeStamp) {
		super();
		this.timeStamp = timeStamp;
	}

	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getCategoryName()
	 */
	public String getCategoryName() {
		return "PWR";
	}

	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getStatusTimeStamp()
	 */
	public long getStatusTimeStamp() {
		return timeStamp;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	public String toString() { return "PowerStatus: "+state;}

}

/**
 * 
 */
package ngat.rcs.newstatemodel;

import java.io.Serializable;

import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class StateModelEvent implements Serializable, StatusCategory {
	
	/** Time the event occurred.*/
	protected long statusTimeStamp;

	/**
	 * @param statusTimeStamp
	 */
	public StateModelEvent(long statusTimeStamp) {
		super();
		this.statusTimeStamp = statusTimeStamp;
	}
	
	@Override
	public String toString(){ 
		return String.format("StateModelEvent %tF %tT.%tL ", 			
				statusTimeStamp, 
				statusTimeStamp,
				statusTimeStamp);
	}

	public String getCategoryName() {
		return "SME";
	}
	
	/**
	 * @return time of this event.
	 */
	public long getStatusTimeStamp() {
		return statusTimeStamp;
	}
	
}

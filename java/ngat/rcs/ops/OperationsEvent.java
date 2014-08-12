/**
 * 
 */
package ngat.rcs.ops;

import java.io.Serializable;

import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class OperationsEvent implements Serializable, StatusCategory {

	/** Time the event occurred.*/
	protected long statusTimeStamp;

	
	/**
	 * @param statusTimeStamp
	 */
	public OperationsEvent(long eventTimeStamp) {
		super();
		this.statusTimeStamp = eventTimeStamp;
	}

	
	
	@Override
	public String toString(){ 
		return String.format("OperationsEvent %tF %tT.%tL ", 			
				statusTimeStamp, 
				statusTimeStamp,
				statusTimeStamp);
	}

	public String getCategoryName() {
		return "OPS";
	}
	/**
	 * @return time of this event.
	 */
	public long getStatusTimeStamp() {
		return statusTimeStamp;
	}
	
}

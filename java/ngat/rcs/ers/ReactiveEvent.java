/**
 * 
 */
package ngat.rcs.ers;

import java.io.Serializable;

import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class ReactiveEvent implements Serializable, StatusCategory {

	/** Time the event occurred.*/
	protected long statusTimeStamp;
	
	

	public ReactiveEvent(long statusTimeStamp) {
		super();
		this.statusTimeStamp = statusTimeStamp;
	}

	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getCategoryName()
	 */
	public String getCategoryName() {
		return "ERS";
	}

	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getStatusTimeStamp()
	 */
	public long getStatusTimeStamp() {
	return statusTimeStamp;
	}
	
	@Override
	public String toString(){ 
		return String.format("ReactiveEvent %tF %tT.%tL ", 			
				statusTimeStamp, 
				statusTimeStamp,
				statusTimeStamp);
	}

}

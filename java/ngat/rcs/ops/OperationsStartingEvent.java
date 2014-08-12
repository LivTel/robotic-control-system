/**
 * 
 */
package ngat.rcs.ops;


/**
 * @author eng
 *
 */
public class OperationsStartingEvent extends OperationsEvent {

	// temporary label
	private String eventName;
	

	/**
	 * @param eventTimeStamp
	 * @param eventName
	 */
	public OperationsStartingEvent(long eventTimeStamp, String eventName) {
		super(eventTimeStamp);
		this.eventName = eventName;
	}
	
	public String toString() { 
			return super.toString()+"Starting: "+eventName;
	}
}

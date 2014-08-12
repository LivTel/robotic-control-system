/**
 * 
 */
package ngat.rcs.ops;


/**
 * @author eng
 *
 */
public class OperationsModeChangedEvent extends OperationsEvent {

	private String oldMode;
	
	private String newMode;

	/**
	 * @param oldMode
	 * @param newMode
	 */
	public OperationsModeChangedEvent(long eventTimeStamp, String oldMode, String newMode) {
		super(eventTimeStamp);
		this.oldMode = oldMode;
		this.newMode = newMode;
	}
	
	/**
	 * @return the oldMode
	 */
	public String getOldMode() {
		return oldMode;
	}

	/**
	 * @return the newMode
	 */
	public String getNewMode() {
		return newMode;
	}

	@Override
	public String toString()   {
		return super.toString()+" : Mode changed from: "+oldMode+" to: "+newMode;
	}
	
}

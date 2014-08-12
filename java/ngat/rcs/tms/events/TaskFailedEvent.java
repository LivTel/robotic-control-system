/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskFailedEvent extends TaskLifecycleEvent {

	/** Error condition which caused the failure.*/
	private ErrorIndicator error;
	
	/**
	 * @param task
	 */
	public TaskFailedEvent(long eventTimeStamp, TaskDescriptor task, ErrorIndicator error) {
		super(eventTimeStamp, task);
		this.error = error;
	}
	
	/**
	 * @return the error
	 */
	public ErrorIndicator getError() {
		return error;
	}
	
	@Override
	public String toString(){ 
		return super.toString()+" : Failed: "+error;
	}

}

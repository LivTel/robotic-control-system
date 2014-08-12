/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskCancelledEvent extends TaskLifecycleEvent {
	
	/**
	 * @param task
	 */
	public TaskCancelledEvent(long eventTimeStamp, TaskDescriptor task) {
		super(eventTimeStamp, task);
	}
	
	@Override
	public String toString(){ 
		return super.toString()+" : Cancelled";
	}

}

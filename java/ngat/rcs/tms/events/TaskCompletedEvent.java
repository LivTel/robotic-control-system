/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskCompletedEvent extends TaskLifecycleEvent {

	/**
	 * @param task
	 */
	public TaskCompletedEvent(long eventTimeStamp, TaskDescriptor task) {
		super(eventTimeStamp, task);
	}
	
	@Override
	public String toString(){ 
		return super.toString()+" : Completed";
	}

}

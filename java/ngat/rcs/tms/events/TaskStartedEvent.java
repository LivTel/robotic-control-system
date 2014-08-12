/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskStartedEvent extends TaskLifecycleEvent {

	/**
	 * @param task
	 */
	public TaskStartedEvent(long eventTimeStamp, TaskDescriptor task) {
		super(eventTimeStamp, task);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String toString(){ 
		return super.toString()+" : Started";
	}
}

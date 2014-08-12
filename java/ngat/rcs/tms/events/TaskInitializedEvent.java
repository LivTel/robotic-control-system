/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskInitializedEvent extends TaskLifecycleEvent {

	/**
	 * 
	 */
	public TaskInitializedEvent(long eventTimeStamp, TaskDescriptor task) {
		super(eventTimeStamp, task);
	}

	@Override
	public String toString(){ 
		return super.toString()+" : Initialized";
	}
}

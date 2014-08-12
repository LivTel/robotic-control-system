/**
 * 
 */
package ngat.rcs.tms.events;

import ngat.rcs.tms.TaskDescriptor;

/**
 * @author eng
 *
 */
public class TaskCreatedEvent extends TaskLifecycleEvent {

	TaskDescriptor mgr;
	
	/**
	 * @param task
	 */
	public TaskCreatedEvent(long eventTimeStamp,TaskDescriptor mgr, TaskDescriptor task) {
		super(eventTimeStamp, task);
		this.mgr = mgr;
	}

	/**
	 * @return the mgr
	 */
	public TaskDescriptor getMgr() {
		return mgr;
	}
	
	@Override
	public String toString(){ 
		return super.toString()+" : Created by:"+
				(mgr != null ?
						"["+mgr.getUid()+"] "+mgr.getName()+" "+mgr.getTypeName() : "null");
	}
}

/**
 * 
 */
package ngat.rcs.telemetry;

import ngat.phase2.IExecutionFailureContext;
import ngat.sms.GroupItem;

/** Event describing a group completion.
 * @author eng
 *
 */
public class CompletionEvent extends MonitorEvent {

	private GroupItem group;
	
	private IExecutionFailureContext error;

	/**
	 * @param group
	 * @param error
	 */
	public CompletionEvent(GroupItem group, IExecutionFailureContext error) {
		super();
		this.group = group;
		this.error = error;
	}

	/**
	 * @return the group
	 */
	public GroupItem getGroup() {
		return group;
	}

	/**
	 * @return the error
	 */
	public IExecutionFailureContext getError() {
		return error;
	}
	
	
	
}

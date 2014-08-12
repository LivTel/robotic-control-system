/**
 * 
 */
package ngat.rcs.ops;

import ngat.phase2.IExecutionFailureContext;
import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class GroupCompletedEvent extends OperationsEvent {

	private GroupItem group;
	
	private IExecutionFailureContext error;

	/**
	 * @param eventTimeStamp
	 * @param group
	 * @param error
	 */
	public GroupCompletedEvent(long eventTimeStamp, GroupItem group, IExecutionFailureContext error) {
		super(eventTimeStamp);
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
	 * @param group the group to set
	 */
	public void setGroup(GroupItem group) {
		this.group = group;
	}

	/**
	 * @return the error
	 */
	public IExecutionFailureContext getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(IExecutionFailureContext error) {
		this.error = error;
	}
	
	@Override
	public String toString() {
		return super.toString()+" GroupCompleted: "+
				(group != null ? group.getName()+(error != null ? " failed:"+error.getErrorCode():" okay") : "null");
	}
	
}
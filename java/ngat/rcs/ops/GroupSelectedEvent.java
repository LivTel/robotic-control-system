/**
 * 
 */
package ngat.rcs.ops;

import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class GroupSelectedEvent extends OperationsEvent {

	private GroupItem group;

	/**
	 * @param eventTimeStamp
	 * @param group
	 */
	public GroupSelectedEvent(long eventTimeStamp, GroupItem group) {
		super(eventTimeStamp);
		this.group = group;
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
	
	@Override
	public String toString() {
		return super.toString()+" GroupSelected: "+(group != null ? group.getName():"null");
	}
	
}

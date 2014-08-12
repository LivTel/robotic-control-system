/**
 * 
 */
package ngat.rcs.sciops;

import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class GroupData {

	private GroupItem group;
	
	private long histId;

	public GroupData(GroupItem group, long histId) {
		this.group = group;
		this.histId = histId;
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
	 * @return the histId
	 */
	public long getHistId() {
		return histId;
	}

	/**
	 * @param histId the histId to set
	 */
	public void setHistId(long histId) {
		this.histId = histId;
	}
	
}

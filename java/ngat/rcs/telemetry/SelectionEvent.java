/**
 * 
 */
package ngat.rcs.telemetry;

import ngat.sms.GroupItem;



/** Event describing a group selection.
 * @author eng
 *
 */
public class SelectionEvent extends MonitorEvent {
	
	private GroupItem group;

	/**
	 * @param group
	 */
	public SelectionEvent(GroupItem group) {
		super();
		this.group = group;
	}

	/**
	 * @return the group
	 */
	public GroupItem getGroup() {
		return group;
	}
	
	
	
}

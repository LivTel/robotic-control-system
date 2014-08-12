/**
 * 
 */
package ngat.rcs.ers;

import ngat.rcs.newstatemodel.PowerCycleStatus;

/**
 * @author eng
 *
 */
public class PowerCycleFilterAdapter implements FilterAdapter {
	
	private String catName;

	private String itemName;

	private Filter filter;

	/** Most of these params are not used.*/
	public PowerCycleFilterAdapter(String catName, String itemName,
			Filter filter) {
		super();
		this.catName = catName;
		this.itemName = itemName;
		this.filter = filter;
	}

	public String getCatName() {
		return catName;
	}



	public String getItemName() {
		return itemName;
	}
	public Filter getFilter() {
		return filter;
	}
	
	/** Extracts the relevant status entry for which this adapter has been setup.
	 * @param status
	 * @return The required status entry or NULL if not available.
	 */
	public Number getStatusItem(PowerCycleStatus status) {
		// we just return the value of the supplied status
		return status.getState();		
	}
	
}

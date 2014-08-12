/**
 * 
 */
package ngat.rcs.ers;

/** A discrete filter which returns the steady value of the input. 
 * @author eng
 *
 */
public class ActualStateFilter extends DiscreteFilter {
	
	
	/**
	 * @param name The name of the filter.
	 */
	public ActualStateFilter(String name) {
		super(name);
	}
	
	
	/** Process an update. 
	 */
	@Override
	protected int processUpdate(long time, int ivalue) {
		return ivalue;	
	}

	@Override
	public String toString() {
		return "ActualFilter: " + getFilterName()+", using:"+sourceDescription;
	}

}

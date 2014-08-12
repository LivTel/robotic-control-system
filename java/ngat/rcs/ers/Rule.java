/**
 * 
 */
package ngat.rcs.ers;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public interface Rule extends Serializable {

	/**
	 * @return Name of this rule.
	 */
	public String getRuleName();
	
	/** Apply rule update.
	 * @param time Time of the update.
	 * @param criterion State of criterion.
	 * @return
	 */
	public double ruleUpdate(long time, boolean criterion);
	
	
}

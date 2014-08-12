/**
 * 
 */
package ngat.rcs.ers;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public interface Criterion extends Serializable {

	
	/**
	 * @return Criterion name.
	 */
	public String getCriterionName();
	
	/** Apply criterion update.
	 * @param time Time of the update.
	 * @param value Value of update.
	 */
	public boolean criterionUpdate(long time, Number value);
	
}

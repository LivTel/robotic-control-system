/**
 * 
 */
package ngat.rcs.ers;

import java.io.Serializable;

/**
 * @author eng
 *
 */
/**
 * @author eng
 *
 */
public interface Filter extends Serializable {

	/**
	 * @return Name of the filter.
	 */
	public String getFilterName();
	
	/**
	 * @return Description of filter.
	 */
	public String getFilterDescription();
	
	/**
	 * @return Name of the filter's input source.
	 */
	public String getSourceName();
	
	/**
	 * @return Description of source.
	 */
	public String getSourceDescription();
	
	/** Apply filter update.
	 * @param time Time of the update.
	 * @param value Value of update.
	 */
	public Number filterUpdate(long time, Number value);
	
}

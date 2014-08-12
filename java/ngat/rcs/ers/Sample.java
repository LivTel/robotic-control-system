/**
 * 
 */
package ngat.rcs.ers;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public class Sample implements Serializable {

	public long time;
	
	public Number value;

	/**
	 * @param time
	 * @param value
	 */
	public Sample(long time, int value) {
		super();
		this.time = time;
		this.value = value;
	}
	
	/**
	 * @param time
	 * @param value
	 */
	public Sample(long time, double value) {
		super();
		this.time = time;
		this.value = value;
	}
	
}

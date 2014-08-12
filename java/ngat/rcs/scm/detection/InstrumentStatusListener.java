/**
 * 
 */
package ngat.rcs.scm.detection;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public interface InstrumentStatusListener {
	
	/** Handle an instrument lost event.
	 */
	public void instrumentLost(InstrumentDescriptor instId);
	
}

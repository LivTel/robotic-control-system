/**
 * 
 */
package ngat.rcs.sciops;

import java.util.Iterator;
import java.util.List;

import ngat.phase2.IExecutiveAction;
import ngat.phase2.IExposure;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.IInstrumentConfigSelector;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ISlew;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;

/**
 * @author eng
 * 
 */
public class ConfigFinder {

	boolean foundSlew = false;
	ISlew slew = null;
	
	boolean foundConfig = false;
	IInstrumentConfig config = null;
	
	boolean foundExposure = false;
	
	
	/**
	 * @param slew
	 */
	public ConfigFinder(ISlew slew) {
		super();
		this.slew = slew; 
		System.err.println("CF:: Starting ConfigFinder for slew: "+slew);
	}


	/**
	 * Given the root component (or some intermediate sequence iterator, find
	 * the instrument which will be used for the next exposure after the
	 * specified slew.
	 * @param iterator The iterator to search.
	 * @param slew The slew for which we wish to find a matching config.
	 * 
	 */
	public void locate(XIteratorComponent iterator) {
		
		  System.err.println("CF: Locate, testing iterator: "+iterator.getComponentName());

		if (foundExposure)
			return;
	
		List compList = iterator.listChildComponents();
		Iterator comps = compList.iterator();
		while (comps.hasNext()) {
			ISequenceComponent comp = (ISequenceComponent) comps.next();
			if (comp instanceof XExecutiveComponent) {
				IExecutiveAction action = ((XExecutiveComponent) comp).getExecutiveAction();
				if (action instanceof ISlew) { 
					System.err.println("CF: Locate, checking slew: "+action.getActionDescription());

					if ((ISlew)action == slew) {
						foundSlew = true;	
						System.err.println("CF: Locate, this is the slew we are looking for");
				
					}
				} else if 
					(action instanceof IInstrumentConfigSelector) {
					 System.err.println("CF: Locate, checking config: "+action.getActionDescription());

					if (foundSlew) {
						 System.err.println("CF: Locate, this is now current config");

						// we have the slew so this is the next config, or the next next
						config = ((IInstrumentConfigSelector)action).getInstrumentConfig();
					}
				} else if
					(action instanceof IExposure) {  
					System.err.println("CF: Locate, checking exposure: "+action.getActionDescription());

					if (foundSlew) {
						 System.err.println("CF: Locate, found the exposure, the relevant config is: "+config);

						// we have the exposure, hopefully we have the correct config
						foundExposure = true;
						return;
					}
				}
			} else if (comp instanceof XIteratorComponent) {
				// recursively sweep sub-iterators
				locate((XIteratorComponent) comp);
			}
		}
		// At this point we should have the last target and config settings and
		// whether we are guiding, tracking
	}


	/** Return the config we were looking for (hopefully).
	 * @return the config
	 */
	public IInstrumentConfig getConfig() {
		return config;
	}


	
}

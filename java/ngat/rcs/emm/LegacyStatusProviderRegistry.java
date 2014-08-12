/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package ngat.rcs.emm;

import ngat.rcs.scm.collation.SkyModelProvider;
import ngat.util.*;

import java.util.*;

/** This class acts as a hook for providing status information.
 * All newer systems provide telemetry via pub-sub mechanisms and the OpsUI leverages these mechanisms
 * to obtain most of its status feeds.
 * 
 * For historic reasons relating to network architecture, the live-status system uses polling to obtain status.
 * 
 * Most systems will need for the foreseeable future to register a provider with the 
 * LegacyStatusProviderRegistry using a call to: 
 *  LegacyStatusProviderRegistry.addStatusCategory(String cat, StatusProvider provider).
 *  
 * Status information is obtained via GUI_RCS.GET_STATUS and similar commands handled by the relevant
 * implementors in ngat.rcs.control package using calls to:
 *  LegacyStatusProviderRegistry.getStatusCategory(String cat)
 *
 * It should also be noted that the status classes in the legacy system implement ngat.util.StatusCategory, whereas status
 * provided by the newer telemetry feeds inherit from ngat.net.telemetry.StatusCategory
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: EMM_Registry.java,v 1.1 2006/12/12 08:29:47 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/EMM_Registry.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class LegacyStatusProviderRegistry implements StatusCategoryGrabber {

    /** Holds references to the category-specific StatusProviders.*/
    protected HashMap categories;

    /** Static (singleton) instance.*/
    protected static LegacyStatusProviderRegistry instance;

    /** Legacy entry to obtain historic sky seeing data.*/
    private SkyModelProvider smp;
    
    /** Create an EMM_Registry.*/
    private LegacyStatusProviderRegistry() {
    	categories = new HashMap();
    }
    
    /** Returns the static (singleton) instance.*/
    public static LegacyStatusProviderRegistry getInstance() {
	if (instance == null)
	    instance = new LegacyStatusProviderRegistry();
	return instance;
    }

    /** Returns the appropriate Status Category.*/
    public StatusCategory getStatusCategory(String cat) throws IllegalArgumentException {	
	StatusProvider provider = (StatusProvider)categories.get(cat);
	if (provider == null)
	    throw new IllegalArgumentException("EMM_Registry: No provider for category: "+cat);	
	return provider.getStatus();	
    }

    /** Add a StatusEntryGrabber for the specified key.*/
    public void addStatusCategory(String cat, StatusProvider provider) {
	if (categories.containsKey(cat)) return;
	categories.put(cat, provider);
	System.err.println("EMM::Added status provider for category: "+cat+" via "+provider);
    } 

    /** Returns a list of categories available.*/
    public Iterator listCategories() { return categories.keySet().iterator(); }

    /** Get the SkyModelProvider - note this does not implement StatusCategory so cannot be treated like
     * other providers.
     * @return
     */
	public SkyModelProvider getSkyModelProvider() {
		return smp;
	}

	/**
	 * Set the SkyModelProvider - note this does not implement StatusCategory so cannot be treated like
     * other providers.
	 * @param smp
	 */
	public void setSkyModelProvider(SkyModelProvider smp) {
		this.smp = smp;
		System.err.println("EMM::Added status provider for category for: SEEING via "+smp);
	}
    
    
    
}

/** $Log: EMM_Registry.java,v $
/** Revision 1.1  2006/12/12 08:29:47  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:45  snf
/** Initial revision
/** */

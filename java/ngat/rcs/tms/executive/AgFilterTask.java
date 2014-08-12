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
package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing an Autoguider Filter (DarkSlide) deployment.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AgFilterTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/AgFilterTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class AgFilterTask extends Default_TaskImpl {
  
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 180000L;    

    /** The autoguider.*/
    protected int state;
    
    Logger logger;
    
    /** Create an AgSelectTask using the suppliedautoguider.
     * @param state The filter deployment state (IN or OUT).
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public AgFilterTask(String      name,
			TaskManager manager,
			int         state) {
	super(name, manager, "CIL_PROXY");
	this.state = state;
	    	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	AGFILTER agFilter = new AGFILTER(name);
	agFilter.setState(state);
	
	command = agFilter;
	
	logger = LogManager.getLogger("TASK");
	
    }     

    /** Carry out subclass specific initialization.*/
    @Override
	protected void onInit() {	
	super.onInit();
	String strState = null;
	switch (state) {
	case AGFILTER.IN:
	    strState = "INLINE";
	    break;
	case AGFILTER.OUT:
	    strState = "RETRACTED";
	    break;
	default:
	    strState = "UNKNOWN:"+state;
	    break;
	}
	logger.log(1, CLASS, name, "onInit",
		   "Starting Autoguider Filter Deployment to: "+strState);
    }
    
    /** Carry out subclass specific completion work. ## NONE ##.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Autoguider filter deployment completed");
    }
    
    /** Carry out subclass specific disposal work.   ## NONE ##.*/
    @Override
	protected void onDisposal() {
	super.onDisposal();
    }

    
}

/** $Log: AgFilterTask.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/** */


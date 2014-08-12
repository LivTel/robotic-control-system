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
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing an Telescope FocusOffset. 
 * @author $Author: snf $
 * @version $version$
 */
public class DefocusTask extends Default_TaskImpl {
    
    public static final String CLASS = "DefocusTask";
    
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** The Offset in mm.*/
    protected double offset;
    
    /** Create an Exposure_Task using the supplied Observation and settings.
     * Sets the Instrument and creates a Connection to its ControlSystem.
     * If the subsystem resource (ControlSystem) cannot be found ???.
     * @param deltaRA  The Offset in RA (rads).
     * @param deltaDec The Offset in Dec (rads).
     * @param name     The unique name/id for this TaskImpl.
     * @param manager  The Task's manager.
     */
    public DefocusTask(String      name,
		       TaskManager manager,
		       double      offset) {
	super(name, manager, "CIL_PROXY");
	this.offset = offset;
	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	
	DFOCUS defocus = new DFOCUS(name);
	defocus.setOffset(offset);
	
	command = defocus;	
    }
    

    /** Carry out subclass specific initialization.*/
    @Override
	protected void onInit() {	
	super.onInit();
			
	logger.log(1, CLASS, name, "onInit",
		   "Starting focus offset:"+
		   " offset: "+offset+" mm");
	
    }
    
    /** Carry out subclass specific completion work. ## NONE ##.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Completed focus offset.");	
    }
  
}

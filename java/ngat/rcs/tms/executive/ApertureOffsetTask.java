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

/** A leaf Task for notifying the Telescope of a change of instrument aperture.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: ApertureOffsetTask.java,v 1.2 2007/07/05 11:29:24 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/ApertureOffsetTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class ApertureOffsetTask extends Default_TaskImpl {
  
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;
    
    /** The instrument offset number.*/
    protected int number;
    
    /** Create a InstrumentSetApertureTask using the supplied number.
     * @param number   The aperture number to use.
     * @param name     Unique name/id for this TaskImpl.    
     * @param manager  Task's manager.
     */
    public ApertureOffsetTask(String      name,
			      TaskManager manager,
			      int         number) {
			 
	super(name, manager, "CIL_PROXY");
	this.number = number;
	
	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	APERTURE ap = new APERTURE(name);
	ap.setNumber(number);

	command = ap;
	
	logger = LogManager.getLogger("TASK");
	
    }  

    @Override
	protected void onInit() {
	super.onInit();
	
	logger.log(1, CLASS, name, "onInit", 
		   "Starting Instrument aperture offset using aperture #"+number);
    }
    
    /** Carry out subclass specific completion work. ## NONE ##.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	logger.log(1, CLASS, name, "onCompletion",
		  "Instrument aperture offset completed");
    }
 
    /** Return the aperture we are using.*/
    public int getNumber() { return number;}

}



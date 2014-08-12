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
/** A leaf Task for performing a Telescope Altitude demand. An appropriate ALTITUDE 
 * command is generated and sent to the telescope control system.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AltitudeTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/AltitudeTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class AltitudeTask extends Default_TaskImpl {
    
    public static final String CLASS = "AltitudeTask";

    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** The altitude to move to (rads).*/
    protected double altitude;

    /** Create an Altitude task to move to specifed position.
     * If the subsystem resource (TelescopeControlSystem) cannot be found ???.
     * @param altitude The altitude demand (rads).
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public AltitudeTask(String name,
			TaskManager manager,
			double altitude) {
	super(name, manager, "CIL_PROXY");
	this.altitude = altitude;
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	ALTITUDE alt = new ALTITUDE(name);
	alt.setAngle(altitude);

	command = alt;
	
    }
    
    /** Returns the default time for this command to execute.*/
    public static long getDefaultTimeToComplete() {
	//return  RCS_Configuration.getLong("tcs_command.altitude.timeout", DEFAULT_TIMEOUT);
	return DEFAULT_TIMEOUT;
    }
    
    /** Compute the estimated completion time.
     * @return The initial estimated completion time in millis.*/
    @Override
	protected long calculateTimeToComplete() {
	return getDefaultTimeToComplete();
    }
    
    /** Carry out subclass specific initialization.*/
    @Override
	protected void onInit() {	
	super.onInit();
	logger.log(1, CLASS, name, "onInit",
		   "Starting Move to Altitude: "+Position.toDegrees(altitude, 3));
    }
    
    /** Carry out subclass specific completion work. ## NONE ##.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Altitude move completed");
    }
    
    /** Carry out subclass specific disposal work.   ## NONE ##.*/
    @Override
	protected void onDisposal() {
	super.onDisposal();
    }

}

/** $Log: AltitudeTask.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/***/

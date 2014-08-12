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

/** A leaf Task for performing a pointing calibration.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: PointingTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/PointingTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class PointingTask extends Default_TaskImpl {
  
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** The calibrate-mode.*/
    protected int calMode;

    /** Stroes the Sky Rms returned from the TCS.*/
    protected double skyRms;
    
    /** Create a PointingTask using the supplied cal-mode.
     * @param calMode The calibrate-mode.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public PointingTask(String      name,
			TaskManager manager,
			int         calMode) {
	super(name, manager, "CIL_PROXY");
	this.calMode = calMode;
	    	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	CALIBRATE cal = new CALIBRATE(name);
	cal.setMode(calMode);
	
	command = cal;
	
    }     

    /** Carry out subclass specific completion work.
     * Save the received value of Sky Rms.*/
    @Override
	public void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	if (response instanceof CALIBRATE_DONE) {
	    CALIBRATE_DONE caldone = (CALIBRATE_DONE)response;
	    logger.log(1, CLASS, name, "onCompletion", "CALIBRATE_DONE received:"+
		       "\nSky rms:        "+caldone.getRmsError());
	    skyRms   = caldone.getRmsError();	  
	}
    }
    
    /** Returns the Sky Rms received from the TCS.*/
    public double getSkyRms() { return skyRms; }
    
}


/** $Log: PointingTask.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */

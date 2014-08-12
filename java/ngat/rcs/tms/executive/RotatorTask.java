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

/** This Task sets the Telescope rotator mode and positioning.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: RotatorTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/RotatorTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RotatorTask extends Default_TaskImpl {
    
    /** Rotator angle (rads).*/
    protected double angle;

    /** Rotator mode.*/
    protected int mode;

    /** Rotator mode name.*/
    protected String modeName;

    /** Create an Rotator_Task using the supplied TelescopeConfig.
     * @param angle The rotator angle (mode dependant.)
     * @param mode The rotator mode.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public RotatorTask(String          name,
		       TaskManager     manager,
		       double          angle,
		       int             mode) {
	super(name, manager, "CIL_PROXY");
	this.angle = angle;
	this.mode  = mode;
	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	ROTATOR rotator = new ROTATOR(name);
	
	rotator.setMode(mode);
	rotator.setPosition(angle);
		
	command = rotator;

	switch (mode) {
	case ROTATOR.SKY:
	    modeName = "SKY "+Position.toDegrees(angle, 3);
	    break;
	case ROTATOR.MOUNT:
	    modeName = "MOUNT "+Position.toDegrees(angle, 3);
	    break;
	case ROTATOR.FLOAT:
	    modeName = "FLOAT";
	    break;
	default:
	    modeName = "rot-mode:"+mode;
	    break;
	}
	
	
    }
        
    @Override
	public void onInit() {
	super.onInit();	

	// Possible command reset.
	ROTATOR rotator = new ROTATOR(name);
	
	rotator.setMode(mode);
	rotator.setPosition(angle);		
	command = rotator;
	
	logger.log(1, CLASS, name, "onInit", 
		   "Starting Rotator setup to "+modeName+" at "+Position.toDegrees(angle, 3)+" degs");
    }
    
    @Override
	public void onDisposal() {
	super.onDisposal();
    }
    
    
    @Override
	public void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Rotator setup completed");   
    }
       
    /** Sets the Rotator angle (rads).*/
    public void setAngle(double angle) {
	this.angle = angle;
    }

}

/** $Log: RotatorTask.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


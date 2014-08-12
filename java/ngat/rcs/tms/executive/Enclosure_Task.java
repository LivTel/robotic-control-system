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
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** This Task sets the Telescope enclosure mode and positioning.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Enclosure_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Enclosure_Task.java,v $
 * </dl>
     * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Enclosure_Task extends Default_TaskImpl {

    protected int mechanism;

    protected int state;
    
    /** Create an Enclosure_Task using the supplied parameters.
     * @param mechanism The shutter / shutters to move.
     * @param state The state to move to { OPEN | CLOSE }.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public Enclosure_Task(String name,
			TaskManager manager,
			int mechanism,
			int state) {
	super(name, manager, "CIL_PROXY");
	this.mechanism = mechanism;
	this.state = state;
		
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	ENCLOSURE enclosure = new ENCLOSURE(name);	
	enclosure.setMechanism(mechanism);
	enclosure.setState(state);
		
	command = enclosure;
	
    }
        
    @Override
	public void onInit() {
	super.onInit();
	// If the mechanisms(s) is/are already in the required state set DONE and return here.
	//e.g.if (mechanism = ENCLOSURE.BOTH && TCS_Status.latest().mechanisms.enclosure1Status == CLOSED_STATE	
	logger.log(1, CLASS, name, "onInit", 
		   "Starting enclosure "+(state == ENCLOSURE.OPEN ? "OPEN" : "CLOSE"));
    }
        
    @Override
	public void onDisposal() {
	super.onDisposal();
    }
    
  
    @Override
	public void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Enclosure "+(state == ENCLOSURE.OPEN ? "OPEN" : "CLOSE")+" completed");
    }
       
}

/** $Log: Enclosure_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


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

/** This Task sets the TCS Operational state.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Operational_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Operational_Task.java,v $
 * </dl>
     * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Operational_Task extends Default_TaskImpl {

    protected int state;
    
    /** Create an Operational_Task using the supplied parameters.
     * @param state The state toswitch the TCS into { ON | OFF }.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public Operational_Task(String      name,
			    TaskManager manager,
			    int         state) {
	super(name, manager, "CIL_PROXY");
	this.state = state;
		
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	OPERATIONAL operational = new OPERATIONAL(name);
	operational.setState(state);
		
	command = operational;
	
    }
        
    @Override
	public void onInit() {
	super.onInit();
	
    }
        
    @Override
	public void onDisposal() {
	super.onDisposal();
    }
    
  
    @Override
	public void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
    }
       
}

/** $Log: Operational_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


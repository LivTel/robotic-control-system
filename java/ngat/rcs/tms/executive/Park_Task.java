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

import ngat.rcs.tms.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** This Task sendss the Telescope to its parking positioning.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Park_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Park_Task.java,v $
 * </dl>
     * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Park_Task extends Default_TaskImpl {

    protected int position;
    
    /** Create a Park_Task using the supplied parameters.
     * @param position The position to send the telescope to. 
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public Park_Task(String name,
			TaskManager manager,
			int position) {
	super(name, manager);
	this.position = position;
	
	try {
	    createConnection("CIL_PROXY");  
	    System.err.println("Made a connection object: "+connection);
	} catch (UnknownResourceException e) {
	    logger.log(1, "Park_Task", name, "Constructor", 
			 "Unable to establish connection to subsystem: CIL_PROXY: ");
	    failed = true;
	    errorIndicator.setErrorCode(CONNECTION_RESOURCE_ERROR);
	    errorIndicator.setErrorString("Creating connection: Unknown resource CIL_PROXY.");
	    errorIndicator.setException(e);
	    return;
	}
    	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	PARK park = new PARK(name);	
	park.setPosition(position);
		
	command = park;
	
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

/** $Log: Park_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


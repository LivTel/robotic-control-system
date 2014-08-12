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

/** This Task sets the Telescope Mirror Cover position..
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: MirrorCover_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/MirrorCover_Task.java,v $
 * </dl>
     * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class MirrorCover_Task extends Default_TaskImpl {

    protected int state;
    
    /** Create an MirrorCover_Task using the supplied parameters.
     * @param state The state to move to { OPEN | CLOSE }.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public MirrorCover_Task(String name,
			TaskManager manager,
			int state) {
	super(name, manager);
	this.state = state;
	
	try {
	    createConnection("CIL_PROXY");  
	    System.err.println("Made a connection object: "+connection);
	} catch (UnknownResourceException e) {
	    logger.log(1, "MirrorCover_Task", name, "Constructor", 
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

	MIRROR_COVER mirrorCover = new MIRROR_COVER(name);	
	mirrorCover.setState(state);
		
	command = mirrorCover;
	
    }
        
    @Override
	public void onInit() {
	super.onInit();
	logger.log(1, CLASS, name, "onInit", 
		   "Starting to "+(state == MIRROR_COVER.OPEN ? "open" : "close")+" the mirror cover");
    }
        
    @Override
	public void onDisposal() {
	super.onDisposal();
    }
    
  
    @Override
	public void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onInit", 
		   "Mirror cover "+(state == MIRROR_COVER.OPEN ? "open" : "close")+" completed");
	
    }
       
}

/** $Log: MirrorCover_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


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
package ngat.rcs.control;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.newstatemodel.*;
import ngat.rcs.ops.OperationsManager;
import ngat.net.*;
import ngat.message.GUI_RCS.*;

import java.util.*;

/** Grabs log messages back from RCS to remote GUI.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: GET_STATE_MODELImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/GET_STATE_MODELImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class GET_STATE_MODELImpl extends CtrlCommandImpl {

    public GET_STATE_MODELImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	GET_STATE_MODEL getm = (GET_STATE_MODEL)command;

	GET_STATE_MODEL_DONE done = new GET_STATE_MODEL_DONE(command.getId());
	
	//RCS_ControlTask ca = RCS_ControlTask.getInstance();
	
	//Map map = ca.getStateInfo();


	StandardStateModel tsm = RCS_Controller.controller.getTestStateModel();
	
	// variables
	Map map = tsm.getStateInfo();

	// current state
	int cs = tsm.getIntState();

	OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();

	// current op
	int cop = opsMgr.getOperationsState();

	if (map == null) {
	    sendError(done, GET_STATE_MODEL.NOT_AVAILABLE, "No state model info is currently available");
	    return;
	}

	//System.err.println("GET_STATE_MODEL:: Cs="+cs+", Cop="+cop+", vars="+map);

	done.setSuccessful(true);
	done.setCurrentState(cs);
	done.setCurrentOperation(cop);
	done.setVariables((HashMap)map);

	sendDone(done);

    }

}

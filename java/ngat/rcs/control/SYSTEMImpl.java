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
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

import java.io.*;

/**
 * Handles the SYSTEM ctrl command.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SYSTEMImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/SYSTEMImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SYSTEMImpl extends CtrlCommandImpl {

	public SYSTEMImpl(IConnection connection, GUI_TO_RCS command) {
		super(connection, command);
	}

	public void handleRequest() {

		SYSTEM sys = (SYSTEM) command;

		SYSTEM_DONE done = new SYSTEM_DONE(command.getId());

		int level = sys.getLevel();

		String opStr = "";

		switch (level) {

		case SYSTEM.RESTART_ENGINEERING:
			opStr = "OP_RESTART_ENG";
			//ca.systemControlExec(RCS_Controller.RESTART_ENGINEERING,
			 //PowerDownTask.INST_NO_ACTION);
			break;
		case SYSTEM.RESTART_AUTOMATIC:
			opStr = "OP_RESTART_AUTO";
			// ca.systemControlExec(RCS_Controller.RESTART_ROBOTIC,
			// PowerDownTask.INST_NO_ACTION);
			break;
		case SYSTEM.HALT:
			opStr = "OP_RCS_REBOOT";
			// ca.systemControlExec(RCS_Controller.HALT,
			// PowerDownTask.INST_NO_ACTION);
			break;
		// add extras for instrument options
		default:
			// Unknown option !
			sendError(done, SWITCH_MODE.NOT_IMPLEMENTED, "No such action known: " + level);
			return;
		}

		EventQueue.postEvent(opStr);

		done.setSuccessful(true);
		done.setErrorString("Implementing: " + opStr);
		sendDone(done);

	}

}

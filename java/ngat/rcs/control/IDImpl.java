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
import ngat.rcs.ops.OperationsManager;
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
 * Handles the ID ctrl command.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: IDImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/IDImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class IDImpl extends CtrlCommandImpl {

	public IDImpl(IConnection connection, GUI_TO_RCS command) {
		super(connection, command);
	}

	public void handleRequest() {

		ID mid = (ID) command;

		ID_DONE done = new ID_DONE(command.getId());

		done.setControl(ID.RCS_PROCESS);

		// done.setOperational(RCS_Controller.controller.isOperational());

		// weird, we set the RCW mode from the reverse of the automatic state of
		// the RCS !
		done.setEngineering(!RCS_Controller.controller.isAutomatic());

		done.setUptime(System.currentTimeMillis() - RCS_Controller.controller.getRunStartTime());

		// Find out from OpsMgr which MCA is current
		OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();

		DefaultModalTask mca = opsMgr.getCurrentModeController();

		if (mca != null) {
			String mcaName = mca.getAgentDesc();
			if (mcaName != null)
				done.setAgentName(mca.getAgentDesc());
			else
				done.setAgentName("None");

			String mcaId = mca.getAgentId();
			if (mcaId != null)
				done.setAgentInControl(mcaId);
			else
				done.setAgentInControl("None");
		} else {
			done.setAgentName("Idle");
			done.setAgentInControl("Idle");
		}
		done.setSuccessful(true);
		sendDone(done);

	}

}

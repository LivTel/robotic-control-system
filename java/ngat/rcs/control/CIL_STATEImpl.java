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
import java.util.*;

/** Stes or retrieves the CIL status.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: CIL_STATEImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/CIL_STATEImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class CIL_STATEImpl extends CtrlCommandImpl {

    public CIL_STATEImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	CIL_STATE cs = (CIL_STATE)command;

	//System.err.println("Recieved CIL State request: "+cs);

	CIL_STATE_DONE done = new CIL_STATE_DONE(command.getId());

	switch (cs.getOption()) {
	case CIL_STATE.BIND:
	    try {
		JCIL.getInstance().doRestart();
	    } catch (IOException iox) {
		sendError(done, CIL_STATE.BIND_ERROR, "Unable to restart: "+iox);
		return;
	    }
	    break;
	case CIL_STATE.RELEASE:
	    try {
		JCIL.getInstance().doClose();
	    } catch (IOException iox) {		
		sendError(done, CIL_STATE.RELEASE_ERROR, "Unable to release CIL port: "+iox);
		return;
	    }
	    break;
	case CIL_STATE.SHOW:
	    done.setActive(JCIL.getInstance().isActive());
	    done.setHost(JCIL.getInstance().getHost());
	    done.setSendPort(JCIL.getInstance().getSendPort());
	    done.setDestPort(JCIL.getInstance().getDestPort());
	    break;
	}
	
	done.setSuccessful(true);
	sendDone(done);
    }
    
}

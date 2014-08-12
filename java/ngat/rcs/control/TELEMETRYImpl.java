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

/** OPens Telemetry connection back to remote GUI.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TELEMETRYImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/TELEMETRYImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class TELEMETRYImpl extends CtrlCommandImpl {

    public TELEMETRYImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	TELEMETRY telreq = (TELEMETRY)command; 
	
	TELEMETRY_DONE done = new TELEMETRY_DONE(command.getId());
	
	Telemetry telemetry = Telemetry.getInstance();

	String clientId = telreq.getClientId();

	ConnectionSetupInfo conset = telreq.getConnect();

	Vector wants = telreq.getWants();

	System.err.println("Ctrl:TelReq::Attempting to setup telemetry connection for: "+clientId+" using: "+conset+" Wants: "+wants);
	
	try {
	    telemetry.addConnection(clientId, conset, wants);
	} catch (IOException iox) {
	    System.err.println("Error setting up Telemetry responder for : "+conset); 
	    sendError(done, 777, "(Temp code) Error setting up Telemetry to: "+conset+" : "+iox);
	    return;	    
	}
	
	done.setSuccessful(true);
	sendDone(done);
	
    	// Thats it  - Telemetry will start sending info messages back asap !

    }
    

}

/** $Log: TELEMETRYImpl.java,v $
/** Revision 1.1  2006/12/12 08:26:29  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:59  snf
/** Initial revision
/** */

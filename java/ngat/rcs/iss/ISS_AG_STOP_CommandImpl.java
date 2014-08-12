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
package ngat.rcs.iss;

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
import ngat.rcs.control.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;

public class ISS_AG_STOP_CommandImpl extends ISS_CommandImpl {

    public static final String CLASS = "ISS_AG_STOP_CommandImpl";

    private static boolean overrideForwarding = true;

    public ISS_AG_STOP_CommandImpl(JMSMA_ProtocolServerImpl serverImpl,
				    COMMAND receivedCommand) {
	super(serverImpl, receivedCommand);
    }

    /** Override forwarding for this command.*/
    public static void setOverrideForwarding(boolean ovr) {overrideForwarding = ovr;}

    @Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
	issLog.log(2, CLASS, "-", "processReceivedCommand",
		   "Received command: "+receivedCommand);			
    }

    @Override
	public long calculateTimeToComplete() { return 20000L; }

    @Override
	public boolean doesForward() { 

	// 31-aug-07 snf The ISS no longer does any AGSTOP forwarding - this will be re-enabled with 
	// standalone ISS when implemented but not in this code.
	return false;

// 	// Check if command forwarding to TCS has been overridden.
//         if (overrideForwarding)
//             return false;
	
// 	return true; 
    }
    
    @Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) { 
	issLog.log(2, CLASS, "-", "translateCommand",
		   "Translating command");
	AUTOGUIDE ag = new AUTOGUIDE(command.getId());
	ag.setState(AUTOGUIDE.OFF);
	return ag;
    }

    @Override
	public void processResponse(COMMAND_DONE response) {
	issLog.log(2, CLASS, "-", "processResponse",
		   "Received response: "+response);		   
    }
    
    @Override
	public COMMAND_DONE makeResponse() {
	
	AG_STOP_DONE done = new AG_STOP_DONE(receivedCommand.getId());
	done.setSuccessful(true);
	done.setErrorNum(0);
	done.setErrorString("Pretending to send AUTOGUIDE OFF");
	issLog.log(2, CLASS, "-", "makeResponse",
		   "Pretending to send AUTOGUIDE OFF");
	return done;

    }

    @Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) { 
	issLog.log(2, CLASS, "-", "translateResponse",
		   "Translating response: "+response);
	AG_STOP_DONE done = new AG_STOP_DONE(receivedCommand.getId());
	done.setSuccessful(response.getSuccessful());
	done.setErrorNum(response.getErrorNum());
	done.setErrorString(response.getErrorString());
	return done;
    }
    
}

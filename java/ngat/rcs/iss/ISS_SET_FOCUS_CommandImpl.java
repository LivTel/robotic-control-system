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

public class ISS_SET_FOCUS_CommandImpl extends ISS_CommandImpl {

    public ISS_SET_FOCUS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl,
				    COMMAND receivedCommand) {
	super(serverImpl, receivedCommand);
    }

    @Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
	System.out.println("ISS_SET_FOCUS_Impl: received command: "+
			   receivedCommand.getId());
    }

    @Override
	public long calculateTimeToComplete() { return 20000L; }

    @Override
	public boolean doesForward() { return true; }
    
    @Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) { 
	System.out.println("ISS_SET_FOCUS_Impl: translating command:");
	ngat.message.RCS_TCS.FOCUS ff = new ngat.message.RCS_TCS. FOCUS(command.getId());
	ff.setFocus(((ngat.message.ISS_INST.SET_FOCUS)command).getFocus());
	return ff;
    }

    @Override
	public void processResponse(COMMAND_DONE response) {
	System.out.println("ISS_SET_FOCUS_Impl: received response: "+
			   response.getId());
    }
    
    @Override
	public COMMAND_DONE makeResponse() {
	return null;
    }

    @Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) { 
	System.out.println("ISS_SET_FOCUS_Impl: translating response: "+response.getClass().getName());
	SET_FOCUS_DONE done = new SET_FOCUS_DONE(receivedCommand.getId());
	done.setSuccessful(response.getSuccessful());
	done.setErrorNum(response.getErrorNum());
	done.setErrorString(response.getErrorString());
	return done;
    }
    
}

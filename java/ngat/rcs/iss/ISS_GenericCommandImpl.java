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

public class ISS_GenericCommandImpl extends ISS_CommandImpl {


    public ISS_GenericCommandImpl(JMSMA_ProtocolServerImpl serverImpl,
				  COMMAND receivedCommand) {
	super(serverImpl, receivedCommand);
	done = null;
    }

    @Override
	public long calculateTimeToComplete() { return 20000L; }

    @Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
	issLog.log(2, CLASS, "-", "processReceivedCommand",
		   "Received command: "+receivedCommand);
    }
    
    @Override
	public boolean doesForward() { return false; }
 
    @Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) { return null;}

    @Override
	public void processResponse(COMMAND_DONE response) {}

    @Override
	public COMMAND_DONE makeResponse() {


	if (receivedCommand instanceof AG_START) {
	    done = new ngat.message.ISS_INST.AG_START_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof AG_STOP ) {
	    done = new ngat.message.ISS_INST.AG_STOP_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof ngat.message.ISS_INST.MOVE_FOLD) {
	    done = new ngat.message.ISS_INST.MOVE_FOLD_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof OFFSET_RA_DEC) {
	    done = new ngat.message.ISS_INST.OFFSET_RA_DEC_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof OFFSET_ROTATOR) {
	    done = new ngat.message.ISS_INST.OFFSET_ROTATOR_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof SET_FOCUS) {
	    done = new ngat.message.ISS_INST.SET_FOCUS_DONE(receivedCommand.getId());
	}
	if (receivedCommand instanceof OFFSET_FOCUS) {
	    done = new ngat.message.ISS_INST.OFFSET_FOCUS_DONE(receivedCommand.getId());
	}

	issLog.log(2, CLASS, "-", "makeResponse",
		   "Built response: "+done);
	done.setSuccessful(true);
	done.setErrorNum(0);
	done.setErrorString("");
	return done;
    }
    
    @Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) { return response;}


}

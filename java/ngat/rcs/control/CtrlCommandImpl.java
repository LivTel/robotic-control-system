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
import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

import java.io.*;

/**
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: CtrlCommandImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/control/RCS/CtrlCommandImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public abstract class CtrlCommandImpl implements CAMPRequestHandler {

	IConnection connection;

	GUI_TO_RCS command;

	public CtrlCommandImpl(IConnection connection, GUI_TO_RCS command) {
		this.connection = connection;
		this.command = command;
	}

	public long getHandlingTime() {
		return 0L;
	}

	public void dispose() {
		System.err.println("CtrlHandler: "+
				this.getClass().getName()+" : "+
				(connection != null ? connection.toString() : "NullConn")+
				(command != null ? command.getId() : "NullCmd")+
				" Closing connection...");
		
		if (connection != null) {			
			connection.close();
		}
		connection = null;
		command = null;
	}
	
	

	/** Sends a done message back to client. Breaks conection if any IO errors. */
	protected void sendDone(GUI_TO_RCS_DONE done) {
		try {
			connection.send(done);
			dispose();
		} catch (IOException iox) {
			System.err.println("Error sending done: " + iox);
			dispose();
		}
	}

	/** Sends an error message back to client. */
	protected void sendError(GUI_TO_RCS_DONE done, int errNo, String errMsg) {
		done.setErrorNum(errNo);
		done.setErrorString(errMsg);
		sendDone(done);
	}

}

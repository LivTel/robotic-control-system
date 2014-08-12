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

/** Grabs log messages back from RCS to remote GUI.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: GRAB_LOGImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/GRAB_LOGImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class GRAB_LOGImpl extends CtrlCommandImpl {

    public GRAB_LOGImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	GRAB_LOG grab = (GRAB_LOG)command;

	GRAB_LOG_DONE done = new GRAB_LOG_DONE(command.getId());

	// Do we see the logger.

	String logName = grab.getLogName();

	if (logName == null) {
	    sendError(done, GRAB_LOG.NO_SUCH_LOGGER, "No logger specified");
	    return;
	}

	int level = grab.getLevel();

	if (level < -1) {
	    sendError(done, GRAB_LOG.INVALID_LEVEL, "Not a valid log level: "+level);
	    return;
	}

	Logger logger = LogManager.getLogger(logName);
	System.err.println("GRAB: Found logger: "+logName);

	TelemetryLogHandler handler = new TelemetryLogHandler(logName, new BasicLogFormatter(150));
	handler.setLogLevel(level);
	
	logger.addHandler(handler);
	System.err.println("GRAB_LOG: Added handler: "+handler);
	
	// Thats it  - the handler will start sending log messages back asap !
	
    }
    

}

/** $Log: GRAB_LOGImpl.java,v $
/** Revision 1.1  2006/12/12 08:26:29  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:59  snf
/** Initial revision
/** */

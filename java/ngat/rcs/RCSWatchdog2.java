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
package ngat.rcs;

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
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.io.*;
import java.util.*;
import java.text.*;

import ngat.util.*;
import ngat.util.logging.*;
import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

/** RCSWatchdog is responsible for starting and stopping the RCS.
 * It provides a server to allow external clients to start the RCS, once the RCS is
 * running it takes over the server's responsibility to allow clients to stop, 
 * restart, reboot etc.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: RCSWatchdog2.java,v 1.1 2006/12/12 08:25:35 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/RCS/RCSWatchdog2.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RCSWatchdog2 {

    public static final int DEFAULT_PORT   = 9120;

    public static final int DEFAULT_STATUS = 0;

    /** Watchdog control server.*/
    private CAMPServer server;

    /** Control server port.*/
    private int port;

    /** Status of previous execution.*/
    private int status;

    /** Create a Watchdog(2) using parameters:
     * @param port   The port to attach to.
     * @param status The init status.
     */
    public RCSWatchdog2(int port, int status) {

	this.port   = port;
	this.status = status;

	// Create a CAMP server.
	server = new CAMPServer("RCW_SERVER");
	server.setRequestHandlerFactory(new HandlerFactory());
	try {
	    server.bind(port);
	    server.start();
	} catch (IOException iox) {
	    System.err.println("RCW or RCS are already running on port: "+port);
	    return;
	}

    } // (RCSWatchdog)


    public static void main(String args[]) {

	// java RCSWatchdog2 -port <port> -status <exit-status>

	CommandParser parser = new CommandParser();

	try {
	    parser.parse(args);
	} catch (ParseException px) {
	    System.err.println("RCW: Error parsing command args: "+px);
	    return;
	}

	ConfigurationProperties config = parser.getMap();

	System.err.println("Config: "+config);

	int port = config.getIntValue("port", DEFAULT_PORT);

	int status = config.getIntValue("status", DEFAULT_STATUS);

	RCSWatchdog2 wd2 = new RCSWatchdog2(port, status);
	
    }
 
    private class HandlerFactory implements CAMPRequestHandlerFactory {

	public CAMPRequestHandler createHandler(IConnection connection, COMMAND command) {

	    // Deal with undefined and illegal args.
	    if (connection == null) return null;
	    if ( (command == null)    || 
		 ! (command instanceof GUI_TO_RCS) ) return null;
	    
	    // Cast to correct subclass.
	    GUI_TO_RCS guicmd = (GUI_TO_RCS)command;

	    return new Handler(connection, guicmd);
	}

    } // [HandlerFactory]

    private class Handler extends CtrlCommandImpl {

	IConnection connection;

	GUI_TO_RCS command;

	Handler(IConnection connection, GUI_TO_RCS command) {
	    super(connection, command);
	    this.connection = connection;
	    this.command    = command;
	}

	public void handleRequest() {

	    if (command instanceof ID) {

		// Just return the RCW status.

		ID_DONE done = new ID_DONE(command.getId());
		done.setControl(ID.WATCHDOG_PROCESS);
		done.setEngineering(false);	
		done.setLastStatus(status); // this indicates the exit status on last RCW startup attempt of RCS
		done.setSuccessful(true);
		sendDone(done);

	    } else if
		(command instanceof START) {

		START start = (START)command;

		// Decides the RCS startup mode.
		if (start.getEngineering())
		    System.exit(21);
		else 
		    System.exit(22);
	

	    } else if
		(command instanceof SYSTEM) {

		SYSTEM sys = (SYSTEM)command;

		SYSTEM_DONE done = new SYSTEM_DONE(command.getId());
		
		int level = sys.getLevel();

		switch (level) {

		case SYSTEM.RESTART_ENGINEERING:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog2 does not do ENG Restart");
		    break;
		case SYSTEM.RESTART_AUTOMATIC:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog2 does not do AUTO Restart");		  
		    break;
		case SYSTEM.HALT:
		    System.exit(24);
		    break;
		case SYSTEM.REBOOT:
		    System.exit(23);
		    break;
		case SYSTEM.SHUTDOWN:
		    System.exit(25);
		    break;
		default:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog2 - Unknown system level: "+level);
		}

	    }

	}

	@Override
	public long getHandlingTime() {
	    return 0L;
	}

	@Override
	public void dispose() {
	    
	    

	}

    } // [Handler]
    

} // [RCSWatchdog]

/** $Log: RCSWatchdog2.java,v $
/** Revision 1.1  2006/12/12 08:25:35  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:29:55  snf
/** Initial revision
/** */

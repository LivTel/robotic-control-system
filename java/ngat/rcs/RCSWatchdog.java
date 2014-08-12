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
 * <dd>$Id: RCSWatchdog.java,v 1.1 2006/12/12 08:25:35 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/RCS/RCSWatchdog.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RCSWatchdog {

    public static final int DEFAULT_PORT = 9120;

    /** When set, indicates that RCS is started automatically when watchdog starts up.*/
    private boolean autoStart;
    
    /** When set, indicates that RCS should start in ENGINEERING mode.*/
    private boolean engineering;

    /** When set, indicates that RCW should exit and prompt a reboot.*/
    private boolean reboot;

    /** When set, indicates that RCW should exit and prompt a shutdown.*/
    private boolean shutdown;

    /** Indicates an error during startup of RCS.*/
    private int error;

    /** RCS eng startup command line.*/
    private String rcsEngCommand;

    /** RCS auto startup command line.*/
    private String rcsAutoCommand;

    /** Watchdog control server.*/
    private CAMPServer server;

    /** Execution thread.*/
    private Executor executor;

    /** Control server port.*/
    private int port;

    /** Delay after lock release before RCS is started (millis).*/
    private long delay;

    public RCSWatchdog() {

	rcsEngCommand = "/occ/bin/rcx eng";

	rcsAutoCommand = "/occ/bin/rcx auto";
	
	// Create a server.
	server = new CAMPServer("RCW_SERVER");
	server.setRequestHandlerFactory(new HandlerFactory());

	// Create an Exec thread but pause it immediately.
	executor = new Executor("RCW_EXEC");
	executor.linger();

    } // (RCSWatchdog)


    public static void main(String args[]) {

	RCSWatchdog wd = new RCSWatchdog();
	
	CommandParser parser = new CommandParser();

	try {
	    parser.parse(args);
	} catch (ParseException px) {
	    System.err.println("RCW: Error parsing command args: "+px);
	    return;
	}

	ConfigurationProperties config = parser.getMap();

	System.err.println("Config: "+config);

	int     port = config.getIntValue("port", DEFAULT_PORT);
	System.err.println("RCW: Found arg: port: "+port);

	boolean eng  = (config.getProperty("eng") != null);

	boolean auto = (config.getProperty("auto") != null);

	wd.setPort(port);
	wd.setEngineering(eng);
	wd.setAutoStart(auto);

	System.err.println("RCW:M Starting exec");
	wd.startExecutor();

	System.err.println("RCW:M Binding server on: "+port);
	try {
	    wd.bindServer();
	    wd.startServer();
	} catch (IOException iox) {
	    System.err.println("RCW:M or RCS are already running on port: "+port);
	    return;
	}

	// We are auto so let the Exec start the RCS in whatever mode.	

	if (auto) {
	    System.err.println("RCW:M Wakeup Exec");
	    wd.awakenExecutor();
	}

	System.err.println("RCW:M Done main");
	

    }
    
    private void setDelay(long delay) { this.delay = delay; }

    private void setPort(int port) { this.port = port; }
    
    private void setEngineering(boolean engineering) { this.engineering = engineering; }

    private void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    /** Bind the server to a port.*/
    protected void bindServer() throws IOException {
	 server.bind(port);
    }

    /** Pause the server - release port only.*/
    protected void pauseServer() throws IOException {
	server.linger();
	server.unbind();
	System.err.println("RCW:M Paused server");
    }

    /** Restart server - rebind and awaken.*/
    protected void restartServer() throws IOException {
	bindServer();
	server.awaken(); 
	System.err.println("RCW:M Restarted server");
    }

    /** Start server.*/
    protected void startServer() throws IOException {
	server.start(); 
	System.err.println("RCW:M Started server");
    }

    /** Release the lock on the Exec thread.*/
    protected void awakenExecutor() {
	executor.awaken(); 
	System.err.println("RCW:M Rewoke executor");
    }

    /** Start the execution thread.*/
    protected void startExecutor() {
	executor.start();
	System.err.println("RCW:M Started executor");
    }

    private class Executor extends ControlThread {

	Executor(String name) {
	    super(name, true);
	}

	@Override
	protected void initialise() {}

	@Override
	protected void mainTask() {

	    System.err.println("RCW:X Done waiting on lock");

	    // Wait a bit before starting RCS
	    try { Thread.sleep(delay);}catch (InterruptedException ix) {}

	    int exitCode = 0;

	    // We are paused here unless we got released after startup due to auto

	    System.err.println("RCW:X Pausing server");
	    try {
		pauseServer();
	    } catch (IOException iox) {
		System.err.println("RCW:X Failed to pause RCW Server - carrying on regardless: "+iox);
	    }

	    Runtime runtime = Runtime.getRuntime();

	    Process p  = null;

	    error = 0;

	    if (engineering) {
		try {
		    p = runtime.exec(rcsEngCommand);
		    System.err.println("RCW:X Started RCS with command: "+rcsEngCommand);
		} catch (IOException iox) {
		    System.err.println("RCW:X Error running RCS process: "+iox);
		    
		}
	    } else {
		try {
		    p = runtime.exec(rcsAutoCommand); 
		    System.err.println("RCW:X Started RCS with command: "+rcsAutoCommand);
		} catch (IOException iox) {
		    System.err.println("RCW:X Error running RCS process: "+iox);
		    
		}
	    }

	    System.err.println("RCW:X Waiting for RCS to finish");

	    try {
		p.waitFor();
	    } catch (InterruptedException ix) {
		System.err.println("RCW:X Interrupted waiting for RCS process to finish: "+ix);
	    }

	    exitCode = p.exitValue() + 605000;

	    System.err.println("RCW:X RCS exited with code: "+exitCode);

	    switch (exitCode) {
	    case RCS_Controller.RESTART_ENGINEERING: 
		// exit 21 Restart RCS with eng flag.
		engineering = true;
		System.err.println("RCW:X RCS exit RESTART_ENG");		
		break;
	    case RCS_Controller.RESTART_ROBOTIC: 
		// exit 22 Restart RCS with auto flag.
		engineering = false;
		System.err.println("RCW:X RCS exit RESTART_AUTO");
		break;
	    case RCS_Controller.HALT: 
		// exit 24 Dont restart RCS - 
		// i.e. linger waiting for GUI to restart us..
		linger();
		System.err.println("RCW:X RCS exit HALT");
		break;
	    case RCS_Controller.REBOOT: 
		// exit 23 Exit with reboot code.
		reboot = true;		
		System.err.println("RCW:X RCS exit REBOOT");	
		break;
	    case RCS_Controller.SHUTDOWN:
		// exit 25 Exit with shutdown code.
		shutdown = true;		
		System.err.println("RCW:X RCS exit SHUTDOWN");
		break;
	    default:
		// Some error code from 1-19. Dont restart RCS - 
		// i.e. linger waiting for GUI to reconfig and restart us..
		error       = exitCode;	
		linger();
		engineering = false;
	    }

	    if (reboot || shutdown) {
		terminate();
		return;
	    }

	    System.err.println("RCW:X Waiting on lock");

	    try {
		restartServer();
	    } catch (IOException iox) {
		System.err.println("RCW:X Failed to restart RCW Server - carrying on regardless: "+iox);
	    }

	}

	@Override
	protected void shutdown() {

	    if (reboot)
		System.exit(23);
	    
	    if (shutdown)
		System.exit(25);
	    
	}

    } // [Executor]

   
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
		done.setEngineering(engineering);	
		done.setLastStatus(error); // this indicates the exit status on last RCW startup attempt of RCS
		done.setSuccessful(true);
		sendDone(done);

	    } else if
		(command instanceof START) {

		START start = (START)command;

		START_DONE done = new START_DONE(command.getId());

		// Stop the server, set flags, unblock exec thread - it will start the RCS

		try {
		    pauseServer();
		} catch (IOException iox) {
		    sendError(done, START.WATCHDOG_ERROR, "Unable to pause RCW Server: "+iox);
		    return;		    
		}

		// Decides the RCS startup mode.
		engineering = start.getEngineering();		
	
		awakenExecutor();
		
	
		done.setSuccessful(true);
		sendDone(done);

	    } else if
		(command instanceof SYSTEM) {

		SYSTEM sys = (SYSTEM)command;

		SYSTEM_DONE done = new SYSTEM_DONE(command.getId());
		
		int level = sys.getLevel();

		switch (level) {

		case SYSTEM.RESTART_ENGINEERING:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog does not do Restarts");
		    break;
		case SYSTEM.RESTART_AUTOMATIC:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog does not do Restarts");		  
		    break;
		case SYSTEM.HALT:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Watchdog does not do Halt at present");		  
		    break;
		case SYSTEM.REBOOT:
		    System.exit(23);
		    break;
		case SYSTEM.SHUTDOWN:
		    System.exit(25);
		    break;
		default:
		    sendError(done, SYSTEM.NOT_AVAILABLE, "Unknown sys level: "+level);
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

/** $Log: RCSWatchdog.java,v $
/** Revision 1.1  2006/12/12 08:25:35  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:29:55  snf
/** Initial revision
/** */

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
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.ops.OperationsManager;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.iss.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

/** Temporary Text interface RCS Command server. Users may telnet to the
 * speciified port and type in commands to control the RCS. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: RCS_CommandServer.java,v 1.4 2008/04/10 07:52:31 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/RCS_CommandServer.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.4 $
 */
public class RCS_CommandServer extends ControlThread implements Logging {

    static SimpleDateFormat     sdf = new SimpleDateFormat("yyyy-MMM-dd 'T' HH:mm:ss z");
    static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    static final String CLASS = "RCS_CommandServer";

    CommandParser parser;

    ServerSocket serverSocket;

    Socket clientSocket;

    String command;
    String lastCommand = "";

    int port;

    String id;

    int connectCount;

    Date date;

    // Logging.
    Logger traceLog;

    Logger errorLog;

    /** Create a RCS_CommandServer using specified settings.*/
    public RCS_CommandServer(String id, int port) {
	super(id, true);
	this.id   = id;
	this.port = port;
	traceLog = LogManager.getLogger(TRACE);
	errorLog = LogManager.getLogger(ERROR);
	sdf.setTimeZone(UTC);

    }

    /** Setup the server.*/
    @Override
	public void initialise() {
	try {
	    serverSocket = new ServerSocket(port);
	    traceLog.log(INFO, 5, CLASS, id, "init", "Opened server socket on port: "+port);
	} catch (IOException iox) {
	    errorLog.log(ERROR, 5, CLASS, id, "init", "Starting server: "+iox); 
	}
    }

    /** Run the server.*/
    @Override
	public void mainTask() {
	Socket clientSocket = null;
	
	// Listen for Client connections. Timeout regularly to check for termination signal.
	while (canRun() && !isInterrupted()) {  
	    try {
		clientSocket = serverSocket.accept();
		traceLog.log(INFO, 5, CLASS, id, "mainTask",
			     "Client attached from: " + clientSocket.getInetAddress()+
			     " port: " + clientSocket.getPort());
	    } catch (InterruptedIOException iie1) {
		// Socket timed-out so try again
	    } catch (IOException ie3) {
		errorLog.log(ERROR, 1, CLASS, id, "mainTask", "Error connecting to client");
		clientSocket = null;
	    }
	    if (clientSocket != null) break; // got a connection.
	}
	// 
	if (clientSocket != null) {
	    traceLog.log(INFO, 5, CLASS, id, "mainTask",
			 "Creating Connection Thread for Client at: ["+
			 clientSocket.getInetAddress() + "] on local port: " + 
			 clientSocket.getLocalPort());
	    CommandConnectionThread connectionThread = new CommandConnectionThread(clientSocket);
	    
	    if (connectionThread == null) {
		errorLog.log(ERROR, 1, CLASS, id, "mainTask", 
			     "Error generating CommandConnection Thread for client: ");
	    } else {
		traceLog.log(INFO, 5, CLASS, id, "mainTask", 
			     "Starting CommandConnection Thread for client connection: ");
		connectionThread.start();
	    }
	}
	
    }

    /** Release resources.*/
    @Override
	public void shutdown() {
	
    }
    
    private class CommandConnectionThread extends ControlThread {
	
	/** Socket associated with current client connection. */
	protected Socket clientSocket;
	
	/** Input stream from client.*/
	BufferedReader cin;
	
	/** Output stream to client.*/
	PrintStream cout;

	String CLASS;

	String prompt = "\nRCS>>";

	boolean connected = true;
    
	long sessionStart;


	/** Create a CommandConnction.*/
	CommandConnectionThread(Socket clientSocket) {	    
	    super("COMMAND_CONNECT#", false);
	    setName("COMMAND_CONNECT#"+(connectCount++));
	    this.clientSocket = clientSocket;	 
	    CLASS = RCS_CommandServer.CLASS+".CommandConnectionThread";
	    sessionStart = System.currentTimeMillis();
	} // (Constructor).
	
	/** Set up I/O streams between here and client. */
	@Override
	protected void initialise() {
	    
	    // Make connections to Client.
	    try {
		//clientSocket.setSoTimeout(20000);
		clientSocket.setTcpNoDelay(true);   // send small packets immediately.
		clientSocket.setSoLinger(true, 600); // give up and close after 5mins.
		cin = new BufferedReader(new InputStreamReader((clientSocket.getInputStream())));
		traceLog.log(INFO, 5, CLASS, getName(), "init",
			     "Opened INPUT stream from Client: " + clientSocket.getInetAddress()+":"+
			     clientSocket.getPort()+" timeout: "+clientSocket.getSoTimeout()+" secs.");	   
	    } catch (IOException ie1) {
		errorLog.log(ERROR, 1, CLASS, getName(), "init",
			     "Error opening input stream from Client: " + 
			     clientSocket.getInetAddress()+
			     " : "+clientSocket.getPort());
		errorLog.dumpStack(2, ie1);
		terminate(); // cant send Error message as connect failed. Client will see IOException.
	    }
	    
	    try {
		cout = new PrintStream(clientSocket.getOutputStream());
		traceLog.log(INFO, 5, CLASS, getName(), "init",
			     "Opened OUTPUT stream to Client: " + clientSocket.getInetAddress()+" : "+
			     clientSocket.getPort()+" and flushed header:");
	    } catch (IOException ie2) {
		errorLog.log(ERROR, 5, CLASS, getName(), "init",
			     "Error opening output stream to Client: " + clientSocket.getInetAddress() +
			     ":" + clientSocket.getPort());
		errorLog.dumpStack(2, ie2);
		terminate(); // cant send Error message as connect failed. Client will see IOException.
	    }		    
	    
	} // (initialise).
	
	@Override
	protected void mainTask() {
	    
	    command = null;
	    String reply   = null;
	    
	    // 0. Send the initial reply.
	    InetAddress client = clientSocket.getInetAddress();
	    String      cAddr  = client.getHostAddress();
	    String      cName  = client.getHostName();
	    String      user   = "";
	    	   
	    user = "User @ "+cAddr;
	    	    
	    reply = initialReply(user);
	    cout.print(reply+prompt);
	    traceLog.log(INFO, 5, CLASS, getName(), "mainTask",
			 "Sending response to Client:");
	    
	    // Loop reading commands and processing.
	    while (connected) {
		
		// 1. Read the data from the Client.
		try {
		    command = cin.readLine();
		    if (command == null) break;
		    
		    traceLog.log(INFO, 5, CLASS, getName(), "init", 
				 "Read Command from Client: "+command);		
		} catch (IOException ie3) {
		    errorLog.log(ERROR, 1, CLASS, getName(), "init", 
				 "Error reading request from Client - IOError: "+ie3);
		    errorLog.dumpStack(2, ie3);
		    terminate();
		}
				
		date = new Date();
		
		// 2. Process.
		if (command.startsWith(".")) {
		    reply = processCommand(lastCommand);
		} else {
		    reply = processCommand(command);
		    lastCommand = command;
		}
		
		// 3. Reply to Client. 
		cout.print(reply+prompt);
		traceLog.log(INFO, 5, CLASS, getName(), "mainTask",
			     "Sending response to Client:");
	    }
	    
	    try {
		cout.close();
		cin.close();
		clientSocket.close();
	    } catch (IOException iox) {

	    }
	} //  main().
	
	
	@Override
	protected void shutdown() {
	    traceLog.log(INFO, 3, CLASS, getName(), "mainTask", "Shutting down now.");
	} // shutdown().
	
	private String initialReply(String user) {
	    return "RCS Command Server: "+id+
		"\n"+sdf.format(new Date())+
		"\nType help for a list of commands."+
		"\nHello "+user;
	}

    
    public String processCommand(String command) {
	
	command = command.trim();
	
	ConfigurationProperties argMap = new ConfigurationProperties();
	
	StringTokenizer st = new StringTokenizer(command);
	
	if (st.countTokens() > 1) {
				
	    // Assemble tokens into an arglist for parsing.
	    String[] args = new String[st.countTokens() - 1];
	    
	    // Skip first - its the command.
	    st.nextToken();
		
	    int i = 0;
	    while (st.hasMoreTokens()) {			    
		args[i] = st.nextToken();	
		i++;	    
	    }
	    
	    // Now Parse them.
	    CommandParser parser = new CommandParser();
	    
	    try { 
		parser.parse(args); 
	    } catch (ParseException px) {
		return "Error parsing args: "+command;
	    }
	    
	    // Extract the arg-properties.
	    argMap = parser.getMap();
	    
	}
	
	if 
	    (command.startsWith("help")) {
	    return processHelp(argMap);
	} else if
	    (command.startsWith("spy")) {
	    return processSpy(argMap);
	} else if
	    (command.startsWith("bye")) {
	    connected = false;
	    return processBye(argMap, sessionStart);
	} else if
	    (command.startsWith("status")) {
	    return processStatus(argMap);	   
	} else if
	    (command.startsWith("log")) {
	    return processLog(argMap);	
	} else if
	    (command.startsWith("iss")) {
	    return processIss(argMap);	
	} else if
	    (command.startsWith("pause")) {
	    return processPause(argMap);
	} else if
	    (command.startsWith("resume")) {
	    return processResume(argMap);
	} else if
	    (command.startsWith("monitor")) {
	    return processMonitor(argMap);
	} else if
	    (command.startsWith("handler")) {
	    return processHandler(argMap);
	} else if
	    (command.startsWith("fire") ||
	     command.startsWith("send") ||
	     command.startsWith("post")) {
	    return processFire(argMap);
	} else if
	    (command.startsWith("exec")) {
	    return processExec(argMap);
	} else if
	    (command.startsWith("go")) {
	    return processGo(argMap);
	} else if
	    (command.startsWith("show")) {
	    return processShow(argMap);
	} else if
	    (command.startsWith("op")) {
	    return processOp(argMap);
	} else if
	    (command.startsWith("ping")) {
	    return processPing(argMap);
	} else if
	    (command.startsWith("threads")) {
	    return processThreads(argMap);
	} else if
	    (command.startsWith("time")) {
	    return processTime(argMap);
	} else if
	    (command.startsWith("cil")) {
	    return processCil(argMap);
	} else if
	    (command.startsWith("see")) {
	    return processSee(argMap);
	} else if
	    (command.startsWith("task")) {
	    return processTask(argMap);
	} else if
	    (command.startsWith("inst")) {
	    return processInst(argMap);
	} else if
		(command.startsWith("sys")) {
		return processSysCommand(argMap);
	} else
	    return defaultMessage();
	
    }
	
	
	private String processHelp(ConfigurationProperties argMap) {
	    
	String cmd = null;

	if (argMap != null)		
	    cmd = argMap.getProperty("c");
	
	
	if (cmd == null) 		
	    return 
		"---------------------------------------------------------------------------"+
		"\nRCS Command list:"+ 
		"\n---------------------------------------------------------------------------"+
		"\n\n help      - Print this list of commands."+
		"\n\n             Extra info on any command"+
		"\n               can be obtained by typing:"+
		"\n                help -c <command>"+	
		"\n"+	     
		"\n\n status    - Print RCS status info."+
		"\n\n threads   - display information on running threads."+
		"\n\n monitor   - Display monitor threads"+
		"\n\n pause     - Pause a running monitor or server thread."+
		"\n\n resume    - Restart a paused monitor or server thread."+	
		"\n\n kill      - Kills a running monitor or server thread."+
		"\n\n log       - Set logging levels, handlers or -show"+
		"\n\n handler   - Set up log handlers."+
		"\n\n fire      - Trigger a simulated event (also 'post' and 'send'."+
		"\n\n op        - Force a start or stop option."+
		"\n\n go        - Switch operating mode."+
		"\n\n snapshot  - Print current Task hierarchy"+
		"\n\n spy       - Set watch parameters for an object."+
		"\n\n iss       - Set ISS parameters."+
		"\n---------------------------------------------------------------------------";
	else {
		 
	    if (cmd.startsWith("go"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage:"+
		    "\n\n go <state>"+
		    "\n\n Where state is any of:"+
		    "\n\n  - operational      : OPERATIONAL (nominally nighttime operations)."+
		    "\n\n  - standby          : STANDBY state (nominally daytime operations)."+
		    "\n---------------------------------------------------------------------------";			
	    else if 
		(cmd.startsWith("exec"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage:"+
		    "\n\n exec -script <script-name> [-after <delay-secs>]"+
		    "\n\n where: script-name is the name of a script (below RCS dir)."+
		    "\n\n delay-secs is a dely in secs before executing."+
		    "\n---------------------------------------------------------------------------";
	    else if 
		(cmd.startsWith("show"))
		return  
		    "---------------------------------------------------------------------------"+
		    "\nUsage: show [ -c <cat> [ -k <key> | -t | -age ] | -all | "+
		    "\n"+
		    "\nParameters:"+
		    "\n           cat    Status category (e.g. METEO, MECHANISM, STATE).."+
		    "\n           key    Key for status entry within category."+
		    "\n           t      Timestamp (yyyy-MM-dd T HH:mm:ss z)."+
		    "\n           age    Age of timestamp (sec)."+
		    "\n           all    Indicates that a list of categories should be returned."+
		    "\n"+
		    "\nExamples:"+
		    "\n"+
		    "\nshow -c METEO"+
		    "\n Returns the full meteorology data."+
		    "\n"+
		    "\nshow -c MECHANISM -k altitude.position"+
		    "\n Returns mechanism info on the current altitude position."+
		    "\n---------------------------------------------------------------------------";      
	    else if 
		(cmd.startsWith("cil"))
		return
		    "---------------------------------------------------------------------------"+
		    "\nUsage: cil [stop | restart | setlog <level> | show ]"+
		    "\n"+
		    "\n---------------------------------------------------------------------------";
		 
	    else if 
		(cmd.startsWith("iss"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: iss [-tag <tagId>] [-user <userId] [-prop <propId>] [-group <gId>] [-obs <obsId>]"+
		    "\n"+
		    "\nParameters:"+
		    "\n"+
		    "\n Sets various FITS header fields in manual observing mode."+
		    "\n---------------------------------------------------------------------------";      
	    else if
		(cmd.startsWith("tmm"))
		return
		    "---------------------------------------------------------------------------"+
		    "\nUsage: tmm <directive>"+
		    "\n"+
		    "\nDirectives:"+
		    "\n"+
		    "\n           -resched  : Reload the schedule."+
		    "\n           -recovery : Reload the recovery information."+
		    "\n---------------------------------------------------------------------------"; 
	    else if 
		(cmd.startsWith("pause"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage:"+
		    "\n\n pause <thread>"+
		    "\n\n Where thread is any of:"+
		    "\n\n  - SMM<n> with n in [0, 10] : TCS Status Monitor#n"+
		    "\n    SMM -all : ALL TCS Status Monitor#n"+
		    "\n\n  - RAT : The RATCAM CCS Status Monitor."+
		    "\n\n  - SUPIR : The SUPIRCAM CCS  Status Monitor."+
		    "\n\n  - MES :   The MES Spectrometer SCS Status Monitor."+
		    "\n\n  - NUVIEW : The NUVIEW II Spectrometer SCS Status Monitor."+
		    "\n---------------------------------------------------------------------------";
	    else if
		(cmd.startsWith("resume"))		     
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: resume -t <thread> [-level <level>] [-int <interval>] [-to <timeout>]"+
		    "\n\n level : The status level (if applicable)."+
		    "\n\n int   : The status request interval (secs)."+
		    "\n\n to    : Request timeout period (secs)."+
		    "\n\n Where thread is any of:"+
		    "\n\n  - SMM<n> with n in [0, 10] : TCS Status Monitor#n"+
		    "\n\n  - RAT : The RATCAM CCS Status Monitor."+
		    "\n\n  - SUPIR : The SUPIRCAM CCS Status Monitor."+
		    "\n\n  - MES :   The MES Spectrometer SCS Status Monitor."+
		    "\n\n  - NUVIEW : The NUVIEW II Spectrometer SCS Status Monitor."+
		    "\n---------------------------------------------------------------------------";		
	    else if
		(cmd.startsWith("log"))		    
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: log [ options ]"+
		    "\n"+
		    "\nOptions: "+	
		    "\n            [ -n[ame] <name> ] [ -off             ]	"+	   
		    "\n            [ -all           ] [ -full            ]"+
		    "\n                          	   [ -show            ]"+
		    "\n                               [ -l[evel] <level> ]"+
		    "\n                               [ -add <handler>   ]"+
		    "\n                               [ -rem <handler>   ]"+
		    "\n"+
		    "\nParameters: "+
		    "\n		name		The name of the logger to select."+
		    "\n		all		Indicates to select ALL current loggers."+	
		    "\n		level		The level of logging to set on the selected logger(s)."+
		    "\n				Any of: ALL, FULL, OFF, 0-99."+
		    "\n		handler	Name of a currently registered handler."+
		    "\n				(Use 'list -handlers' to obtain a list of handlers)."+
		    "\n"+
		    "\nDirectives:"+	
		    "\n		-show		Display data on selected logger(s)."+
		    "\n		-level	Set the log level of the selected logger(s)."+
		    "\n		-full		Indicates that selected logger(s) should have log-level"+
		    "\n				set to FULL. (Equivalent to 'log -level FULL')."+
		    "\n		-off		Indicates that selected logger(s) should have log-level"+
		    "\n				set to OFF. (Equivalent to 'log -level OFF')."+
		    "\n		-add		Add the named handler to the selected logger(s)."+
		    "\n		-rem 		Remove the named handler from the selected logger(s)"+
		    "\n				if possible."+
		    "\n"+
		    "\nExamples:"+
		    "\n"+
		    "\nlog -show -all 	"+	
		    "\n Display details of ALL loggers."+
		    "\n"+
		    "\nlog -level 5 -n ERROR 	"+
		    "\n Set the log-level 0ff ERROR log to 5"+
		    "\n"+
		    "\nlog -name TRACE -add SYS_CON:2"+
		    "\n Add the handler SYS_CON:2 to TRACE logger."+
		    "\n---------------------------------------------------------------------------";
		 
	    else if
		(cmd.startsWith("handler"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: handler -name <name> -type <type> -fmt <formatter> <options>"+
		    "\n"+
		    "\n Parameters:"+
		    "\n             type            Any of: CONSOLE, MCAST, FILE, (SOCKET), (SYSLOG)."+
		    "\n             formatter       Any of: SIMPLE, PLAIN, BOG, HTML, CSV."+
		    "\n             name            A unique ID for this handler."+ 
		    "\n"+
		    "\n Additional options (per type) include:"+
		    "\n\n type CONSOLE (no extra options)"+
		    "\n\n type FILE"+
		    "\n   -file <filepath> : full pathname of the logging file."+
		    "\n   -limit <rec-limit> : max no of records to log per file."+
		    "\n   -start <file-start> : label to append to first rotating log file."+
		    "\n   -end <file-end> : label to append to last rotating log file."+
		    "\n   -append : if included, open files in append mode (not overwrite)."+
		    "\n\n type MCAST"+
		    "\n\n -addr <address> : multicast group address."+
		    "\n\n -port <port> : port number to use."+
		    "\n---------------------------------------------------------------------------";	
	    else if
		(cmd.startsWith("fire")) 
		// fire -e <event-name> [-o <option>] [-l <level>] [-timed [-a <after-secs>] [-b <before-secs>]]
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: send -e <event-code> [-o <optional-data>] | -l <level> ] [ time-spec] | -show"+
		    "\n"+
		    "\n Parameters:"+
		    "\n             event-code       Any valid event label."+
		    "\n             optional-data    An extra piece of information."+
		    "\n             level            One of: DEFAULT, PRIORITY."+
		    "\n"+
		    "\n             time-spec = -timed [ -a <after> ] [ -b <before> ]"+
		    "\n"+
		    "\n             after            Will be posted after number of seconds."+
		    "\n             before           Will be posted within number of seconds."+
		    "\n"+
		    "\n Directives:"+
		    "\n"+
		    "\n             -show            List all available event codes."+
		    "\n---------------------------------------------------------------------------";
	    else if
		(cmd.startsWith("op"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: op -a <action>"+
		    "\n\n Where action is one of:"+
		    "\n   restart-robotic : Stop current invokation and let watchdog restart in AUTO mode."+
		    "\n   restart-eng     : Stop current invokation and let watchdog restart in ENG mode."+
		    "\n   reboot          : Reboot the OCC system."+
		    "\n   stop            : Stop watchdog and reboot ICS."+
		    "\n   halt            : Shutdown OCC system."+
		    "\n   shutdown        : Shutdown OCC system and all ICS."+
		    "\n---------------------------------------------------------------------------";

	    else if
		(cmd.startsWith("spy"))
		return 
		    "---------------------------------------------------------------------------"+
		    "\nUsage: spy [  -off                              ] [ -sensor <sensor-name>   ]"+
		    "\n           [  -on  [-l <level>] -log <logname>  ] [ -filter <filter-name>   ]"+          
		    "\n           [                                    ] [ -monitor <monitor-name> ]"+ 	  
		    "\n"+
		    "\nExamples:"+
		    "\n"+
		    "\nspy -off -sensor SS_RAIN"+
		    "\n  Switch spy logging off for rain sensor."+
		    "\n"+
		    "\nspy -on -filter F_WIND_SPEED -log WEATHER -l 5"+
		    "\n  Switch on spy logging of windspeed filter at level 5 to WEATHER log."+
		    "\n---------------------------------------------------------------------------"; 

	    else
		return "Help is not available for "+cmd+" or no such command";
	}
    }
	
    private String processBye(ConfigurationProperties argMap, long sessionStart) {
	    
	// bye

	double sd = (System.currentTimeMillis() - sessionStart)/1000.0;
	return "Bye, session lasted "+((int)sd)+" seconds.";
    }
	
    private String processTime(ConfigurationProperties argMap) {	
	    
	// time
	    
	return RCS_Controller.getObsDate().toString();
    }

	private String processInst(ConfigurationProperties argMap) {
		return "Depreciated";
	/*    // Inst
	    String instId  = argMap.getProperty("use");
	    
	    if (instId == null ||
		instId.equals(""))
		return "No instrument specified";

	    Instrument inst = Instruments.findInstrument(instId);

	    if (inst == null)
		return "Unknown instrument: "+instId;

	    double rotcorr = inst.getRotatorAlignmentCorrection();
	    FITS_HeaderInfo.setRotatorSkyCorrection(rotcorr);
	    
	    return "OK using "+instId+" with correction "+Position.toDegrees(rotcorr,2);*/
	    
	}
	
    private String processPing(ConfigurationProperties argMap) {
	    
	// ping
	    
	return "Depreciated";
    }

    private String processThreads(ConfigurationProperties argMap) {

	// threads

	Tabulator table = new Tabulator("Thread Statii.", new int[] {25, 30, 12});
	table.putLine(new String[] { "ID", "Name", "State"} );
	table.hline('-');

	Map map = RCS_Controller.controller.getThreadRegistry();
	Iterator it = map.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();		
	    ControlThread t = (ControlThread)map.get(name);
	    String state = "UNKNOWN";
		
	    if (t.isAlive()) {
		state = (t.isPaused() ? "PAUSED" : "RUNNING");
	    } else {
		state = "DEAD";
	    }
	    table.putLine(new String[] {name, t.getName(), state});			    
	    table.hline('-');
	}
	    
	return table.getBuffer();
    }

    private String processCil(ConfigurationProperties argMap) {
	    
	// cil [ -stop | -restart | -show | setlog <level> ]
	    
	if (argMap == null)		
	    return "cil: (args): No args specified";
	    
	boolean show    = (argMap.getProperty("show")    != null);
	boolean restart = (argMap.getProperty("restart")    != null);
	boolean stop    = (argMap.getProperty("stop") != null);

	int setlog = argMap.getIntValue("setlog", -1);

	if (show) {
	    return "CIL: "+(JCIL.getInstance().isActive() ? "Active" : "Inactive")+", From: "+JCIL.getInstance().getSendPort()+
		", To: "+JCIL.getInstance().getHost()+
		" : "+JCIL.getInstance().getDestPort();
	}

	if (stop) {
	    try {
		JCIL.getInstance().doClose();
		return "CIL stopped";
	    } catch (IOException iox) {
		return "CIL:: Unable to stop: "+iox;
	    }
	}

	if (restart) {
	    try {
		JCIL.getInstance().doRestart();
		return "CIL restarted";
	    } catch (IOException iox) {
		return "CIL:: Unable to restart: "+iox;
	    }
	}

	if (setlog != -1) {
	    //JCIL.getInstance().setRecvLogLevel(setlog);
	    return "CIL:: Recv log level unable to set yet: ";
	}

	return "CIL:: Unknown action";

    } 
	
    private String processSee(ConfigurationProperties argMap) {
	    
	// see [ -update <seeing> | -predict ]
	    
	if (argMap == null)		
	    return "see: (args): No args specified";
	    
	boolean doupdate = (argMap.getProperty("update") != null);
	    
	boolean show =  (argMap.getProperty("predict")    != null);
	    
	long now = System.currentTimeMillis();

	
	
	return "see:: (args): No action";
	    
    }
	
    private String processIss(ConfigurationProperties argMap) {

	// iss [-tag <tag>] [-user <pi-user>] [-proposal <propId>] [-group <gid>] [-obs <obsId>]
	//     [-forward  ] <command> ] 
	//     [-simulate ]

	if (argMap == null)		
	    return "iss: (args): No args specified";
	    
	String tagName = argMap.getProperty("tag");
	if (tagName != null)
	    FITS_HeaderInfo.current_TAGID.setValue(tagName);

	String userName = argMap.getProperty("user");
	if (userName != null)
	    FITS_HeaderInfo.current_USERID.setValue(userName);
	    
	String propName = argMap.getProperty("prop");
	if (propName != null)
	    FITS_HeaderInfo.current_PROPID.setValue(propName);
	    
	String groupName = argMap.getProperty("group");
	if (groupName != null)
	    FITS_HeaderInfo.current_GROUPID.setValue(groupName);
	    
	String obsName = argMap.getProperty("obs");
	if (obsName != null)
	    FITS_HeaderInfo.current_OBSID.setValue(obsName);
	    	    
	String mode = argMap.getProperty("mode");
	if (mode != null) {
	    if (mode.equals("MANUAL")) {
		FITS_HeaderInfo.current_TELMODE.setValue(mode);
		FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_MANUAL);
	    } else if
		(mode.equals("AUTOMATIC")) {
		FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_AUTOMATIC);
	    }
	}

	String targId = argMap.getProperty("target");
	if (targId != null) {
	    FITS_HeaderInfo.current_OBJECT.setValue(targId);
	    FITS_HeaderInfo.current_CAT_NAME.setValue(targId);
	}

	boolean show = (argMap.getProperty("showmode") != null);

	if (show) {
		
	    return "Operating mode is: "+ FITS_HeaderInfo.toModeString(FITS_HeaderInfo.getTelMode())+
	    		" and TAG="+FITS_HeaderInfo.current_TAGID;
		
	}
	    
	String commandClassName = argMap.getProperty("forward");
	if (commandClassName != null) {

	    commandClassName = "ngat.message.ISS_INST."+commandClassName;

	    try {
		Class.forName(commandClassName);
	    } catch (ClassNotFoundException cx) {		    
		return "iss: (forward): Unknown command class: "+commandClassName;
	    }

	    ISS.getInstance().setDoForward(commandClassName, true);
		
	    return "ISS will forward all commands of class: "+commandClassName;
		
	}

	commandClassName = argMap.getProperty("fake");
	if (commandClassName != null) {

	    commandClassName = "ngat.message.ISS_INST."+commandClassName;

	    try {
		Class.forName(commandClassName);
	    } catch (ClassNotFoundException cx) {		    
		return "iss: (fake): Unknown command class: "+commandClassName;
	    }

	    ISS.getInstance().setDoForward(commandClassName, false);
		
	    return "ISS will fake replies to all commands of class: "+commandClassName;
		
	}

	    
	return "FITS headers updated";	    

    }
    
    private String processSysCommand(ConfigurationProperties argMap) {
    	if (argMap == null)		
    	    return "sys: (args): No args specified";
    	
    	if (argMap.getProperty("show") != null) {
    		String key = argMap.getProperty("show");
    		String value = System.getProperty(key);
    		return "System: "+key+" = "+value;
    	}
    	
    	if (argMap.getProperty("k") != null  && argMap.getProperty("v") != null) {
    		String key = argMap.getProperty("k");
    		String value = argMap.getProperty("v");
    		System.setProperty(key, value);
    		return "sys: set "+key+" = "+value;
    	}
    	
    	return "sys: unknown args";
    }
	
    /** Sets up (or disables) spying on one or more RCS objects.
     */
    private String processSpy(ConfigurationProperties argMap) {
	    
	// spy [ -off ] | [ -on <obj> -log <logname> 
	// object = [ -sensor <name> ] | [ -filter <name> ]  

	// e.g. spy -on -sensor SS_RAIN -log SP1 

	// e.g. spy -off -filter F_WIND

	if (argMap == null)		
	    return "spy: (args): No args specified";
	    
	boolean on = (argMap.getProperty("on") != null);

	boolean objsensor  = (argMap.getProperty("sensor")  != null);
	boolean objfilter  = (argMap.getProperty("filter")  != null);
	boolean objmonitor = (argMap.getProperty("monitor") != null);
	boolean objrule    = (argMap.getProperty("rule")    != null);
	boolean objrset    = (argMap.getProperty("ruleset")    != null);
	    

	// Its a Sensor.
	if (objsensor) {

	    String name = argMap.getProperty("sensor");
	    Sensor sensor = SensorsXX.getSensor(name);
		
	    if (sensor == null)
		return "spy: (args): No such sensor: "+name;

	    if (on) {
		sensor.setSpy(true);
		String logname = argMap.getProperty("log");
		if (logname == null) 
		    return "spy: (args): No logname supplied";
		sensor.setSpyLog(logname);
		return "spy: Enabled spy-logging for sensor: "+name+" using: "+logname;
	    } else {
		sensor.setSpy(false);
		return "spy: Disabled spy-logging for sensor: "+name;
	    }

	}
	    
	// Its a Filter.
	if (objfilter) {

	    String name = argMap.getProperty("filter");
	    ngat.rcs.scm.detection.Filter filter = FiltersXX.getFilter(name);
		
	    if (filter == null)
		return "spy: (args): No such filter: "+name;

	    if (on) {
		filter.setSpy(true);
		String logname = argMap.getProperty("log");
		if (logname == null) 
		    return "spy: (args): No logname supplied";
		filter.setSpyLog(logname);
		return "spy: Enabled spy-logging for filter: "+name+" using: "+logname;
	    } else {
		filter.setSpy(false);
		return "spy: Disabled spy-logging for filter: "+name;
	    }

	}

	if (objmonitor) {
		
	    String name = argMap.getProperty("monitor");
		
	    Monitor monitor = MonitorsXX.getMonitor(name);
		
	    if (monitor == null)
		return "spy: (args): No such monitor: "+name;

	    if (on) {
		monitor.setSpy(true);
		String logname = argMap.getProperty("log");
		if (logname == null) 
		    return "spy: (args): No logname supplied";
		monitor.setSpyLog(logname);
		return "spy: Enabled spy-logging for monitor: "+name+" using: "+logname;
	    } else {
		monitor.setSpy(false);
		return "spy: Disabled spy-logging for monitor: "+name;
	    }
	}

	if (objrule) {
		
	    String name = argMap.getProperty("rule");
		
	    Rule rule = MonitorsXX.getRule(name);
		
	    if (rule == null)
		return "spy: (args): No such rule: "+name;

	    if (on) {
		rule.setSpy(true);
		String logname = argMap.getProperty("log");
		if (logname == null) 
		    return "spy: (args): No logname supplied";
		rule.setSpyLog(logname);
		return "spy: Enabled spy-logging for rule: "+name+" using: "+logname;
	    } else {
		rule.setSpy(false);
		return "spy: Disabled spy-logging for rule: "+name;
	    }
	}
	    
	return "spy: (args): No object specified";

    }

    /** Sends an event-code to the RCS event-queue.
     */
    private String processFire(ConfigurationProperties argMap) {
	    
	// fire -e <event-name> [-o <option>] [-l <level>] [-timed [-a <after-secs>] [-b <before-secs>]]

	if (argMap == null)		
	    return "fire: (args): No args specified";
	    
	String eventName = argMap.getProperty("e");
	if (eventName == null)
	    return "fire: (args): No event specified";
	   	   	   
	String option = argMap.getProperty("o");
	   	    
	int level = argMap.getIntValue("l", EventQueue.DEFAULT_LEVEL);

	boolean timed = (argMap.containsKey("timed"));
	    
	if (timed) {
	    long after  = argMap.getLongValue("a", 0L);
	    long before = argMap.getLongValue("b", 0L);
		
	    long now = System.currentTimeMillis();

	    if (after > 0)  after  = now + 1000*after;
	    if (before > 0) before = now + 1000*before;
		
	    // If either is null we use the 0 value = whenever...
		
	    EventQueue.postTimedEvent(eventName, option, after, before, level);
		
	    return "Posted Timed Event: ["+eventName+"]"+(option != null ? " with option ["+option+"]" : "");
	    
	} else {
		
	    if (option != null)		    
		EventQueue.postEvent(eventName, option, level);
	    else
		EventQueue.postEvent(eventName, level);
		
	    return "Posted Event: ["+eventName+"]"+(option != null ? " with option ["+option+"]" : "");
	}
    }

    private String processExec(ConfigurationProperties argMap) {

	// exec -script <script> | [-after <delay-secs>]

	if (argMap == null)	
	    return "exec: (args): No args specified";	

	String scriptName = argMap.getProperty("script");
	if (scriptName == null)
	    return "exec: (args): No script specified";

	// Open the script file.
	BufferedReader in = null;
	try {
	    in = new BufferedReader(new FileReader(scriptName));
	} catch (IOException e) {
	    return "exec: (!): Failed to open/read script file: "+e;
	}
	    
	// Read the commands and execute.
	try {
	    String line = null;
	    while ( (line = in.readLine()) != null) {
		command = line;
		String reply = processCommand(line); 		   
	    }
	}  catch (IOException e) {
	    return "exec: (!): Failed while running script: "+e;
	}
	return "Script "+scriptName+" done.";
    }

    private String processTask(ConfigurationProperties argMap) {

	// task -config -t <taskname>.

	if (argMap == null)	
	    return "task: (args): No args specified";

	String taskId = argMap.getProperty("t");
	if (taskId == null)
	    return "task: (args): Bo task identified";

	boolean doConfig = (argMap.getProperty("config") != null);

	TaskConfigRegistry reg = ParallelTaskImpl.getTaskConfigRegistry();
	if (reg == null)
	    return "task: (fatal): TaskConfigRegistry not found";

	ConfigurationProperties config = reg.getTaskConfig(taskId);

	if (config == null)
	    return "task: No config for ngat.rcs."+taskId;

	return "Task: ["+taskId+"] - "+config;	
	
    }

    private String processShow(ConfigurationProperties argMap) {
	    
	// show -c <cat> [-k <key>]  

	if (argMap == null)	
	    return "show: (args): No args specified";

	boolean all = (argMap.getProperty("all") != null);

	String  cat = null;
	String  key = null;

	boolean tim = (argMap.getProperty("t") != null);

	boolean age = (argMap.getProperty("age") != null);

	if (! all) {
		
	    cat = argMap.getProperty("c");
	    if (cat == null)
		return "show: (args): No status category supplied";
	
	}

	long now = System.currentTimeMillis();

	LegacyStatusProviderRegistry emm = LegacyStatusProviderRegistry.getInstance();
	if (emm == null)
	    return "show: (!): EMM Registry not found";
	    
	if (all) {

	    StringBuffer buffer = new StringBuffer("[");

	    Iterator cats =  emm.listCategories();
		
	    while (cats.hasNext()) {
		    
		String acat = (String)cats.next();

		buffer.append(acat+" : ");

	    }
	    buffer.append("]");
		
	    return buffer.toString();

	} else {
	    StatusCategory grabber = null;
	    try {
		grabber = emm.getStatusCategory(cat);			
	    } catch (IllegalArgumentException iax) {
		return "show: (args): No such status category: "+cat;
	    }
		
	    if (tim) {
		return "Status: ["+cat+"] : TimeStamp: "+sdf.format(new Date(grabber.getTimeStamp()));
	    } else if 
		(age) {
		return "Status: ["+cat+"] : Age: "+(now - grabber.getTimeStamp())+" millis.";
	    } else {	
		key = argMap.getProperty("k");
		if (key != null) {
		    if (key.indexOf("_") != -1)
			key = key.replace('_', ' ');
		    try {
			return "Status: ["+cat+" : "+key+"] = "+grabber.getStatusEntryRaw(key);	
		    } catch (IllegalArgumentException iax) {
			return "show: (args): "+cat+" - Unknown key: "+key;
		    }
		} else {
		    System.err.println("Requested category: "+cat+" isa "+grabber.getClass().getName()+" -> "+grabber);
		    return grabber.toString();
		}
	    }
	}
    }
	
    private String processGo(ConfigurationProperties argMap) {
	if (command.indexOf("operational") != -1) {
	    EventQueue.postEvent(RCS_Controller.START_NIGHT_OPS_COMMAND);
	    return "OPERATIONAL request posted.";
	} else if
	    (command.indexOf("standby") != -1) {
	    EventQueue.postEvent(RCS_Controller.START_DAY_OPS_COMMAND);
	    return "STANDBY request posted."; 
	} else 
	    return "Unknown state change option.";
    }

    private String processOp(ConfigurationProperties argMap) {

	// op [-quit] | 
 
	if (argMap == null)	
	    return "op: (args): No args supplied";
	  	    
	if (argMap.getProperty("restart") != null) {				
	    System.exit(21);
	}
	    
	if (argMap.getProperty("robotic") != null) {				
	    System.exit(22);
	}
	    
	if (argMap.getProperty("reboot") != null) {				
	    System.exit(23);
	}
  
	if (argMap.getProperty("halt") != null) {				
	    System.exit(24);
	}
	if (argMap.getProperty("shutdown") != null) {				
	    System.exit(25);
	}

	int code = argMap.getIntValue("code", 24);

	System.exit(code);
	    
	    
	//     if
	//  		(command.indexOf("standby") != -1) {
	//  		EventQueue.postEvent(RCS_Controller.START_DAY_OPS_COMMAND);
	//  		return "OP_STANDBY request posted.";
	//  	    } else if
	//  		(command.indexOf("operational") != -1) {
	//  		EventQueue.postEvent(RCS_Controller.START_NIGHT_OPS_COMMAND);
	//  		return "OP_OPERATIONAL request posted.";	    
	//  	    } else if
	//  		(command.indexOf("restart-robotic") != -1) {
	//  		EventQueue.postEvent("OP_"+RCS_ControlTask.OPERATOR_RESTART_ROBOTIC_COMMAND);
	//  		return "RESTART_ROBOTIC request posted.";
	//  	    } else if
	//  		(command.indexOf("restart-eng") != -1) {
	//  		EventQueue.postEvent("OP_"+RCS_ControlTask.OPERATOR_RESTART_ENGINEERING_COMMAND);
	//  		return "RESTART_ENGINEERING request posted.";
	//  	    } else if
	//  		(command.indexOf("reboot") != -1) {
	//  		EventQueue.postEvent("OP_"+RCS_ControlTask.OPERATOR_REBOOT_COMMAND);
	//  		return "REBOOT request posted.";	   
	//  	    } else if
	//  		(command.indexOf("halt") != -1) {
	//  		EventQueue.postEvent("OP_"+RCS_ControlTask.OPERATOR_HALT_COMMAND);
	//  		return "HALT request posted.";
	//  	    } else if
	//  		(command.indexOf("shutdown") != -1) {
	//  		EventQueue.postEvent("OP_"+RCS_ControlTask.OPERATOR_SHUTDOWN_COMMAND);	
	//  		return "SHUTDOWN request posted.";
	//  	    } else 
	//  		return "Unknown operation.";
	    
	return "op: (args): ?";

    }
	
    private String processStatus(ConfigurationProperties argMap) {

	// status [-rcs] | [-sm] | [-hist] | [-inst <inst-name>] | [-agent <agentID>]
 
	if (argMap == null)	
	    return "status: (args): No args supplied";
	    
	boolean rcs = (argMap.getProperty("rcs") != null);

	boolean sm  = (argMap.getProperty("sm") != null);

	boolean hist = (argMap.getProperty("hist") != null);

	String instName = argMap.getProperty("inst");
	    
	String agentId  = argMap.getProperty("agent");
	    
	//RCS_ControlTask ctrlAgent = RCS_ControlTask.getInstance();
 
	OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();
	
	Task cma = opsMgr.getCurrentModeController();
			
	
	String cmaDesc = null;
	if (cma != null)
	    cmaDesc = ((ModalTask)cma).getAgentDesc();
	    
	// RCS.
	if (rcs) {
	    RCS_Controller controller = RCS_Controller.controller;
		
	    Date date = new Date(RCS_Controller.startupTime);
	    int  time = (int)(System.currentTimeMillis() - RCS_Controller.startupTime)/1000;	       
	    int  m = time/60;
	    int  s = time - 60*m;
	    return 
		"-------------------------------------------------------------"+
		"\nRobotic Control System: "+controller.getRcsId()+" : Boot "+controller.getRunCounter()+
		"\n\nTelescope:  "+controller.getTelescopeDesc()+" ("+controller.getTelescopeId()+")"+
		"\n\nSite:       "+controller.getTelescopeLocation()+
		"\n\nStarted on: "+sdf.format(date)+" Up: "+m+"M"+" "+s+"S"+
		"\n\nStatus:     "+(controller.isOperational() ? "OPERATIONAL"+(cma == null ? "" : "/AIC"+cmaDesc) 
				    : "NOT_OPERATIONAL")+
		"\n-------------------------------------------------------------";
	}
	


	// StateModel. DEFUNCT
	if (sm)
	  return "Depreciated";
	  
	 /*return  ctrlAgent.stateToString();		    	 
	}*/
	    
	// Instruments. DEFUNCT
	if (argMap.containsKey("inst")) {
		return "Depreciated";
	  /*  if (instName == null)
		return "status: (args): No instrument ID supplied";
		
	    if (Instruments.findInstrument(instName) != null) {
		return CCSPool.getInstance(instName).latest().toString();	
	    } else 
		return "status: (args): No such instrument: "+instName;*/
	}
	    
	// MC Agents.
	if (argMap.containsKey("agent")) {
	    if (agentId == null)
		return "status: (args): No mode control agent ID supplied";
		
	    if (TaskOperations.getInstance().getAgent(agentId) != null) {
		return TaskOperations.getInstance().getAgent(agentId).toString();
	    } else
		return "status: (args): No such mode control agent : "+agentId;
	}
	    
	return "status: (args): No args supplied";
	    
    }
	
   /* private String processKill(ConfigurationProperties argMap) {
	EventQueue.postEvent(RCS_ControlTask.OPERATOR_SHUTDOWN_COMMAND);
	return "SHUTDOWN signal sent:";
    }*/
	
    private String defaultMessage() {
	return "Unknown command OR not implemented at present.";
    }

    private String processStart(ConfigurationProperties argMap) {
	   
	return "Command implementation temporarily removed";
    }
	
    private String processToop(ConfigurationProperties argMap) {
	if (command.equalsIgnoreCase("to_alert")) {
	    EventQueue.postEvent("TO_ALERT", "GRB");
	    EventQueue.postEvent("TO_ALERT", "GRB"); // Post 2 to kick RCS_CT
	    return "Gamma Burst Alert received: RCS switching to REACTIVE mode:"+
		"\n Continue sending position updates to Command Server on port 8221."+
		"\n#Contact TOSH Server on port 8410 with position updates:";
	} else if
	    (command.equalsIgnoreCase("to_clear")) {
	    EventQueue.postEvent("TO_CLEAR", "GRB");
	    EventQueue.postEvent("TO_CLEAR", "GRB");// Post 2 to kick RCS_CT
	    return "Gamma Burst All-Clear received: RCS reverting to PLANETARIUM mode:";
	} else if
	    (command.startsWith("to_mesg")) {		
	    StringTokenizer st = new StringTokenizer(command, " ");
	    if (st.countTokens() == 3) {
		st.nextToken();
		double RA = 0.0; 
		double dec = 0.0;
		try {
		    // to_alert 234.0 45.5 (degs)
		    RA  = Math.toRadians(Double.parseDouble(st.nextToken()));
		    dec = Math.toRadians(Double.parseDouble(st.nextToken()));
		} catch (NumberFormatException nfx) {
		    return "TO_MESG - could not parse arguments: "+nfx;
		}		   
		EventQueue.postEvent("TO_MESG", "ra="+RA+"&dec="+dec);
		Position target = new Position(RA, dec);
		return "Gamma Burst Position Update: Event Posted: "+
		    "\nCoordinates: RA: "+Position.toHMSString(RA)+" Dec: "+ Position.toDMSString(dec)+
		    "\nTarget: Altitude: "+ Position.toDegrees(target.getAltitude(), 3)+
		    " Azimuth: "+ Position.toDegrees(target.getAzimuth(), 3);
	    } else {
		return "GRB - missing parameters USE: grb-pos:<ra>:<dec> ";
	    }
	} else {
	    return "Unknown grb command variant: grb-alert OR grb-pos OR grb-clear.";
	}
    }

    // Display the state of the various status monitors.
    private String processMonitor(ConfigurationProperties argMap) {  
	Tabulator table = new Tabulator("Status Monitor Statii.", new int[] {10, 30, 12, 10, 10, 7});
	table.putLine(new String[] { "ID", "Name", "State", "Interval", "Timeout", "B/Off"} );
	table.hline('-');
	// Status Monitors.
	SMM_MonitorThread st = null;
	String state = "UNKNOWN";
	   
	for (int i = 0; i < 11; i++) {
	    st = SMM_Controller.getController().getMonitor(i);
	    if (st != null) {
		if (st.isEnabled()) {
		    if (st.isAlive()) {
			state = (st.isPaused() ? "PAUSED" : "RUNNING");
		    } else {
			state = "DEAD";
		    }	   
		} else {
		    state = "DISABLED";
		}
		table.putLine(new String[] {"TCS - "+i, 
					    SMM_Controller.nameof(i), 
					    state, 
					    ""+(st.getInterval()/1000.0),
					    ""+(st.getClientTimeout()/1000.0),
					    ""+(st.isBackingOff())});
		table.hline('-');
	    }
	}

/*	// ICS Status Monitors.
	CCS_StatusMonitorThread ism = null;
	state = "UNKNOWN";
	Iterator im = Instruments.findInstrumentSet();
	while (im.hasNext()) {
	    String id = (String)im.next();
	    ism = RCS_Controller.getCCSStatusMonitor(id);
	    if (ism != null) {
		if (ism.isAlive()) {
		    state = (ism.isPaused() ? "PAUSED" : "RUNNING");
		} else {
		    state = "DEAD";
		}
		table.putLine(new String[] {
		    id, 
		    ism.getName(), 
		    state, 
		    ""+(ism.getInterval()/1000.0),
		    ""+(ism.getClientTimeout()/1000.0)});
		table.hline('-');
	    }
	}*/
	
	table.hline('-');
	return table.getBuffer();
    }
	
    // Pause a Thread.
    private String processPause(ConfigurationProperties argMap) {

	// pause [ -all | -t <thread> | -status <smm> | -inst <inst> ]
	    
	if (argMap == null)	
	    return "pause: (args): No args specified";
  
	boolean thd = (argMap.getProperty("t") != null);
	boolean all = (argMap.getProperty("all") != null);
	boolean sms = (argMap.getProperty("status") != null);
	boolean ism = (argMap.getProperty("inst") != null);
	    
	// All.
	if (all) {
	    Iterator it = RCS_Controller.threadRegistry.entrySet().iterator();
	    while (it.hasNext()) {
		ControlThread ct = (ControlThread)it.next();
		ct.linger();		  
	    }  
	    return "Paused ALL registered threads at: "+sdf.format(date);
	}
	    
	// Thread.
	if (thd) {
	    String thread = argMap.getProperty("t");
	    if (thread == null)
		return "pause: (args): No target speciifed";
		
	    ControlThread ct = (ControlThread)RCS_Controller.threadRegistry.get(thread);
		
	    if (ct == null) 
		return "pause: (!): No such thread: "+thread;

	    ct.linger();
	    return "Paused "+ct.getDesc()+" at: "+sdf.format(date);

	}


	// Status Monitor.
	if (sms) {
	    String smtId = argMap.getProperty("status");
		
	    if (smtId.equalsIgnoreCase("all")) {
		SMM_Controller.getController().pauseAll();
		return "Paused ALL TCS Status monitors at: "+sdf.format(date); 
	    } else {
		    
		SMM_MonitorThread monitor = SMM_Controller.getController().getMonitor(smtId);

		if (monitor == null)
		    return "pause; (!): No such TCS Status monitor: "+smtId+" was found";				
		monitor.linger(); 
		return "Paused TCS Status monitor: "+smtId+" at: "+sdf.format(date); 	
	    }

	}

	//CCS_StatusMonitorThread getCCSStatusMonitor(

	// Inst  Monitor.
	if (ism) {
	    String instId = argMap.getProperty("inst");
		
	    //CCS_StatusMonitorThread monitor = RCS_Controller.getCCSStatusMonitor(instId);
	   // if (monitor == null)
		//return "pause; (!): No such ICS Status monitor: "+instId+" was found";				
	   // monitor.linger(); 
	    //return "Paused ICS Status monitor: "+instId+" at: "+sdf.format(date); 	
	    return "Not implemented";
	}

	return "pause: (args): Unknown target specification";  
    }
	
    // Resume a Thread.
    private String processResume(ConfigurationProperties argMap) {
	if (command.indexOf(" ") == -1) 
	    return "Resume what ?: Use: resume [-level <level>] -t <thread> -int <interval> -to <timeout>"; 
	String rest = command.substring(7);

	parser = new CommandParser();
	try {
	    parser.parse(rest);
	} catch (ParseException pex) {
	    return "Log: Error parsing args: "+pex;
	}
	ConfigurationProperties props = parser.getMap();
	// Ok there are some options.
	    
	int level    =  props.getIntValue("level", -1);

	int interval = props.getIntValue("int", -1);

	int timeout  = props.getIntValue("to", -1);
	    
	String tmon = props.getProperty("t");

	if (tmon != null)
	    tmon = tmon.trim();

	String imon = props.getProperty("i");
	    
	if (imon != null)
	    imon = imon.trim();

	if (tmon == null && imon == null)
	    return "Resume what ?: "+
		"Use: resume [-level <level>] [-t <thread> | -i <thread> ] -int <interval> -to <timeout>";	    
	    
	if (tmon != null) {		
	    SMM_MonitorThread monitor = SMM_Controller.getController().getMonitor(tmon);
	    if (monitor == null)
		return "Resume "+tmon+" - No such TCS monitor was found.";		
	    if (timeout != -1)
		monitor.setClientTimeout((long)timeout*1000);
	    if (interval != -1)
		monitor.setInterval((long)interval*1000);
	    monitor.awaken(); 
	    return "Resumed TCS-StatusMonitor "+tmon+" at: "+sdf.format(date); 		
	} 
	    
	if (imon != null) {
	    //  int no2 = 0;
	    //  		try {
	    //  		    no2 = Integer.parseInt(imon);
	    //  		} catch (NumberFormatException e){
	    //  		    return "Resume ISM: Number ?: Use: resume -i<n>";
	    //  		}
	    //  		if (no2 < 0 || no2 > 10)
	    //  		    return "Resume ISM: Number out of range [0, 10]";
	    //  		if (level != -1)
	    //  		    RCS_Controller.icsStatusMonitor[no2].setClientLevel(level);
	    //  		if (timeout != -1)
	    //  		    RCS_Controller.icsStatusMonitor[no2].setClientTimeout((long)timeout*1000);
	    //  		if (interval != -1)
	    //  		    RCS_Controller.icsStatusMonitor[no2].setInterval((long)interval*1000);
	    //  		RCS_Controller.icsStatusMonitor[no2].awaken(); 
	    //  		return "Resumed ICS-StatusMonitor "+no2+" at: "+sdf.format(date);
	} 
	return "Resume: Unknown thread "+imon+"/ "+tmon;	 
    }
	
    private String processLog(ConfigurationProperties argMap) {
	    
	// log -show | -name <log-name> [-l <level>] [-h <handler>]
	
	if (argMap == null)	
	    return "log: (args): No args specified";


	String reply = "";
	
	// Log-name.
	String logname = argMap.getProperty("name");
	if (logname != null) {
	    logger = LogManager.getLogger(logname);	    
	    reply = "Logger: "+logger.getName();
	} 
	
	// Show.
	if (argMap.getProperty("show") != null) {
	    reply = 
		"\n|-------------------------------------------------------------|"+
		"\n| Logging statistics:                                         |"+
		"\n|-------------------------------------------------------------|"+
		"\n| Name                        | Level | Handlers     | H_Level|"+
		"\n|-------------------------------------------------------------|";
	    Iterator it = LogManager.listLoggers();
	    Logger alog = null;
	    String nm = "";
	    String ll = "";
	    String hn = "";
	    String hs = "";
	    int    hl = 0;
	    while (it.hasNext()) {
		alog = (Logger)it.next();
		//reply += "\n"+alog.getName()+"\t"+alog.getLogLevel()+"\t";
		LogHandler[] hdlrs = alog.getHandlers();
		if ( hdlrs.length > 0) {
		    for (int i = 0; i < hdlrs.length; i++) {
			if (i != 0) {
			    nm = "";
			    ll = "";
			} else {
			    nm = alog.getName();
			    ll = ""+alog.getLogLevel();
			}
			hn = hdlrs[i].getName();
			hl = hdlrs[i].getLogLevel();
			switch (hl) {
			case -1:
			    hs = "ALL";
			    break;
			case 0:
			    hs = "OFF";
			    break;
			default:
			    hs = ""+hl;
			}
			//reply += hdlrs[i].getName()+"\t"+hdlrs[i].getLogLevel()+"\n\t\t";
			try {
			    reply += "\n"+StringUtilities.
				tabulate(
					 new String[] {"| "+nm, "| "+ll, "| "+hn, "| "+hs},
					 new int   [] {30, 8, 15, 9}
					 )+"|";
			} catch (IllegalArgumentException iax) {
			    reply += "**";
			}			    
		    }
		    
		    reply +=  "\n|-------------------------------------------------------------|";
		}
	    }
	    return reply;
	}
	
	// Level.
	try {
	    int level = argMap.getIntValue("l", OFF);
	    if (logger != null) {
		logger.setLogLevel(level);
		reply = reply + " Level: "+level;
	    }
	} catch (Exception e) {
	    reply = reply + "Error setting level: "+e;
	}
	
	// Handler.
	String hdlr = argMap.getProperty("handler");
	if (hdlr != null) {
	    LogHandler handler = LogManager.getHandler(hdlr);
	    if (handler != null) {
		logger.addHandler(handler);
		reply += " Added handler: "+handler.getName();
	    }
	}
	
	if (logger != null)
	    logger.log(1, "Logger Test");
	
	return reply;
    }
    

    
    private String processHandler(ConfigurationProperties argMap) {
	
	// handler -name <name> [-l <level>] [-f <fmt> ] [ <out> ]
	//
	//  out = [ -console | -file <file> | -mcast <addr>:<port>]
	//  fmt = [ bog | simple | plain | html | xml | csv ] 
	
	
	// e.g. handler -name SPY -l 3 -f CSV -file spylog 
	
	// e.g. handler -name X -l 1 -f BOG -mcast 202.223.1.2:6969
	
	if (argMap == null)	
	    return "handler: (args): No args specified";
	
	String reply = null;
	
	LogHandler handler = null;
	
	// Name.
	String name = argMap.getProperty("name");
	
	// See if its already there - can only set its loglevel.
	if (LogManager.getHandler(name) != null) {
	    handler = LogManager.getHandler(name);	    
	}
	
	// H-level.
	int level = argMap.getIntValue("l", -5); // default is to leave alone.
	if (level < -1) {
	    // Leave it
	    if (handler != null)
		return "handler: "+name+" exists";
	} else {	
	    if (handler != null) {
		handler.setLogLevel(level);
		return "handler: "+name+" reset log-level to: "+level;
	    }
	}

	// Formatter.
	LogFormatter formatter = null;
	    
	String fmt = argMap.getProperty("fmt", "bog");
	
	if (fmt.equalsIgnoreCase("simple"))
	    formatter = new SimpleLogFormatter();
	else if
	    (fmt.equalsIgnoreCase("plain"))
	    formatter = new PlainLogFormatter();
	else if
	    (fmt.equalsIgnoreCase("bog"))
	    formatter = new BogstanLogFormatter();
	else if
	    (fmt.equalsIgnoreCase("html"))
	    formatter = new HtmlLogFormatter();
	else if 
	    (fmt.equalsIgnoreCase("xml"))
	    formatter = new XmlLogFormatter();
	else if 
	    (fmt.equalsIgnoreCase("csv"))
	    formatter = new CsvLogFormatter();
	else
	    formatter = new SimpleLogFormatter();
	    
	   
	// Handler-type.
	boolean con   = (argMap.getProperty("console") != null);
	boolean file  = (argMap.getProperty("file") != null);
	boolean mcast = (argMap.getProperty("mcast") != null);

	if (con) {

	    // Console log.

	    handler = new ConsoleLogHandler(formatter);	
	    
	} else if 
	    (file) {
	    
	    // File log.
	    
	    String logfile = argMap.getProperty("file");
	    if (logfile == null) 
		return "handler: (args): No file specified for FileLogHandler";
	    
	    try {
		handler = new FileLogHandler(logfile, formatter, FileLogHandler.DAILY_ROTATION);	
		reply = "handler: Created File handler: "+logfile;
	    } catch (FileNotFoundException fx) {
		return "handler: (!): Unable to open logging file: "+fx;
	    }
	  
	} else if     	    
	    (mcast) {
	    	    
	    // Multicast.
	    
	    String mc = argMap.getProperty("mcast");

	    if (mc.indexOf(":") == -1)
		return "handler: (args): Illegal multicast specification: "+mc;
			    
	    String group    = mc.substring(0, mc.indexOf(":"));
	    String strport  = mc.substring(mc.indexOf(":"));

	    int port = 0;
	    try {
		port = Integer.parseInt(strport);
	    } catch (NumberFormatException nx) {
		return "handler: (args): Unable to resolve port: "+mc;
	    }

	    try {
		handler = new MulticastLogRelay(group, port);
		reply =  "handler: Created Multicast handler: "+name+" On Address: "+group+" Port: "+port;
	    } catch (IOException iox) {
		return "handler: (args): Unable to create MCast handler: "+iox;
	    }
	} else {

	    return "handler: (args): No handler class specified";

	}

	// Register handler.	   
	handler.setName(name);
	LogManager.registerHandler(handler);
	
	if (level >= -1) {
	    handler.setLogLevel(level);
	    reply += " HLog-level: "+level;
	}
	
	return reply;
    }
    
    } // [CommandConnectionThread].
    
}

/** $Log: RCS_CommandServer.java,v $
/** Revision 1.4  2008/04/10 07:52:31  snf
/** fixed bug in parser where missing args cause NPX
/**
/** Revision 1.3  2008/04/10 07:44:24  snf
/** added support for rotcorr
/**
/** Revision 1.2  2007/07/05 11:31:14  snf
/** checkin
/**
/** Revision 1.1  2006/12/12 08:26:29  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:59  snf
/** Initial revision
/**
    /** Revision 1.3  2002/09/16 09:38:28  snf
    /** *** empty log message ***
    /**
    /** Revision 1.2  2001/06/08 16:27:27  snf
    /** Added GRB_ALERT.
    /**
    /** Revision 1.1  2001/04/27 17:14:32  snf
    /** Initial revision
    /** */

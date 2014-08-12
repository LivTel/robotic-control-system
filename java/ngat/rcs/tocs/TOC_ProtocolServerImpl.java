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
package ngat.rcs.tocs;

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
import ngat.rcs.iss.*;
import ngat.rcs.calib.*;

import java.io.*;
import java.net.*;
import java.util.*;

import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.base.*;

/** Implementation of the JMS TOOP Control protocol at
 * the server end.
 * <br><br>
 * $Id: TOC_ProtocolServerImpl.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOC_ProtocolServerImpl implements ProtocolImpl, Logging {

    public static final long ADDON_SLEEP_TIME = 30000L;

    /** ERROR_BASE for this class.*/
    public static final int ERROR_BASE = 300;
    
    public static final int SERVER_TIMEOUT_ERROR        = 000301;

    public static final int SERVER_INITIALIZATION_ERROR = 000302;

    public static int count = 0;
    
    public final String CLASS = "TOC_ServerImpl";
    
    public String id;

    Logger logger;

    /** Handler factory - generates the appropriate RequestHandler for
     * a received command.*/
    RequestHandlerFactory hFactory;

    /** Stores the command received.*/
    String command;

    /** The abstract connection to use.*/
    IConnection connection;
    
    /** Handles the COMMAND recieved from the client.*/
    protected RequestHandler handler;

    /** Indicates that the command has completed or failed. i.e. the TCS
     * has sent either a <i>completed</i> or an <i>error</i> message.*/
    protected volatile boolean completed;
    
    /** Object to synch on.*/
    protected Object csynch;

    /** Records time when this protocol session is expected to complete.*/
    private volatile long timeOfCompletion;

    
    /** Create a TOC_ProtocolServerImpl using the supplied parameters.
     * @param hFactory  Generates an appropriate type of RequestHandler.
     * @param connection The abstract connection to use.*/
    public TOC_ProtocolServerImpl(IConnection connection, RequestHandlerFactory hFactory ) {
	this.connection = connection;
	this.hFactory = hFactory;	
	csynch = new Object();
	count++;
	id = ""+count;
	logger = LogManager.getLogger("TOCS");
    }
    
    
    /** The implementation. <br>
     * Notes: <ul>
     * <li> Returns silently if the connection dies
     * or the handler cannot be created.
     * <li> The handler's handleRequest() method should return either
     * immediately if it starts a ClientConnectionThread or before its
     * initially calculated tiomeout if it does processing.
     * </ul>
     * The handler (via a proxy) will have to keep sending ACKs back
     * to the client using the sendAck() method on this ServerImpl.
     *
     * <ol>
     *    <li>Get the command from the client.
     *    <li>Using RequestHandlerFactory, instantiate an appropriate handler.
     *    <li>Call handler's handleRequest().
     *    <li>Waiting for handler to complete. The execution thread
     *        is made to sleep for the timeout period. If the handler has spawned
     *        a client thread, then this may recieve ACKs and pass them on to
     *        here. The acked flag is reset on receipt of an ACK and another
     *        timeout period can be entered.
     * </ol>
     */
    public void implement() {

	// 1. Get the command from the client. If this fails then we 
	// try to send an error message to the client can't do much else.
	logger.log(1, CLASS, id, "impl",
		   "About to read command");
	try { 
	    command = (String)connection.receive();
	    logger.log(1,CLASS, id, "impl",
		       "Received command: "+command);
	} catch (IOException e) {
	    sendError("IO_READ", "Exception: "+e, command);
	    return;
	} catch (ClassCastException ce) {
	    sendError("GARBLED", "Exception: "+ce, command);
	    return;
	}  
	
	// 2. Create a handler.
	if (command == null) {
	    sendError("NO_COMMAND", "Nothing received", null);
	    return;
	}
	handler = hFactory.createHandler(this, command);
	if (handler == null) {
	    sendError("UNABLE_TO_PROCESS", "No implementation available", command);
	    return;
	}

	long startRequestTime = System.currentTimeMillis();

	// 4. Handle the request.
	handler.handleRequest();

	// 5.we may already have completed by here...
	if (! completed) {
	    // Not yet so wait for it to happen...
	    timeOfCompletion = System.currentTimeMillis() + handler.getHandlingTime();
	
	    while (!completed && (System.currentTimeMillis() < timeOfCompletion)) {
		long sleepTime = timeOfCompletion - System.currentTimeMillis() + ADDON_SLEEP_TIME;
		try {
		    Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
		}	
	    }
	
	    // 6. If not completed now then we've really timed out. Send an error to Client.
	    if (! completed() ) {
		logger.log(1, CLASS, id, "impl", 
			   "Timed out uncompleted");
	    	  
		sendError("TIMED_OUT", 
			  "Waiting for handler to complete after "+
			  (System.currentTimeMillis()-startRequestTime)+" millis", 
			  command);
	    }
	}
    }

    /** Send an ACK message to the client.*/
    public void sendAck(long timeout) {
	//try { 	   	
	//  if (connection != null)
	//connection.send("ACK");  
	//} catch (IOException e) {
	//  handler.exceptionOccurred(this,e);
	//}
	logger.log(1, CLASS, id, "sendAck",
		   "TOC_ProtocolSeverImpl: handleAck: "+timeout);
	timeOfCompletion += timeout;
	System.err.println("TOC_ProtocolServer: HandleAck: "+timeout+", TOC now: "+(new Date(timeOfCompletion)));
    }
      
    /** Send a reply to the client and finish execution thread.*/
    public void sendReply(String reply) {
	synchronized (this) {
	    try { 
		logger.log(1, CLASS, id, "impl", 
			   "Sending Reply");		
		if (connection != null)
		    connection.send(reply);  
	    } catch (IOException e) {
		handler.exceptionOccurred(this,e);
	    }
	    setCompleted();
	    if (handler != null)
		handler.dispose(); 
	    if (connection != null)
		connection.close();
	    connection = null;
	    command    = null;
	    hFactory   = null;
	}
    }
       
    /** Set the completion status to indicate that handler has finished.*/
    private void setCompleted() {
	synchronized(csynch) {
	    completed = true;
	}
    }
    
    /** @return True if the handler has completed processing.*/
    private boolean completed() {
	synchronized(csynch) {
	    return completed;
	}
    }
    
    /** Send a message to the client to indicate an error occurred
     * while reading or executing the command. 
     * @param code    An error code string.
     * @param message A message to send to the client.
     * @param command The command which initiated this message.*/
    protected void sendError(String code, String message, String command) {	
	String id = "";
	if (command != null)
	    id = command;
	else
	    id = "no_command";
	String reply = "ERROR "+code+" "+message+" Command: "+command;	
	sendReply(reply);
    }
  
    /** Forces the CCT-Thread to unblock by any suitable method.*/
    public void cancel() {
	connection.close();
    }

    /** Returns the ID for this implementor.*/
    public String getId() { return id;}
    
}

/** $Log: TOC_ProtocolServerImpl.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/09/03 14:54:07  snf
/** Initial revision
/** */

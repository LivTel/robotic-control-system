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
package ngat.rcs.scm.collation;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** Client which deals with sending SHOW status requests to the
 * TCS and deals with the returned status data. This is pushed into
 * the globally accessable TCS_Status_Pool and may then cause
 * monitoring threads to trigger events, completing the basic
 * feedback loop.
 * <br><br>
 * $Id: SMM_MonitorClient.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 */
public class SMM_MonitorClient extends JMSMA_ClientImpl {

    public static final String CLASS = "SMM_MonitorClient";


    /** Constant: Default capacity of the default SlotBuffer used
     * in the connection.*/
    public static final int DEFAULT_BUFFER_CAPACITY = 3;

    public static int count = 0;

    protected String id;

    /** The SHOW status segment key.*/
    protected int key;

    /** Create a SMM_MonitorClient using a SlotBufferConnection to
     * the CIL_Proxy_Server and the specified id. <b>This is the normal constructor</b>.
     */
    public SMM_MonitorClient(String id, int key) {
	this(new SlotConnection(CIL_Proxy_Server.getInstance(), DEFAULT_BUFFER_CAPACITY), key);
	this.id = id;
    }
       

    /** Create a SMM_MonitorClient using the supplied connection
     * and the SHOW key set to SHOW.ALL .
     * @param connection The connection to use.
     */
    public SMM_MonitorClient(IConnection connection) {
	this(connection, SHOW.ALL);
    }

    /** Create a SMM_MonitorClient using the supplied connection.
     * @param connection The connection to use.
     * @param key The SHOW key to use.
     */
    public SMM_MonitorClient(IConnection connection, int key) {
	super(connection);
	count++;
	this.key  = key;
	id        = "show-"+key+":"+count;
	piFactory = JMSMA_ProtocolImplFactory.getInstance();
	command   = new SHOW(id);
	((SHOW)command).setKey(key);
	logger = LogManager.getLogger("STATUS");
    }

    /** Sets the key for the SHOW command. Must be a valid SHOW keytype
     * as defined in ngat.message.RCS_TCS.SHOW.
     * @param key The SHOW command key to use.*/
    public void setKey(int key) {
	this.key = key;
	((SHOW)command).setKey(key);
    }

    /** Handle the ACK.*/
    public void handleAck(ACK ack) {	
    }

    /** Handle the response. Push the status into the globally accessable
     * TCS_Status_pool.*/
    public void handleDone(COMMAND_DONE done) {
	logger.log(3, CLASS, id, "handleDone",
		   "Received response: "+
		   "\nClass:           "+done.getClass().getName()+
		   "\nSuccess:         "+done.getSuccessful()+
		   "\nError code:      "+done.getErrorNum()+
		   "\nError message:   "+done.getErrorString());	

	// TEMP Could be something else !
	if (!done.getSuccessful()) {
	    TCS_Status.Network network = new TCS_Status.Network();
	    network.networkState = TCS_Status.STATE_ERROR;
	    StatusPool.insert(network);
	    return;
	}

	TCS_Status.Segment segment = ((SHOW_DONE)done).getStatus();
	
	// TNG REMOVED TEMPRARILY
	StatusPool.insert(segment);

	TCS_Status.Network network = new TCS_Status.Network();
	network.networkState = TCS_Status.STATE_OKAY;
	StatusPool.insert(network);
	logger.log(2, CLASS, id,  "handleDone",
		   "SMC::"+id+": Pushed status into pool: Timestamp: "+segment.timeStamp+
		   ", Class="+segment.getClass().getName());


    }
    
    /** Handle failure to connect.*/
    public void failedConnect(Exception e) {
	EventQueue.postEvent("CIL_PROXY_OFFLINE");
    }
    
    /** handle failure to despatch.*/
    public void failedDespatch(Exception e) {
	EventQueue.postEvent("CIL_PROXY_OFFLINE");
    }
    
    /** Handle failure to receive ACK.*/
    public void failedAck(Exception e) {
	EventQueue.postEvent("CIL_PROXY_OFFLINE");
    }
    
    /** Handle failure to receive Response - usually a timeout.*/
    public void failedResponse(Exception e) {
	 TCS_Status.Network network = new TCS_Status.Network();
	 network.networkState = TCS_Status.STATE_ERROR;
	 StatusPool.insert(network);
    }
    
    /** Overridden - does nothing.*/
    public void sendCommand(COMMAND command) {}
   
    /** Handle general failure of the client execution thread.*/
    public void exceptionOccurred(Object source, Exception e) {
	EventQueue.postEvent("EX_"+source+"_"+e);
	e.printStackTrace(System.err);
    }
  
}

/** $Log: SMM_MonitorClient.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/**
/** Revision 1.5  2001/06/08 16:27:27  snf
/** Added telfocus trapping info.
/**
/** Revision 1.4  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.3  2000/12/22 14:40:37  snf
/** Backup.
/**
/** Revision 1.2  2000/12/20 10:25:56  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */

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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import ngat.net.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

import java.util.*;

/** Carries out the implementation of a POS  command.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CommandImpl.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CommandImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public abstract class POS_CommandImpl implements RequestHandler, POS_CommandProcessor {

    public static final long   DEFAULT_HANDLING_OVERHEAD = 90000L;

    public static final double FUDGE = 1.2;

    /** The Queue level for this commandimpl/processor (may be 0 meaning not queued).*/
    protected int level;

    /** A logger to use for logging.*/
    protected static Logger pcaLog;

    /** The COMMAND which was received from the client. */
    protected POS_TO_RCS command;
    
    /** Stores the response for the client.*/
    protected POS_TO_RCS_DONE done;
    
    /** POS_Queue object to synch on.*/
    protected POS_Queue posQ;

    /** PlanetariumControlAgent.*/
    protected PlanetariumControlAgent pca;

    /** The ProtocolImpl which carries out the server function for and
     * which invoked this handler.*/
    protected JMSMA_ProtocolServerImpl serverImpl;
    
    /** A period at which the handler should timeout and tell the server
     * to send an ACK(timeout) to its client to keep it alive. The timeout 
     * should be set by the handler based on the actual command received
     * and any relevant telescope/TCS/RCS parameters which should allow the
     * handler to <i>estimate</i> the time the command will actually take.
     * Each POS_CI should set a variable <i>duration</i> which holds
     * the expected  (maximum) time for the task associated with the command
     * to take assuming it is executed immediately. E.g. for Exposures this
     * would include an allowance for slewing, inst-config, expose time etc.
     */
    protected long handlingTime;
    
   

    public POS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, POS_TO_RCS command) {
	this.serverImpl = serverImpl;
	this.command    = command;
	level           = getLevel();
	posQ = POS_Queue.getInstance();
	pca  = (PlanetariumControlAgent)PlanetariumControlAgent.getInstance();
	handlingTime    = getDuration();;	
	pcaLog = LogManager.getLogger("PCA");
    }
    
    /** Handle a command. 
     * If this PCI requires to be queued and the POS_Queue will accept requests at the
     * level specified for this handler then the command will be pushed onto the POS_Queue
     * at the appropriate level and this handler registered for the completion and error
     * callbacks. If the queue will not accept it at this time then an error message is 
     * sent back to the client. If this request does not require to be queued it will 
     * be processed here immediately.*/
    public void handleRequest() {
	
	
	if (enqueue()) {

	    // ENQUEUED Commands are move or processing

	    synchronized (posQ) {
		if (posQ.accept()) {  

		    // PCA is up.
		    
		    // Check if the RTOC is allowed to control at this time.
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "Request from RTOC ["+command.getControllerAddress()+"] Checking Authorization.");
		    
		    // Is client known to us.
		    if (!pca.isAuthorized(command.getControllerAddress())) {
			pcaLog.log(1, "POS_CommandImpl", command.getId(), "HandleRequest",
				   "RTOC was not valid");
			processError(POS_TO_RCS.INVALID_RTOC, "NOT_A_VALID_RTOC", null);
			return;
		    }
		    
		    // Are they supposed to be IN_CONTROL
		    if (!pca.isInControl(command.getControllerAddress())) {
			pcaLog.log(1, "POS_CommandImpl", command.getId(), "HandleRequest",
				   "RTOC is not in control at this time");
			processError(POS_TO_RCS.NOT_IN_CONTROL, "NOT_IN_CONTROL", null);
			return;
		    }
		    
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "POS_Q accepted request .. pushing command onto POS_Q at level: "+getLevel());
		    
		    posQ.push(getLevel(), this);
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "Pushed CommandProcessor onto POS_Q at level: "+getLevel()+
			       " QueueSize: "+posQ.getSize(getLevel()));
		    
		    // Work out the handling time here .. sum up all the stuff higher in
		    // the queue using handler.getDuration() .. the expected exec time.
		    
		    long total = 0L;
		    int level = getLevel();
		    // Add executor's total time.
		    if (posQ.getExecutor() != null)
			total = total + posQ.getExecutor().getDuration();
		    //System.err.println("POS_CI::After Exec: Total: "+total);
		    
		    // Work out how many levels above and include THIS level.
		    
		    for (int i = 0; i <= level; i++) {
			// Total for the level.
			total = total + posQ.getTotalDuration(i); 
			//System.err.println("POS_CI::After Level: "+i+" Total: "+total);
		    }
		    
		    
		    // Upgrade the handling time for this request and push to client.
		    handlingTime = total;
		    
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "Expected duration from tasks ahead in queue (inc. this): "+handlingTime+" millis.");
		    
		    ACK ack = new ACK(command.getId());
		    ack.setTimeToComplete((int)total);
		    processAck(ack);
		    
		    // Try to upgrade any tasks behind us ... i.e. at Lower levels.
		    // Sum total duration ahead and Ack its handler.
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "Attempting to upgrade lower level tasks.");
		    
		    POS_CommandImpl ohandler = null;
		    int nl = posQ.getLevels();
		    if (nl > level + 1) {
			for (int i = level + 1; i < nl; i++) {			 
			    Iterator it = posQ.listAll(i);
			    while (it.hasNext()) {
				ohandler = (POS_CommandImpl)it.next();
				ack.setTimeToComplete((int)total);
				ohandler.processAck(ack);
				pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
					   "Re-Acked "+ ohandler);
				total += ohandler.getDuration();
			    }
			}
		    }
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "HandleRequest",
			       "Done upgrading lower tasks.");
		    
		} else {

		    // PCA is not up for soem reason:
		    
		    if (RCS_Controller.controller.isOperational()) {
			
			if (pca.isScheduledWindow()) {
			    if (pca.getPcaInit()) {	
				processError(TESTLINK.PLANETARIUM_INITIALIZING, "PCA is currently initializing", null);
			    } else {
				processError(TESTLINK.OVERRIDDEN, "A higher priority MCA has overridden you", null);
			    }
			} else { 
			    processError(POS_TO_RCS.NOT_PLANETARIUM_MODE, "Request rejected by POS_Q", null);
			}

		    } else {
			processError(POS_TO_RCS.NOT_OPERATIONAL, "RCS is not currently operational", null);

		    }
		   
		}
	    }
	    
	} else {

	    // NON_ENQUEUED Commands are status and setup commands.

	    COMMAND_DONE done = processCommand();	
	    processDone(done);

	}
    }
    
    /** Returns the queue level of this command.
     * For those commands which are NOT enqueued this should return zero.*/
    public abstract int getLevel();

    /** Returns the command assocaited with this handler.*/
    public POS_TO_RCS getCommand() { return command; }
    
    /** Returns true if this command should be queued. The getLevel() method
     * should return the queue level if so. If false the getQueue() method
     * is not expected to be called but should return zero in any case.*/
    public abstract boolean enqueue();

    /** Returns the expected maximum duration of this command processing.*/
    public long getDuration() { return  DEFAULT_HANDLING_OVERHEAD; }

    /** Returns the default value for handling this command/request.*/
    public long getDefaultHandlingTime() { return DEFAULT_HANDLING_OVERHEAD; }
    
    /** Returns the current value of the handlingTime for this command - i.e.
     * how long a waiting thread should wait before testing again. This value may
     * be reset at any time e.g. by the JMS ACK mechanism.
     */
    public long getHandlingTime() {
	return handlingTime;
    }
    
    /** Creates a Task to be implemented to carry out this request.
     * Non queued handlers will not need this.*/
    public  abstract Task createTask();
    

    /** Boosts the handling time by a fudge factor to avoid SCT timeout before 
     * the executing/pending task has completed and then boosts again before
     * sending onto the client. 
     * ### CHECK THIS FUDGE ###.*/
    public void processAck(ACK ack) {
	handlingTime = (long)(FUDGE*ack.getTimeToComplete()) + DEFAULT_HANDLING_OVERHEAD;
	ack.setTimeToComplete((int)(FUDGE*handlingTime + DEFAULT_HANDLING_OVERHEAD));
	serverImpl.sendAck(ack);
    }
    
    /** Just calls sendDone() on the attached ServerImpl.*/
    public void processDone(COMMAND_DONE done) {
	serverImpl.sendDone(done); 
    }

    /** Make up an <i>appropriate</i> DONE and set its error code. Call processDone()
     * to force the message to be sent back.*/
    public abstract void processError(int errorCode, String errorMessage, Exception e);
    
    /** Clear up on disposal.
     * This method is called by ServerImpl as the final operation before it dies off.
     * It must locate the ProcessDescriptor in the POS_Queue and remove it whether it 
     * is PENDING or EXECUTING. If this was NOT an enqueued request then does nothing.
     */
    public void dispose() {
	// If this command was enqueued then we should remove its Queue reference.
	if (enqueue()) {
	    synchronized (posQ) {
		// We are executing .. wipe.
		if (posQ.getExecutor() == this) {  
		    pcaLog.log(2, "POS_CommandImpl", command.getId(), "dispose",
			       "Removing this handler as POS_Q executor.");
		    posQ.setExecutor(null);
		    return;
		}
	    }
	    // Ok we may be pending.
	    pcaLog.log(2, "POS_CommandImpl", command.getId(), "dispose",
		       "Looking for this handler in POS_Q pending lists.");
	    // If we cant remove it dont worry -- it may have been removed already 
	    // by an ABORT command.
	    if (posQ.removeRequest(this))
		pcaLog.log(2, "POS_CommandImpl", command.getId(), "dispose",
			   "Located and removed handler from POS_Q.");	  
	} 
    }
    
    
    /** This method is used to try and abort any attached task (Mainly ignore!).*/
    public void abort(int code, String message) {}

}

/** $Log: POS_CommandImpl.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/06/08 16:27:27  snf
/** Initial revision
/** */

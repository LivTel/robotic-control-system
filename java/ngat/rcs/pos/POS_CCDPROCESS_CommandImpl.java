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
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

/** Carries out the implementation of a POS CCDOBSERVE command.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CCDPROCESS_CommandImpl.java,v 1.2 2007/05/25 07:27:14 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CCDPROCESS_CommandImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class POS_CCDPROCESS_CommandImpl extends POS_CommandImpl {

    public static final String CLASS = "POS_CCDPROCESS_CommandImpl";
    
    public static final int LEVEL = 1;
    
    public static final int ERROR_BASE   = 9400;

    protected POS_CcdProcess_Task ccdProcessTask;
  
    public POS_CCDPROCESS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, POS_TO_RCS command) {
	super(serverImpl, command);	
    }

    /** Returns true if this command should be queued. */
    public boolean enqueue() { return true; }
    
    /** Returns the queue level of this command. */
    public int     getLevel() { return LEVEL; }

    /** Returns the expected maximum duration of this command processing.
     * ### Uses 30 secs, should be based on process type and mosaics/jpegs etc. 
     * ### plus some allowance for the data transfer ???? or do we seperate these out ??
     */
    public long getDuration() {
	return 30000L;	    
    }
    
   
    public COMMAND_DONE processCommand() {return null;}

    /** Creates a Task to be implemented to carry out this request.
     * Non queued handlers will not need this.*/
    public  Task createTask() {
    
	PlanetariumControlAgent planetariumCA = 
	    (PlanetariumControlAgent)PlanetariumControlAgent.getInstance();
	
	CCDPROCESS ccdproc = (CCDPROCESS)command;

	int  dest       = ccdproc.getDestination();
	int  procType   = ccdproc.getType();
	int  sourceType = ccdproc.getSourceType();
	long startFrame = ccdproc.getStartFrame();
	long endFrame   = ccdproc.getEndFrame();
	
	long frameMax = planetariumCA.getCurrentFrameCounter();
	if ((startFrame < 0) || (endFrame > frameMax)) {// or either  way ##
	    pcaLog.log(1, CLASS, command.getId(), "createTask",
		       "Start Frame: "+startFrame+" End Frame: "+endFrame+
		       " Current Range: [0 to "+frameMax+"]");
	    
	    processError(CCDPROCESS.MISSING_IMAGE,
			 "Start Frame: "+startFrame+" End Frame: "+endFrame+
			 " Current Range: [0 to "+frameMax+"]", null);
	    return null; // FATAL	    
	}
	
	ccdProcessTask =
	    new POS_CcdProcess_Task(ccdproc.getId(), 
				    planetariumCA,
				    this, 
				    dest,
				    procType,
				    sourceType,
				    startFrame,
				    endFrame);	
	return ccdProcessTask;    
    }
    
    /** Makes up a CCDOBSERVE_DONE and sets its error code etc.*/
    public void processError(int errorCode, String errorMessage, Exception e) {
	CCDPROCESS_DONE done = new CCDPROCESS_DONE(command.getId());
	done.setSuccessful(false);
	done.setErrorNum(errorCode);
	done.setErrorString(errorMessage + " : " + e);
	processDone(done);
    }

    /** Clear up on disposal. Calls super to unqueue from the POSQueue
     * This method is called by ServerImpl as the final operation before it dies off.
     */
    public void dispose() {
	super.dispose();
	ccdProcessTask = null;
    }
    
    /** This method is used to try and abort the attached task.*/
    public void abort(int code, String message) {
	if (ccdProcessTask != null) {
	    ccdProcessTask.setAbortCode(code, message);
	    ccdProcessTask.abort();
	}
    }
    
    /** Watch this we probably need to set an error code and return it to client.*/
    public void exceptionOccurred(Object source, Exception exception) {
	pcaLog.log(1, CLASS, command.getId(), "ExceptionCallback",
		   "POS_CCDProcessImpl::Source: "+source+" Exception: "+exception);
    }
    

}

/** $Log: POS_CCDPROCESS_CommandImpl.java,v $
/** Revision 1.2  2007/05/25 07:27:14  snf
/** changed log output for exception handler callback.
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */













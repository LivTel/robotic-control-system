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
import ngat.fits.*;
import ngat.phase2.*;
import ngat.phase2.nonpersist.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

import java.io.*;
import java.util.*;

/** This Task creates a series of TCS and ICS Tasks to carry out the
 * configuration and slewing of the Telescope and setting up of the 
 * relevant instruments. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CcdObserve_Task.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CcdObserve_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_CcdObserve_Task extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;
 
    /** Small angle offset = 1 arcsec.*/
    public static final double ANGLE_ARCSEC = Math.toRadians(1.0/3600.0); // arcsec.

    /** The Observation to perform.*/
    protected Observation observation;

    /** Tangent plane offset X (rads).*/
    double tpXOffset;
    
    /** Tangent plane offset Y (rads).*/
    double tpYOffset;


    /** A POS_CommandImplementor to handle the response data.*/
    protected POS_CommandImpl processor;

    /** Reference to the (single) Observation_Sequence_Task managed by this (TaskManager).*/
    protected Observation_Sequence_Task currentObservationSequenceTask;

    /** Stores the path of the image file on the remote (camera) system. Needed for
     * retrieval to local disk.*/
    protected String remoteImageFilename;

    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();


    /** Create a POS_CcdObserve_Task using the supplied Observation and settings.
     * @param observation The Observation to perform.
     * @param processor A POS_CommandImpl to handle the response data. Note: that all
     * POS_CommandImpls implement POS_CommandProcessor also so are effectively both. We need
     * the Impl methods for the response not the Processor methods.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public POS_CcdObserve_Task(String          name,
			       TaskManager     manager,
			       POS_CommandImpl processor,
			       Observation     observation) {
	super(name, manager);
	this.processor = processor;
	this.observation = observation;
    }

    /** Sets the Tangent Plane X offset.*/
    protected void setTpXOffset(double tpXOffset) { this.tpXOffset = tpXOffset; }

    /** Sets the Tangent Plane Y offset.*/
    protected void setTpYOffset(double tpYOffset) { this.tpYOffset = tpYOffset; }

    /** If the Observation_Sequence fails, generally the best we can do is
     * to send an error (U/S) and quit.*/
    public void onSubTaskFailed(Task task) {
	synchronized (taskList) {
	    super.onSubTaskFailed(task);
	    ErrorIndicator err = task.getErrorIndicator();
	    System.err.println("** NOTIFICATION - PosCcdObserveTask: onSubtaskFailed: Sending errorcode for U/S: "+
			       POS_TO_RCS.UNSPECIFIED_ERROR);
	    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 
	    // NOTE This calls abort(), hence onAborting() and so we sendError twice but
	    // second will not arrive as the handler will be gone by then via dispose() !.
	}
    }
  
    /** If the Observation_Sequence is aborted, generally we have called abort
     * either because the POT received an operational abort (weather/mech etc)
     * or an ABORT command was sent from client - either way we do nothing extra
     * here. Once we are fully aborted (ie. subtask has reported to us), we signal
     * the POT (manager) which then tells us to transmit an appropriate error code
     * to our client and disposes this task.
     */ 
    public void onSubTaskAborted(Task task) {
	super.onSubTaskAborted(task); 	
	System.err.println("** NOTIFICATION - PosCcdObserveTask: Subtask aborted: "+task.getName());

    }
    
    /** No special handling here - we only have one subtask so all done now.*/
    public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);
    }

    /** Causes the handler to send an Error message and dispose of itsself and
     * the queued/executing process in POS_Q. We dont hang about waiting for the task
     * to actually die off ??? WHY NOT ????*/
    public void onAborting() {
	super.onAborting();

	System.err.println("** NOTIFICATION - PosCcdObserveTask:: onAborting: with:"+
			   " Code = "+abortCode+
			   " Reason = "+abortMessage);
	
	processor.processError(abortCode,
			       abortMessage,
			       null);
    }
  
    
    /** When the subtask (Observation_Sequence) completes we should already have the name of the
     * FITS file (remoteImageFilename) on the CCS host. We need to ask the POT for a framenumber
     * and let it update its table of (frameno/remotefilename) so we can grab the image later or 
     * even now?. If we do not have the filename we need to log the error and send an error to 
     * the client UNSPECIFIED_ERROR with the appropriate lost-file message. If grabbing image now,
     * a TitClient is used to transfer the remote image into the POT's image-base directory.*/
    public void onCompletion() {
	super.onCompletion();
	// No location received for the remote (CCS-Host) image filename.
	// Tell the client..
	if (remoteImageFilename == null) {
	    processor.processError(CCDOBSERVE.UNSPECIFIED_ERROR,
				   "Observation completed but POS did not receive image file location.",
				   null);
	    return;
	} else { 
	   
	    long frameNumber = 0;
	    // Query the PCA for its frameCounter. We should have got an image file off the lowest level
	    // Exposure_Task via sigMessage(.., EXPOSURE_COMPLETE, imagefilename)	
	    try {
		frameNumber =  planetariumCA.advanceFrameCounter(remoteImageFilename);
	    } catch (IOException iox) {
		processor.processError(CCDOBSERVE.UNSPECIFIED_ERROR,
				       "Observation completed but POS could not link to instrument image directory.",
				       iox);
		return;
	    }
	   
	    CCDOBSERVE_DONE done = new CCDOBSERVE_DONE(name);
	    done.setSuccessful(true);
	    done.setErrorNum(0);
	    done.setErrorString("");
	   
	    done.setFrameNumber(frameNumber);	
	    processor.processDone(done);	  	    
	}
    }
    
    /** Overriden to perform setup <i>just prior</i> to initialization.*/
    public void preInit() {
	super.preInit();
    }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    public void onInit() {
	super.onInit();
	// TAG is RTOC in control or NULL.
	FITS_HeaderInfo.current_TAGID.setValue   (planetariumCA.getControllerId()); 
	// UID will be (schoolID) from the command's userId field.
	FITS_HeaderInfo.current_USERID.setValue  (planetariumCA.getCurrentUserName());
	FITS_HeaderInfo.current_PROPID.setValue  ("NONAME"); 
	FITS_HeaderInfo.current_GROUPID.setValue ("NONAME");
	
	// Need to set these mainly blank or they will inherit last SCA values.
	FITS_HeaderInfo.current_GRPTIMNG.setValue("PLANETARIUM");
	FITS_HeaderInfo.current_GRPTIMNG.setComment("Realtime observation");
	FITS_HeaderInfo.current_GRPMONP.setValue(new Double(0.0));
	FITS_HeaderInfo.current_GRPMONP.setComment("blank");

	// This is meaningless
	FITS_HeaderInfo.current_GRPUID.setValue(new Integer(0));	

	// This is to switch AG forwarding off for the RTI operations.
	ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);
	

    }
    
    /** Creates the TaskList for this TaskManager. A single Observation_Sequence_Task
     * using the Observation specified on construction.*/
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
	//
	// Basically we need the following:
	//
	// 1. SLEW only if we are not on target.
	// 2. IC   if its changed from current config (or inst has changed)
	// 3. INST only if the inst has changed (i.e. we were in another mode?)
	// 4. ROT SKY only if the skyPA has been changed
	// 5. ROT FLOAT if there is a TP offset.
	//
	// We should not need to do INST as the POS_INIT will do this for us?
	
	currentObservationSequenceTask = 
	    new Observation_Sequence_Task(name+"/OBSEQ",
					  this);
					 
	currentObservationSequenceTask.setObservation(observation);
	currentObservationSequenceTask.setStandard(false);     // Not a standard.
	currentObservationSequenceTask.setFixed(false);        // Not a fixed time.
	currentObservationSequenceTask.setTrackAfterDone(true);// Continue tracking after obs.
	currentObservationSequenceTask.setTpXOffset(tpXOffset);// Tangent plane offset X.
	currentObservationSequenceTask.setTpYOffset(tpYOffset);// Tangent plane offset Y.
	// Decide if we need to offset in TP and float rotator.
	if ((tpXOffset > ANGLE_ARCSEC) || (tpYOffset > ANGLE_ARCSEC))
	    currentObservationSequenceTask.setFloatAfterSlew(true);

	taskList.addTask(currentObservationSequenceTask);
	
	return taskList;
    }
    
    /** We deal with the following messages categories.
     * <dl>
     *  <dt> (Exposure_Task) EXPOSURE_COMPLETE (602101)
     *   <dd> Set the name of the remoteImageFilename to <message>.
     *  <dt> (Default_TaskImpl) ACK_RECEIVED (600001)
     *   <dd> Asynch request Planetarium_Ops task to broadcast to any waiting server threads.
     * </dl>
     * Any other categories are ignored
     */
    public void sigMessage(Task source, int category, Object message) {
	//System.err.println("CcdObserve_Task:: Message received from: "+source.getName()+
	//	   " Cat: "+category+" Message: "+message);
	switch (category) {
	case Exposure_Task.EXPOSURE_COMPLETE:
	    remoteImageFilename = (String)message;
	    break;
	case JMSMA_TaskImpl.ACK_RECEIVED:	
	    int level = processor.getLevel();
	    ACK ack   = (ACK)message;
	    //planetariumCA.broadcastAck( level, ack );
	    //System.err.println("CcdObserve_Task:: **** Relaying ACK");
	    processor.processAck(ack);
	    break;
	default:
	    break;
	}
    }

}

/** $Log: POS_CcdObserve_Task.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


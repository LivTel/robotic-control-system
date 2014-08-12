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
 * <dd>$Id: POS_MosaicObserveTask.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_MosaicObserveTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_MosaicObserveTask extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;



 
    /** Small angle offset = 1 arcsec.*/
    public static final double ANGLE_ARCSEC = Math.toRadians(1.0/3600.0); // arcsec.

    /** The Observation to perform.*/
    protected Observation observation;

    /** Tangent plane offset X (rads).*/
    protected double tpXOffset;
    
    /** Tangent plane offset Y (rads).*/
    protected double tpYOffset;

    /** The rotation of the plane (rads).*/
    protected double rotation;

    /** The target.*/
    protected Source source;

    /** A POS_CommandImplementor to handle the response data.*/
    protected POS_CommandImpl processor;

    /** Stores the path of the image file on the remote (camera) system. Needed for
     * retrieval to local disk.*/
    protected String remoteImageFilename;

    protected TangentPlaneOffsetTask offsetTask;

    protected Exposure_Task exposureTask;

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
    public POS_MosaicObserveTask(String          name,
				 TaskManager     manager,
				 POS_CommandImpl processor,
				 Observation     observation) {
	super(name, manager);
	this.processor = processor;
	this.observation = observation;
	source = observation.getSource();
    }

    /** Sets the Tangent Plane X offset.*/
    protected void setTpXOffset(double tpXOffset) { this.tpXOffset = tpXOffset; }

    /** Sets the Tangent Plane Y offset.*/
    protected void setTpYOffset(double tpYOffset) { this.tpYOffset = tpYOffset; }

    /** Sets the rotation of the plane.*/
    protected void setRotation(double rotation) { this.rotation = rotation; }

    /** If the Observation_Sequence fails, generally the best we can do is
     * to send an error (U/S) and quit.*/
    public void onSubTaskFailed(Task task) {
	synchronized (taskList) {
	    super.onSubTaskFailed(task);

	    ErrorIndicator ei = task.getErrorIndicator();
	    
	    taskLog.log(2, CLASS, name, "onSubTaskFailed",
			"During observation: "+task.getName()+" failed due to: "+ei.getErrorString());
    
	    if ( task instanceof Exposure_Task ) {	
		
		exposureTask = (Exposure_Task)task;
		int runs = exposureTask.getRunCount();
		if (runs < 2) {		   
		    resetFailedTask(exposureTask);
		    exposureTask.setDelay(10000L);
		    
		} else {
		    taskLog.log(2, CLASS, name, 
				"Expose Failed after 3 attempts: Error: code: "+
				ei.getErrorCode()+" msg: "+ei.getErrorString()); 
		    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
					   ei.getErrorString(),
					   ei.getException());	 
		    failed(Observation_Task.EXPOSURE_TASK_FAILED, 
			   "Expose failed after 3 attempts", null);		    
		}
	    } else if
		( task instanceof TangentPlaneOffsetTask ) { 
		
		offsetTask = (TangentPlaneOffsetTask)task;
		int runs = offsetTask.getRunCount();		    
		if (runs < 2) {
		    resetFailedTask(offsetTask); 
		    offsetTask.setDelay(10000L);	
		} else {
		    taskLog.log(2, CLASS, name, 
				"TPOffset Failed after 3 attempts: Error: code: "+
				ei.getErrorCode()+" msg: "+ei.getErrorString()); 
		    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
					   ei.getErrorString(),
					   ei.getException());	 
		    failed(Observation_Task.OFFSET_TASK_FAILED, 
			   "TPOffset failed after 3 attempts", null);
		}
	    } else {
		taskLog.log(2, CLASS, name, 
			    "Error: code: "+ei.getErrorCode()+" msg: "+ei.getErrorString());
		processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				       ei.getErrorString(),
				       ei.getException());	 
		failed(ei.getErrorCode(),
		       ei.getErrorString(),
		       ei.getException());
	    }
	    
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

	    opsLog.log(1, CLASS, name, "onCompletion",
			"Completed PCA Mosaic Observation exposure sequence successfully");

	    CCDOBSERVE_DONE done = new CCDOBSERVE_DONE(name);
	    done.setSuccessful(true);
	    done.setErrorNum(0);
	    done.setErrorString("");
	   
	    done.setFrameNumber(frameNumber);	
	    processor.processDone(done);	  	    
	}
    }
          
    public void onDisposal() {
	super.onDisposal();
	
    }

    /** Overriden to perform setup <i>just prior</i> to initialization.*/
    public void preInit() {
	super.preInit();
    }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    public void onInit() {
	super.onInit();	

	Position target = source.getPosition();
	
	// TAG is RTOC in control or NULL.
	FITS_HeaderInfo.current_TAGID.setValue   (planetariumCA.getControllerId()); 
	// UID will be (schoolID) from the command's userId field.
	FITS_HeaderInfo.current_USERID.setValue  (planetariumCA.getCurrentUserName());
	FITS_HeaderInfo.current_PROPID.setValue  ("NONAME"); 
	FITS_HeaderInfo.current_GROUPID.setValue ("NONAME");
	// Decide which type of Group we are doing for data-compression.
	
	// Work out if the Moon is UP or DOWN right now (start of obs) for FITS Header MOONSTAT.
	Position moon = Astrometry.getLunarPosition(); //## TEMP ##
	//Position moon = JSlalib.getLunarPosition();
	//Position sun  = JSlalib.getSolarPosition();
	if (moon.isRisen(0.0)) // add refraction stuff here FIXED amount. isRisen(-34')
	    FITS_HeaderInfo.current_MOONSTAT.setValue("UP");
	else
	    FITS_HeaderInfo.current_MOONSTAT.setValue("DOWN");
	Position sun  = Astrometry.getSolarPosition();
	double angle = moon.getAngularDistance(sun);
	FITS_HeaderInfo.current_MOONFRAC.setValue(new Double(0.5*(1.0 + Math.cos(Math.PI-angle))));

	taskLog.log(1, CLASS, name, "onInit",
		    "Starting PCA Mosaic Observation Sequence");
    }

    public void onFailure() {
	super.onFailure();
	opsLog.log(1, "Failed PCA Mosaic Observation Sequence."+
		   "\n\tCode:      "+errorIndicator.getErrorCode()+
		   "\n\tReason:    "+errorIndicator.getErrorString()+
		   "\n\tException: "+errorIndicator.getException());
    }

    /** Creates the TaskList for this TaskManager. A single Observation_Sequence_Task
     * using the Observation specified on construction.*/
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
	//
	// Basically we need the following:
	//
	// Offset tpx, tpy
	// Expose

	offsetTask = new TangentPlaneOffsetTask(name+".TPOFFSET",
						this,
						source,
						tpXOffset,
						tpYOffset,
						rotation);
	
	taskList.addTask(offsetTask);
	    
	exposureTask = new Exposure_Task(name+".EXPOSE",
					 this,
					 observation,
					 false);
	taskList.addTask(exposureTask);

	try {
	    taskList.sequence(offsetTask, exposureTask);
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for Observation: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for Observation.");
	    errorIndicator.setException(tx);
	    return null;
	}

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

/** $Log: POS_MosaicObserveTask.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


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
import ngat.instrument.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;

/** This Task is responsible for configuring the selected Instrument and setting up
 * appropriate rotator positioning.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_MosaicSetupTask.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_MosaicSetupTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_MosaicSetupTask extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;
 
    /** Configures the default instrument.*/
    protected Task instConfigTask ;

    /** Takes the (first) exposure.*/
    protected Task exposeTask ;

    /** Does position offsets.*/
    protected TangentPlaneOffsetTask offsetTask;

    /** Sets the rotator sky angle.*/
    protected RotatorTask rotatorPositionTask;

    /** Sets the rotator to FLOAT mode.*/
    protected RotatorTask rotatorFloatTask;

    /** Instrument configuration.*/
    protected InstrumentConfig config;

    /** Observation.*/
    protected Observation observation;
    
    /** The target.*/
    protected Source source;
    
    /** Processor to take response messages.*/
    protected POS_CommandImpl  processor;
 
    /** Reference to PCA.*/
    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();

    /** The instrument.*/
    protected Instrument inst;

    /** Instrument TCS alias.*/
    String instAlias;
    
    /** Stores the path of the image file on the remote (camera) system. Needed for
     * retrieval to local disk.*/
    protected String remoteImageFilename;

    /** Tangent plane offset X (rads).*/
    protected double tpXOffset;
    
    /** Tangent plane offset Y (rads).*/
    protected double tpYOffset;

    /** The rotation of the plane (rads).*/
    protected double rotation;

    /** Create a POS_MosaicSetupTask using the supplied Observation and settings.    
     * @param name        The unique name/id for this TaskImpl.
     * @param manager     The Task's manager.
     * @param observation The Observation to perform.
     * @param processor   A POS_CommandImpl to handle the response data. 
     */
    public POS_MosaicSetupTask(String           name,
			       TaskManager      manager,
			       POS_CommandImpl  processor,  
			       Observation      observation
			       ) {
	super(name, manager);
	this.processor   = processor;
	this.observation = observation;
	
	source    = observation.getSource();
	inst      = Instruments.findInstrumentFor(observation.getInstrumentConfig());	
	instAlias = Instruments.findAliasFor(inst.getName());

    }

    /** Sets the Tangent Plane X offset.*/
    protected void setTpXOffset(double tpXOffset) { this.tpXOffset = tpXOffset; }

    /** Sets the Tangent Plane Y offset.*/
    protected void setTpYOffset(double tpYOffset) { this.tpYOffset = tpYOffset; }

    /** Sets the rotation of the plane.*/
    protected void setRotation(double rotation) { this.rotation = rotation; }

    /** Send error and quit.*/
    public void onSubTaskFailed(Task task) {
	
	super.onSubTaskFailed(task);
	
	ErrorIndicator err = task.getErrorIndicator();
	
	if (task == instConfigTask) {
	    errorLog.log(1, "Sending InstError to POS");
	    processor.processError(CCDOBSERVE.CCD_FAULT,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 
	} else if
	    (task == exposeTask) {
	    errorLog.log(1, "Defaulting to errorcode for U/S: "+POS_TO_RCS.UNSPECIFIED_ERROR);
	    processor.processError(CCDOBSERVE.CCD_FAULT,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 	    	   
	} else if
	    (task == rotatorPositionTask) {
	    errorLog.log(1, "Sending TelError to POS");
	    processor.processError(CCDOBSERVE.TELESCOPE_FAULT,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 	   
	} else if
	    (task == rotatorFloatTask) {
	    errorLog.log(1, "Sending TelError to POS");
	    processor.processError(CCDOBSERVE.TELESCOPE_FAULT,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 
       
	} else {
	    
	    errorLog.log(1, "Defaulting to errorcode for U/S: "+POS_TO_RCS.UNSPECIFIED_ERROR);
	    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				   err.getErrorString(),
				   err.getException());	 
	    failed(err); 	   
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
    
    /** No special handling here.*/
    public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);
    }

    /** No special handling here.*/
    public void onAborting() {
	super.onAborting();	
	// Abort any ICS tasks.
	taskList.addTask(new Abort_Task(name+"**(ABORT)", 
					this, 
					inst.getName()));	
	processor.processError(abortCode,
			       abortMessage,
			       null);
    }
  
    /** When the subtask completes we should already have the name of the
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
	    opsLog.log(1, CLASS, name, "onCompletion",
			"Completed PCA Mosaic Observation setup sequence but POS did not receive image file location");

	    return;
	} else { 
	    
	    long frameNumber = 0;
	    // Query the PCA for its frameCounter. We should have got an image file off the lowest level
	    // Exposure_Task via sigMessage(.., EXPOSURE_COMPLETE, imagefilename)	
	    try {
		frameNumber =  planetariumCA.advanceFrameCounter(remoteImageFilename);

		opsLog.log(1, CLASS, name, "onCompletion",
			   "Completed PCA Mosaic Observation setup sequence with remote image frame no: "+frameNumber);
		
	    } catch (IOException iox) {
		processor.processError(CCDOBSERVE.UNSPECIFIED_ERROR,
				       "Observation setup completed but POS could not link to instrument image directory.",
				       iox);
		opsLog.log(1, CLASS, name, "onCompletion",
			    "Completed PCA Mosaic Observation setup sequence but POS could not link to instrument image directory.");
		
		return;
	    }

	    opsLog.log(1, CLASS, name, "onCompletion",
			"Completed PCA Mosaic Observation setup sequence successfully");
	    
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
	
	Source source   = observation.getSource();
	Position target = source.getPosition();
	
	ModalTask cmt =  (ModalTask)RCS_Controller.controlAgent.getCurrentModalTask();	
	String rootId =  
	    RCS_Controller.controller.getTelescopeId()+"_"+
	    (cmt != null ? cmt.getAgentId() : "???")+"_"+
	    (cmt != null ? cmt.getAgentVersion() : "???");
	
	opsLog.log(1, "Started PCA Mosaic Observation Setup."+
		   "\nObservation: "+observation.getName()+
		   "\n    Program:    "+rootId+
		   "\n   Multruns:    "+observation.getNumRuns()+
		   "\n   Exposure:    "+(observation.getExposeTime()/1000.0f)+" secs"+
		   (observation.isConditionalExposure() ? " (Time-adjustable)." : " (Fixed-duration).")+      
		   "\n     Source:    "+source.getName()+
		   "\n         RA:    "+Position.toHMSString(target.getRA())+
		   "\n        Dec:    "+Position.toDMSString(target.getDec())+
		   "\n         HA:    "+Position.toHMSString(target.getHA())+
		   "\n    Azimuth:    "+Position.toDegrees(target.getAzimuth(), 3)+
		   "\n   Altitude:    "+Position.toDegrees(target.getAltitude(), 3)+		  
		   "\n   InstConf:    "+observation.getInstrumentConfig().toString()+
		   "\n   TeleConf:    "+observation.getTelescopeConfig().toString()+
		   "\n     Source:    "+observation.getSource().toString());
	
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

	double ra =target.getRA();
	FITS_HeaderInfo.current_CAT_RA.setValue (Position.formatHMSString(ra, ":"));
	FITS_HeaderInfo.current_APP_RA.setValue (Position.formatHMSString(ra, ":"));				 
	FITS_HeaderInfo.current_RA.setValue     (Position.formatHMSString(ra, ":"));
	
	double dec = target.getDec();
	FITS_HeaderInfo.current_CAT_DEC.setValue (Position.formatHMSString(dec, ":"));
	FITS_HeaderInfo.current_APP_DEC.setValue (Position.formatHMSString(dec, ":"));
	FITS_HeaderInfo.current_DEC.setValue     (Position.formatHMSString(dec, ":"));
	
	taskLog.log(1, CLASS, name, "onInit",
		    "Starting PCA Mosaic Observation Setup");
	
    }
    
    public void onFailure() {
	super.onFailure();
	opsLog.log(1, "Failed PCA Mosaic Observation Setup."+
		   "\n\tCode:      "+errorIndicator.getErrorCode()+
		   "\n\tReason:    "+errorIndicator.getErrorString()+
		   "\n\tException: "+errorIndicator.getException());
    }

    /** Creates the TaskList for this TaskManager. A single Observation_Sequence_Task
     * using the Observation specified on construction.*/
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
       
	// Instrument config.
	InstrumentConfig instConfig = observation.getInstrumentConfig();	
	instConfigTask = new InstConfigTask(name+".INST_CFG", this,
					    instConfig);
	taskList.addTask(instConfigTask);

	// Exposure.	
	exposeTask = new Exposure_Task(name+".EXPOSE",
				       this,
				       observation,
				       false);
	exposeTask.setDelay(15000L);
	taskList.addTask(exposeTask);

	// Rot Pos.
	rotatorPositionTask = new RotatorTask(name+".ROT_SKY", this,
					      rotation, ROTATOR.SKY);
	taskList.addTask(rotatorPositionTask);
	
	// Rot Float.
	rotatorFloatTask = new RotatorTask(name+".ROT_FLT", this,
					   0.0, ROTATOR.FLOAT);
	rotatorFloatTask.setDelay(15000L);
	taskList.addTask(rotatorFloatTask);

	// Tangent plane offset.
	offsetTask = new TangentPlaneOffsetTask(name+".TPOFFSET",
						this,
						source,
						tpXOffset,
						tpYOffset,
						rotation);
	
	taskList.addTask(offsetTask);

	// Sequencing.
	try {	  
	    taskList.sequence(instConfigTask,      rotatorPositionTask);
	    taskList.sequence(offsetTask,          rotatorPositionTask);
	    taskList.sequence(rotatorPositionTask, rotatorFloatTask); 
	    taskList.sequence(rotatorFloatTask,    exposeTask);
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for POS_Observe: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for POS_MosaicSetup.");
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

/** $Log: POS_MosaicSetupTask.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/** */


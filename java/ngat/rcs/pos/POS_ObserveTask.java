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
import ngat.message.GUI_RCS.*;

import java.io.*;
import java.util.*;

/** This Task is responsible for configuring the selected Instrument
 * and taking the specified exposure.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_ObserveTask.java,v 1.2 2007/07/05 11:33:11 snf Exp $
 * <dt><b>Source</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_ObserveTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class POS_ObserveTask extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;
 
    /** Configures the default instrument.*/
    protected Task instConfigTask ;
    
    /** Takes the exposure.*/
    protected Task exposeTask ;

    /** Processor to take response messages.*/
    protected POS_CommandImpl  processor;
 
    /** Reference to PCA.*/
    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();

    /** Observation.*/
    protected Observation observation;

    /** The instrument.*/
    protected Instrument inst;

    /** Describes source type.*/
    protected String sourceType;

    /** Instrument TCS alias.*/
    String instAlias;
    
    /** Name of obs = frame number.*/
    String obsName = null;

    /** set to override autoguide on commands via ISS.*/
    protected boolean overrideAutoguider = false;

  /** Stores the path of the image file on the remote (camera) system. Needed for
     * retrieval to local disk.*/
    protected String remoteImageFilename;


    /** Create a POS_CcdObserve_Task using the supplied Observation and settings.    
     * @param name        The unique name/id for this TaskImpl.
     * @param manager     The Task's manager.
     * @param observation The Observation to perform.
     * @param processor   A POS_CommandImpl to handle the response data. 
     */
    public POS_ObserveTask(String           name,
			   TaskManager      manager,
			   POS_CommandImpl  processor,
			   Observation      observation,
			   String           sourceType) {

	super(name, manager);
	this.processor   = processor;
	this.observation = observation;
	this.sourceType = sourceType;

	inst      = Instruments.findInstrumentFor(observation.getInstrumentConfig());	
	instAlias = Instruments.findAliasFor(inst.getName());
    }

    /** If the Observation_Sequence fails, generally the best we can do is
     * to send an error (U/S) and quit.*/
    public void onSubTaskFailed(Task task) {
	synchronized (taskList) {
	    super.onSubTaskFailed(task);

	    ErrorIndicator err = task.getErrorIndicator();
	    int code = err.getErrorCode();

	    if (code == 100302 ||
		code == 1000302) {
		// We should detect a problem with the AG acquisition and reset the ISS override temporarily 
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed",
			    "Detected failed autoguider during observation");
			   
		taskLog.log(2, CLASS, name, "handleObservationTaskFailed",
			    "Temporarily re-enabling ISS autoguider start command override");
		ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);
		resetFailedTask(task);
	
	    } else {
		
		errorLog.log(1, "Currently defaulting to errorcode for U/S: "+POS_TO_RCS.UNSPECIFIED_ERROR);
		processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				       err.getErrorString(),
				       err.getException());	 
		failed(err); 	   
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

	System.err.println("** NOTIFICATION - ObserveTask:: onAborting: with:"+
			   " Code = "+abortCode+
			   " Reason = "+abortMessage);
		
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
				   "Observation completed but PCA did not receive image file location.",
				   null);
	    opsLog.log(1, CLASS, name, "onCompletion",
			"Completed PCA observation sequence but PCA did not receive image file location");

	    return;
	} else { 
	    
	    long frameNumber = 0;
	    // Query the PCA for its frameCounter. We should have got an image file off the lowest level
	    // Exposure_Task via sigMessage(.., EXPOSURE_COMPLETE, imagefilename)	
	    try {
		frameNumber =  planetariumCA.advanceFrameCounter(remoteImageFilename);

		opsLog.log(1, CLASS, name, "onCompletion",
			    "Completed PCA observation sequence with remote image frame no: "+frameNumber);
		
	    } catch (IOException iox) {
		processor.processError(CCDOBSERVE.UNSPECIFIED_ERROR,
				       "Observation completed but PCA could not link to instrument image directory.",
				       iox);
		opsLog.log(1, CLASS, name, "onCompletion",
			    "Completed PCA observation sequence but PCA could not link to instrument image directory.");
		
		return;
	    }

	    opsLog.log(1, CLASS, name, "onCompletion",
			"Completed PCA observation sequence successfully");

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

	// when UNSET, the AGSTART commands will be forwarded appropriately.
	overrideAutoguider = (config.getProperty("override.autoguider") != null);

	// Override forwarding of AG START commands by ISS to TCS.
	ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(overrideAutoguider);

	// Reset focus expicitly at start of obs.
	ISS.getInstance().currentFocusOffset = 0.0;

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
	
	opsLog.log(1, "Started PCA observation sequence."+
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

	// ### This may need formatting to fixed length with '000..' padding.
	obsName = ""+planetariumCA.getNextFrameCounter();
	FITS_HeaderInfo.current_OBSID.setValue   (obsName);

	// Set the target IDs.
	FITS_HeaderInfo.current_SRCTYPE.setValue  (sourceType);
	FITS_HeaderInfo.current_CAT_NAME.setValue (source.getName());
	FITS_HeaderInfo.current_OBJECT.setValue   (source.getName());
	
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
		    "Starting PCA Observation sequence");

	// Telemetry.
	String program = rootId;

	ObservationInfo info = new ObservationInfo(System.currentTimeMillis());
	info.setObservation(observation);
	info.setProgramId(program);
	info.setFixed(false);
	info.setStandard(false);
	
	Telemetry.getInstance().publish("OBS", info);

    }

    public void onFailure() {
	super.onFailure();
	opsLog.log(1, "Failed PCA Observation sequence."+
		   "\n\tCode:      "+errorIndicator.getErrorCode()+
		   "\n\tReason:    "+errorIndicator.getErrorString()+
		   "\n\tException: "+errorIndicator.getException());
    }

    /** Creates the TaskList for this TaskManager. A single Observation_Sequence_Task
     * using the Observation specified on construction.*/
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
	
	// Instrument config.
	InstrumentConfig instConfig = observation.getInstrumentConfig();
	instConfigTask = new InstConfigTask(name+"/INST_CFG", this,
					    instConfig);
	taskList.addTask(instConfigTask);

	// Exposure.	
	exposeTask = new Exposure_Task(name+"/OBS",
				       this,
				       observation,
				       false);
	taskList.addTask(exposeTask);

	// Sequencing.
	try {
	    taskList.sequence(instConfigTask, exposeTask);
	    
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for POS_Observe: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for POS_Observe.");
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
	    
	    String tagName  = planetariumCA.getControllerId();
	    String userName = planetariumCA.getCurrentUserName();
	    obsName         = "Frame-"+planetariumCA.getNextFrameCounter();
	    
	    obsLog.log(1, "PCA Program:"+ tagName+" : "+userName+" : - : - : "+obsName+": Exposure Completed, File: "+message);
	    
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

/** $Log: POS_ObserveTask.java,v $
/** Revision 1.2  2007/07/05 11:33:11  snf
/** checkin
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/** */


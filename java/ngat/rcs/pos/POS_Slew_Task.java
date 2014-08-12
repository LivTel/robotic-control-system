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
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;

/** This Task performs slewing of the Telescope in Planetarium mode.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_Slew_Task.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_Slew_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_Slew_Task extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;

    /** Delay before retrying a failed (Track) Task.*/
    public static final long RETRY_DELAY = 10000L;

    /** Source to slew to.*/
    protected Source source;

    /** A POS_CommandImplementor to handle the response data.*/
    protected POS_CommandImpl processor;

    /** Reference to the (single) SlewTask managed by this (TaskManager).*/
    protected SlewTask slewTask;

    /** Tracking task.*/
    protected Task trackOnAzTask;

    /** Tracking task.*/
    protected Task trackOnAltTask;

    /** Tracking task.*/
    protected Task trackOnRotTask;

    protected Task rotMountTask;

    protected Task rotFloatTask;

    protected Task rotSkyTask;

    /** TEMP sets if we use the rotator as specd or rot mou+float.*/
    protected boolean useRotator = false;

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
    public POS_Slew_Task(String          name,
			 TaskManager     manager,
			 POS_CommandImpl processor,
			 Source          source) {
	super(name, manager);
	this.processor = processor;
	this.source    = source;
    }
    
    /** If the Slew fails, generally the best we can do is
     * to send an error (U/S) and quit.*/
    public void onSubTaskFailed(Task task) {
	synchronized (taskList) {

	    super.onSubTaskFailed(task);

	    if 
		( task == trackOnAzTask ||
		  task == trackOnAltTask ||
		  task == trackOnRotTask ||
		  task == rotMountTask   ||
		  task == rotFloatTask   ||
		  task == rotSkyTask) {
		
		// TRACK ON.
		
		int runs = ((JMSMA_TaskImpl)task).getRunCount();
		errorLog.log(1, CLASS, name, "onSubTaskFailed", 
			     "Task: "+task.getName()+" failed..on run "+runs);
		if (runs < 3) {	  	   
		    resetFailedTask(task);
		    ((JMSMA_TaskImpl)task).setDelay(RETRY_DELAY);
		    errorLog.log(1, CLASS, name, "onSubTaskFailed", 
				 "Waiting "+RETRY_DELAY+" millis before retrying.");
		} else if
		    (runs >= 3) { 
		    ErrorIndicator err = task.getErrorIndicator();
		    System.err.println("Sending errorcode for TELESCOPE_ERROR: "+CCDOBSERVE.TELESCOPE_FAULT);
		    processor.processError(CCDOBSERVE.TELESCOPE_FAULT,
					   err.getErrorString(),
					   err.getException());	
		    
		    failed(666772, "Track ON failed - ##TEMP## aborting", null);
		}

	    } else {
		
		// Anything else.

		ErrorIndicator err = task.getErrorIndicator();
		System.err.println("Sending errorcode for TELESCOPE_ERROR: "+CCDOBSERVE.TELESCOPE_FAULT);
		processor.processError(CCDOBSERVE.TELESCOPE_FAULT,
				       err.getErrorString(),
				       err.getException());	 
		failed(err); 
		// NOTE This calls abort(), hence onAborting() and so we sendError twice but
		// second will not arrive as the handler will be gone by then via dispose() !.
	    }
	}
    }

    /** If the Slew is aborted, generally we have called abort
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
	synchronized (taskList) {  super.onAborting();	  	  
	// Stop TCS Slewing scope.
	taskList.addTask(new Track_Task(name+"**TRACK_AZ_OFF)",
					this,
					TRACK.AZIMUTH,
					TRACK.OFF));
	
	taskList.addTask(new Track_Task(name+"**TRACK_ALT_OFF)",
					this,
					TRACK.ALTITUDE,
					TRACK.OFF));
	
	taskList.addTask(new Track_Task(name+"**TRACK_ROT_OFF)",
					this,
					TRACK.ROTATOR,
					TRACK.OFF));
	
	
	}
    }
  
    
    /** When the subtask (Observation_Sequence) completes we should already have the name of the
     * FITS file (remoteImageFilename) on the CCS host. We need to ask the POT for a framenumber
     * and let it update its table of (frameno/remotefilename) so we can grab the image later or 
     * even now?. If we do not have the filename we need to log the error and send an error to 
     * the client UNSPECIFIED_ERROR with the appropriate lost-file message. If grabbing image now,
     * a TitClient is used to transfer the remote image into the POT's image-base directory.*/
    public void onCompletion() {
	super.onCompletion();

	taskLog.log(1, CLASS, name, "onCompletion",
		   "Completed PCA Slew");

	Properties props = planetariumCA.getAgentProperties();
	if (props !=null)
	    props.setProperty("current", "");
	
	CCDOBSERVE_DONE done = new CCDOBSERVE_DONE(name);
	done.setSuccessful(true);
	done.setErrorNum(0);
	done.setErrorString("");
	
	done.setFrameNumber(0);	
	processor.processDone(done);	
    }
    
    /** Overriden to perform setup <i>just prior</i> to initialization.*/
    public void preInit() {
	super.preInit();
 
	useRotator = (config.getProperty("use.rotator","false").equals("true"));	

    }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    public void onInit() {
	super.onInit();	

	// Check if we need to TRACK ON or not.

	





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

	FITS_HeaderInfo.current_EQUINOX.setValue  (new Double(source.getEquinox()));
	
	taskLog.log(1, CLASS, name, "onInit",
		    "Starting PCA Slew");

	planetariumCA.setActivity("Slew:"+source);

    }
    
    /** Creates the TaskList for this TaskManager. 
     *
     * A combination of enabling tracking on the axes, setting rotator (or not)
     * and performing a slew to position. This needs modifying to cope with various
     * rotator issues and will be influenced by mosaicing.
     *
     */
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
	
	trackOnAzTask = new Track_Task(name+"/TRACK_ON_AZ",
				     this,
				     TRACK.AZIMUTH,
				     TRACK.ON);
	
	trackOnAltTask = new Track_Task(name+"/TRACK_ON_ALT",
				     this,
				     TRACK.ALTITUDE,
				     TRACK.ON);
	
	//trackOnRotTask = new Track_Task(name+"/TRACK_ON_ROT",
	//		     this,
	//		     TRACK.ROTATOR,
	//		     TRACK.ON);
	
	taskList.addTask(trackOnAzTask);
	taskList.addTask(trackOnAltTask);
	//taskList.addTask(trackOnRotTask);

	slewTask = new SlewTask(name+"/SLEW",
				this,
				source);
	taskList.addTask(slewTask);
	
	if (useRotator) {

	    rotSkyTask = new RotatorTask(name+"/ROT_SKY",
	    			 this,
	    			 0.0,
	    			 ROTATOR.SKY);
	    rotSkyTask.setDelay(5000L);
	    taskList.addTask(rotSkyTask);
	    
	} else {
	    
	    rotMountTask = new RotatorTask(name+"/ROT_MOU",
					   this,
					   0.0,
					   ROTATOR.MOUNT);
	    rotMountTask.setDelay(5000L);
	    taskList.addTask(rotMountTask);
	    
	    rotFloatTask = new RotatorTask(name+"/ROT_FLO",
					   this,
					   0.0,
					   ROTATOR.FLOAT);
	    taskList.addTask(rotFloatTask);
	}

	try {	
	    if (useRotator) {
		taskList.sequence(trackOnAzTask,   rotSkyTask);
		taskList.sequence(trackOnAltTask,  rotSkyTask);
	    } else {
		taskList.sequence(trackOnAzTask,   rotMountTask);
		taskList.sequence(trackOnAltTask,  rotMountTask);
	    }

	    taskList.sequence(trackOnAzTask,   slewTask);
	    taskList.sequence(trackOnAltTask,  slewTask);
 
	    if (! useRotator) {		 
		taskList.sequence(rotMountTask,    rotFloatTask);
	    }

	} catch (TaskSequenceException tx) {	   
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for POS_Slew: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for POS_Slew.");
	    errorIndicator.setException(tx);
	    return null;
	}

	return taskList;
    }   

}

/** $Log: POS_Slew_Task.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */


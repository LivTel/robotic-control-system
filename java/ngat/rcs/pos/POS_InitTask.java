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

/** This Task is responsible for configuring the telescope ready for
 * the start of Planetarium operations. The telescope is NOT slewed and
 * the default instrument is selected but NOT configured.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_InitTask.java,v 1.3 2007/05/24 12:50:09 snf Exp $
 * <dt><b>Source</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_InitTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.3 $
 */
public class POS_InitTask extends ParallelTaskImpl {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;
    
    public static final int TASK_FAILURE = 665787;

 
    /** Selects the default POS instrument.*/
    protected Task instrumentTask ;
    
    /** Sets the rotator mode and sky angle to default.*/
    protected Task rotatorTask ;

    /** Starts the Trascking on all axes and trackable mechs.*/
    protected Task trackOnTask;

    /** Selects autoguider.*/
    protected Task agSelectTask;
        
    /** Selects default autoguider mode.*/
    protected Task autoguideTask ;

    /** Select appropriate aperture offset for pos instrument.*/
    protected Task apertureOffsetTask;

    /** Stops the Azimuth axis.*/
    protected Task stopAzTask;
    
    /** Stops the Altitude.*/
    protected Task stopAltTask;

    /** Stops the Rotator.*/
    protected Task stopRotTask;
    
    protected Task tempFocusTask;

    /** Reference to PCA.*/
    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();


    /** POS Instrument.*/
    protected String instName;
 
    /** Instrument TCS alias.*/
    protected String instAlias;
    

    /** Create a POS_CcdObserve_Task using the supplied Observation and settings.
     * @param observation The Observation to perform.
     * @param processor A POS_CommandImpl to handle the response data. Note: that all
     * POS_CommandImpls implement POS_CommandProcessor also so are effectively both. We need
     * the Impl methods for the response not the Processor methods.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public POS_InitTask(String      name,
			TaskManager manager) {
	super(name, manager);
	
	// Use PCA default instrument.
	instName  = planetariumCA.getInstrumentName();	
	instAlias = Instruments.findAliasFor(instName);

    }

    /** If the Observation_Sequence fails, generally the best we can do is
     * to send an error (U/S) and quit.*/
    public void onSubTaskFailed(Task task) {
	
	super.onSubTaskFailed(task); 
	
	if 
	    ( task == trackOnTask ) {
	    
	    // TRACK ON.
	    
	    int runs = ((JMSMA_TaskImpl)task).getRunCount();
	    errorLog.log(1, CLASS, name, "onSubTaskFailed", 
			 "Task: "+task.getName()+" failed..on run "+runs);
	    if (runs < 3) {
		((JMSMA_TaskImpl)task).setDelay(10000L);	  	   
		resetFailedTask(task); 
	    } else if
		(runs >= 3) { 
		failed(TASK_FAILURE, "Track ON failed - ##TEMP## aborting", null);
	    }
	    
	} else if
	    (task instanceof StopTask) {
	    
	    // STOP AXIS.
	    
	    int runs = ((JMSMA_TaskImpl)task).getRunCount();
	    errorLog.log(1, CLASS, name, "onSubTaskFailed", 
			 "Task: "+task.getName()+" failed..on run "+runs);
	    if (runs < 6) {	  	   
		resetFailedTask(task); 
	    } else if
		(runs >= 6) { 
		failed(TASK_FAILURE, "Stop Axis failed - ##TEMP## aborting", null);
	    }
	    
	} else if
	    ( task == instrumentTask ) {
	    
	    // INST SELECT.
	    
	    int runs = ((JMSMA_TaskImpl)task).getRunCount();
	    errorLog.log(1, CLASS, name, "onSubTaskFailed", 
			 "Task: "+task.getName()+" failed..on run "+runs);
	    if (runs < 3) {	  
		((JMSMA_TaskImpl)task).setDelay(10000L);
		resetFailedTask(task); 
	    } else if
		(runs >= 3) { 
		failed(TASK_FAILURE, "Instrument Select failed - ##TEMP## aborting", null);
	    }
	    
	} else if
	    ( task == agSelectTask ) {
	    
	    // AGSEL SELECT.
	    
	    int runs = ((JMSMA_TaskImpl)task).getRunCount();
	    errorLog.log(1, CLASS, name, "onSubTaskFailed", 
			 "Task: "+task.getName()+" failed..on run "+runs);
	    if (runs < 3) {
		((JMSMA_TaskImpl)task).setDelay(10000L);	  	   
		resetFailedTask(task); 
	    } else if
		(runs >= 3) { 
		failed(TASK_FAILURE, "AGSelect failed - ##TEMP## aborting", null);
	    }

	} else if
            ( task == apertureOffsetTask) {
	    
	    errorLog.log(1, CLASS, name, "onSubTaskFailed",
			 "Task: "+task.getName()+" failed, skipping - perfomance may be compromised");
	    taskList.skip(task);

	}  else {
	    
	    // Anything else we give up immediately.
	    
	    ErrorIndicator err = task.getErrorIndicator();	   
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
    }
    
    /** Log completion.*/
    public void onCompletion() {
	super.onCompletion();	
	opsLog.log(1, "Planetarium mode was successfully initialized");
	planetariumCA.setInitializedStatus(true);
    }

    /** Log failure.*/
    public void onFailure() {
	super.onFailure();
	opsLog.log(1, "Planetarium mode was not correctly initialized but carrying on anyway"+
		   "\n Error Code: "+errorIndicator.getErrorCode()+
		   "\n Reason:     "+errorIndicator.getErrorString());
	planetariumCA.setInitializedStatus(false);
    }

    public void onDisposal() {
	super.onDisposal();	
	planetariumCA.setPcaInit(false);	
	POS_Queue.getInstance().setAccept(true);
    }
       
    /** Overriden to perform setup <i>just prior</i> to initialization.*/
    public void preInit() {
	super.preInit();
	planetariumCA.setPcaInit(true);
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
		    "Starting PCA initialization");

	opsLog.log(1, CLASS, name, "onInit",
		    "Starting PCA initialization");

    }
    
    /** Creates the TaskList for this TaskManager. A single Observation_Sequence_Task
     * using the Observation specified on construction.*/
    protected TaskList createTaskList(TaskMonitorFactory tmfactory) {
	
	// select the POS instrument.
	instrumentTask = new InstrumentSelectTask(name+".I_SEL", 
						  this, 
						  instName,
						  instAlias);
	taskList.addTask(instrumentTask);

	// setup aperture offset for POS instrument.
	int number = Instruments.findApertureNumber(instName);

	apertureOffsetTask = new ApertureOffsetTask(name+"/AP_OFF",
						    this,
						    number);
	taskList.addTask(apertureOffsetTask);

		
	// Rotator mode selection.
	TelescopeConfig teleConfig = planetariumCA.getDefaultTelescopeConfig();
	double angle = teleConfig.getSkyAngle();
	int    mode  = 0;
	switch (teleConfig.getRotatorAngleMode()) {
	case TelescopeConfig.ROTATOR_MODE_MOUNT:
	    mode = ROTATOR.MOUNT;
	    break;
	case TelescopeConfig.ROTATOR_MODE_SKY:
	    mode = ROTATOR.SKY;
	    break;
	default:
	    mode = ROTATOR.SKY;
	    angle = 0.0;
	}
	
	if (teleConfig.getUseParallacticAngle()) {
	    mode  = ROTATOR.VERTICAL;
	    angle = 0.0;
	}
	
	rotatorTask = new RotatorTask(name+".ROTAT", 
				      this, 
				      angle, 
				      mode);
	taskList.addTask(rotatorTask);
	
	
	// Autoguider selection
	agSelectTask = new AgSelectTask(name+".AG_SEL", 
					this,
					AGSELECT.CASSEGRAIN);
	taskList.addTask(agSelectTask);
	
	autoguideTask = new AutoGuide_Task(name+".AG_MODE", 
					   this, 
					   teleConfig, 
					   AUTOGUIDE.OFF);
	taskList.addTask(autoguideTask);

	// Stop all axes for now.
	stopAzTask = new StopTask(name+".STOPAZ",
				  this,
				  STOP.AZIMUTH);
	taskList.addTask(stopAzTask);
	taskList.skip(stopAzTask);
	stopAltTask = new StopTask(name+".STOPALT",
				   this,
				   STOP.ALTITUDE);
	taskList.addTask(stopAltTask);
	taskList.skip(stopAltTask);
	stopRotTask = new StopTask(name+".STOPROT",
				   this,
				   STOP.ROTATOR);
	taskList.addTask(stopRotTask);
	taskList.skip(stopRotTask);

	// Tracking ON.
	//trackOnTask = new Track_Task(name+".TRKON",
	///		    this,
	//		    TRACK.ALL,
	//		    TRACK.ON);

	//taskList.addTask(trackOnTask);
	//taskList.skip(trackOnTask);

	// TEMP Focus.	
	tempFocusTask = new FocusTask(name+".TMP_FOCUS",
				      this,
				      InitializeTask.initFocus);

	tempFocusTask.setDelay(5000L);
	taskList.addTask(tempFocusTask);
	
	// Sequencing. ( stopAxes & ((instSel & rot) + (agSel & AgOff)) & trackOn )
	try {
	    taskList.sequence(stopAzTask,     instrumentTask);
	    taskList.sequence(stopAltTask,    instrumentTask);
	    taskList.sequence(stopRotTask,    instrumentTask);
	    taskList.sequence(instrumentTask, tempFocusTask);
	    taskList.sequence(instrumentTask, apertureOffsetTask);
	    taskList.sequence(apertureOffsetTask, tempFocusTask);
	    taskList.sequence(tempFocusTask,  rotatorTask);
	    taskList.sequence(agSelectTask,   autoguideTask);
	    //taskList.sequence(rotatorTask,    trackOnTask);
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for POS_Init: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for POS_Init.");
	    errorIndicator.setException(tx);
	    return null;
	}

	return taskList;
    }  

}

/** $Log: POS_InitTask.java,v $
/** Revision 1.3  2007/05/24 12:50:09  snf
/** typo on skiptask
/**
/** Revision 1.2  2007/05/24 12:47:25  snf
/** added aperture offset to initialization.
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/** */


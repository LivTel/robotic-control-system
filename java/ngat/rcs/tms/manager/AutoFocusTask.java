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
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;
import java.awt.geom.*;

/** This Task manages the automatic focus based on truss temperature.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AutoFocusTask.java,v 1.1 2007/11/15 11:45:40 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/AutoFocusTask.java,v $
 * </dl>
 * @author $Author $
 * @version $Revision: 1.1 $
 */
public class AutoFocusTask extends ParallelTaskImpl implements Logging {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, AUTO_FOCUS = 800
	
    public static final double DEFAULT_ALTITUDE_ERROR = 1.0;

    public static final double DEFAULT_FOCUS_ERROR    = 0.05;

    /** Track focus off.*/
    Track_Task trackFocusOffTask;
    
    /** Track focus on.*/
    Track_Task trackFocusOnTask;
   
    /** Track agfocus off.*/
    Track_Task trackAgFocusOffTask;
    
    /** Track agfocus on.*/
    Track_Task trackAgFocusOnTask;
 
    /** Shift to altitude.*/
    AltitudeTask altitudeTask;
    
    /** Select focus.*/
    FocusTask focusTask;

    /** Offset focus.*/
    DefocusTask defocusTask;

    double altitudeError;

    double focusError;
    
    double altitude;

    double trussTemperature;

    /** Required focus (mm).*/
    double focus;
    
    /** Focus function slope.*/
    double focusSlope;
   
    /** Focus fn zero point.*/
    double focusZero;

    /** Focus travel rate (mm/sec).*/
    double focusTravelRate;

    /** TEMP: For use by ins_sel during MCA mode change.*/
    public static double initFocus;

    // InstrumentDeployTestTask ######
    
    static final SimpleDateFormat sd1 = new SimpleDateFormat("yyyyMMdd");
    
    /** Create an AutoFocusTask using the supplied settings.
     * @param name    The unique name/id for this// ERROR_BASE: RCS = 6, TMM/EXEC = 40, CONFIG = 200 TaskImpl .
     * @param manager The Task's manager.
     */
    public AutoFocusTask(String      name,
			 TaskManager manager) {
	super(name, manager);
    }

    @Override
	public void reset() {
	super.reset();
    }

    /** Handle subtask failure.*/
    @Override
	public void onSubTaskFailed(Task task) {
	
	super.onSubTaskFailed(task);
	
	if 
	    (task instanceof AltitudeTask) {
	    
	    double currentAltitude = StatusPool.latest().mechanisms.altPos;
	    if (Math.abs(currentAltitude - altitude) < altitudeError) {
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "Alt: "+currentAltitude+
			    ", Focus Alt: "+altitude+" within: "+altitudeError+
			    ", Close enough so carrying on");
		taskList.skip(task);
		
	    } else {
		
		// Not close enough so try again.
		if (((JMSMA_TaskImpl)task).getRunCount() <= 3) {
		    taskLog.log(2, CLASS, name, "onSubTaskFailed", 
				"Alt diff is too large, retrying in 10 secs");
		    ((JMSMA_TaskImpl)task).setDelay(10000L);
		    resetFailedTask(task);
		    
		} else {
		    taskLog.log(2, CLASS, name, "onSubTaskFailed", 
				"After 4 attempts alt diff is still wild, ignore and hope for the best");
		    taskList.skip(task);
		}

	    }
	    
	} else if 
	    (task instanceof Track_Task) {

	    if (((JMSMA_TaskImpl)task).getRunCount() < 2) {
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "Retrying after 10 secs");
		((JMSMA_TaskImpl)task).setDelay(10000L);
		resetFailedTask(task);
		
	    } else {

		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "After 2 failed attempts, ignore and hope for the best");
		taskList.skip(task);

	    }
	    
	} else if 
	    (task instanceof DefocusTask) {

	    if (((JMSMA_TaskImpl)task).getRunCount() < 6) {
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "Retrying after 10 secs");
		((JMSMA_TaskImpl)task).setDelay(10000L);
		resetFailedTask(task);
		
	    } else {
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "After 6 failed attempts, ignore and hope for the best");
		taskList.skip(task);

	    }
     
	} else if
	    (task instanceof FocusTask) {
	    
	    if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
			
		FocusTask ftask = (FocusTask)task;
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "Retrying after 20 secs");

		ftask.setDelay(20000L);
		resetFailedTask(ftask);
		
		trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;
		
		focus = focusZero + focusSlope*trussTemperature;
		
		initFocus = focus;
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    name+": Recalculated after failure: Focus="+focus+" at Alt="+altitude+
			    ", With new Truss="+trussTemperature);

		ftask.setFocus(focus);

	    } else {
		
		taskLog.log(2, CLASS, name, "onSubTaskFailed", 
			    "After 3 failed attempts, ignore and hope for the best");
		taskList.skip(task);
		
	    }

	} else {		    
	    failed(640801, "Temporary fail Autofocus operation due to subtask failure.."+task.getName(), null);
	}
	
    }
    
    
    
    @Override
	public void onSubTaskAborted(Task task) {
	super.onSubTaskAborted(task);
    }
    
    @Override
	public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);
	
	if (task == trackFocusOffTask) {
	
	    trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;

	    focus = focusZero + focusSlope*trussTemperature;
	    
	    initFocus = focus;
	       
	    taskLog.log(2, CLASS, name, "onSubTaskDone", 
			name+": On completion TrackFOff: Calculated Focus="+focus+" at Alt="+altitude+
			", With new Truss="+trussTemperature);
	    focusTask.setFocus(focus);
	}

    }
    
    @Override
	public void onAborting() {
	super.onAborting();
    }
    
    @Override
	public void onDisposal() {
	super.onDisposal();
    }
    
    @Override
	public void onCompletion() {
	super.onCompletion();	
	taskLog.log(WARNING, 1, CLASS, name, "onCompletion",
		    "Completed telescope autofocus.");	
    }
    
    @Override
	public void preInit() {
	super.preInit();

	try {

	    altitudeError = config.getDoubleValue("altitude.error", DEFAULT_ALTITUDE_ERROR);

	    altitude = config.getDoubleValue("focus.altitude");

	 //    // Dont bother with alt if we are near.
// 	    double currentAltitude = StatusPool.latest().mechanisms.altPos;
// 	    if (Math.abs(currentAltitude - altitude) < altitudeError) {
// 		taskLog.log(2, CLASS, name, "preInit", "Current Alt: "+currentAltitude+
// 			    "- Focus Alt: "+altitude+" < Max Alt.Error: "+altitudeError+
// 			    " Not changing altitude");
// 		doAltitude = false;
// 	    }

	    focusError = config.getDoubleValue("focus.error", DEFAULT_FOCUS_ERROR);
	    
	    double focusLoLimit = config.getDoubleValue("focus.low.limit");
	    double focusHiLimit = config.getDoubleValue("focus.high.limit");

	    focusSlope = config.getDoubleValue("focus.function.slope");
	    focusZero  = config.getDoubleValue("focus.function.zero");
	    
	    focusTravelRate = config.getDoubleValue("focus.travel.rate", 0.1);
	    
	    trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;

	    focus = focusZero + focusSlope*trussTemperature;

	    initFocus = focus;

// 	    if ((focus < focusLoLimit) || (focus > focusHiLimit)) {
// 		taskLog.log(2, CLASS, name, "preInit", 
// 			    "Calculated focus ("+focus+") is outside valid limits: "+
// 			    focusLoLimit+" - "+focusHiLimit);
// 		doFocus = false;
// 	    }

	    double fnow = StatusPool.latest().mechanisms.secMirrorPos;
	    double freq = StatusPool.latest().mechanisms.secMirrorDemand;
	    
	    double ttf = 1000.0*(Math.abs(fnow - freq)/focusTravelRate) + 30000.0;
	    
	    // Dont bother with focus if we are near.
	    if (Math.abs(fnow - focus) < focusError) {
		taskLog.log(2, CLASS, name, "preInit", 
			    name+": PreInit: Focus: "+fnow+" is within "+focusError+" of required: "+focus);
		//doFocus = false;
		// removed temporarily as focus demand and actual are NOT both virtual positions 
		// one is actual other is virtual (cant recall which is which)
	    }

	    taskLog.log(2, CLASS, name, "preInit", 
			name+": PreInit: Calculated Focus="+focus+" at Alt="+altitude+", With Truss="+trussTemperature);
	    	    
	} catch (ParseException px) {
	    taskLog.log(2, CLASS, name, "preInit", 
			"Failed to parse Focussing parameter values from config: "+px);
	    //doFocus = false;
	}

    }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(INFO, 1, CLASS, name, "onInit",
		    "Starting Telescope Autofocus");
    }
    
    /** Creates the TaskList for this TaskManager. */
    @Override
	protected TaskList createTaskList() {

	try {
	    
	    defocusTask = new DefocusTask(name+".DEF00",
					  this,
					  0.0);
	    taskList.addTask(defocusTask);

	 
	    altitudeTask = new AltitudeTask(name+"/GO_ALT",
					    this,
					    Math.toRadians(altitude));
	    taskList.addTask(altitudeTask);
	    
	    // Focus tracking 
	    trackFocusOffTask = new Track_Task(name+"/TRK_FOC_OFF",
					       this,
					       TRACK.FOCUS,
					       TRACK.OFF);
	    trackFocusOffTask.setDelay(10000L);
	    taskList.addTask(trackFocusOffTask);
	    
	    trackFocusOnTask = new Track_Task(name+"/TRK_FOC_ON",
					      this,
					      TRACK.FOCUS,
					      TRACK.ON);
	    trackFocusOnTask.setDelay(5000L);
	    taskList.addTask(trackFocusOnTask);
	    
	    // AGFocus tracking
	    trackAgFocusOffTask = new Track_Task(name+"/TRK_AGFOC_OFF",
						 this,
						 TRACK.AGFOCUS,
						 TRACK.OFF);
	    trackAgFocusOffTask.setDelay(5000L);
	    taskList.addTask(trackAgFocusOffTask);
	    
	    trackAgFocusOnTask = new Track_Task(name+"/TRK_AGFOC_ON",
						this,
						TRACK.AGFOCUS,
						TRACK.ON);
	    trackAgFocusOnTask.setDelay(10000L);
	    taskList.addTask(trackAgFocusOnTask);
	    
	    // Focussing
	    focusTask = new FocusTask(name+"/FOCUS",
				      this,
				      focus);
	    focusTask.setDelay(5000L);
	    taskList.addTask(focusTask);
	 
	    // sequencing
	    taskList.sequence(altitudeTask,        defocusTask);
	    taskList.sequence(defocusTask,         trackFocusOffTask);
	    taskList.sequence(defocusTask,         trackAgFocusOffTask);
	    taskList.sequence(trackFocusOffTask,   focusTask);
	    taskList.sequence(trackAgFocusOffTask, focusTask);
	    taskList.sequence(focusTask,           trackFocusOnTask);		
	    taskList.sequence(focusTask,           trackAgFocusOnTask);
		
	} catch (TaskSequenceException tx) {
  	    errorLog.log(1, CLASS, name, "createTaskList", 
  			 "Failed to create Task Sequence for Autofocus: "+tx);
  	    failed = true;
  	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
  	    errorIndicator.setErrorString("Failed to create Task Sequence for Autofocus.");
  	    errorIndicator.setException(tx);
  	    return null;
  	}
	
	return taskList;
    }
    
}

/** $Log: AutoFocusTask.java,v $
/** Revision 1.1  2007/11/15 11:45:40  snf
/** Initial revision
/**
/** Revision 1.4  2007/10/11 08:15:07  snf
/** addedd tracking for AGfocus in tandem (but slightly delayed) with focus tacking . Agfocus is tracked on after focus tracking and  off before focus tracking is off in both cases by about 5 sec - this may need configurization but will complexify the sequencing decisions which are already a bit manic.
/**
/** Revision 1.3  2007/07/05 11:29:52  snf
/** focus on startup problem
/**
/** Revision 1.2  2007/02/21 19:51:27  snf
/** added defocus 0
/**
/** Revision 1.1  2006/12/12 08:28:54  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:38  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */

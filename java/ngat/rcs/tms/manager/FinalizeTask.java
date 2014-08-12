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
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/** This Task manages the closure of the dome and other mechanisms
 * as a result of an THREAT ALERT received by the RCS Controller.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: FinalizeTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/FinalizeTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class FinalizeTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, FINAL = 1000
	
    int recCount;

    MirrorCover_Task mirrorCoverTask;

    Park_Task        parkTask;

    AzimuthTask      azimuthTask;

    RotatorTask      rotatorTask;

    AgFilterTask     agFilterTask;

    DarkSlideTask    darkSlideTask;

    //TemporaryAutoFlatsTask twiTask;

    double azimuth = 0.0;

    boolean doAzimuth = false;

    double rotAngleMin = 0.0;
    double rotAngleMax = 0.0;
    
    boolean doRotator = false;

    /** ### Temp feature.*/
    //    boolean doTwilight = false;

    boolean doAgFilter = false;

    boolean doDarkSlide = false;

    int std = 0;
  
    static final SimpleDateFormat sd1 = new SimpleDateFormat("yyyyMMdd");
     
    /** Create an FinalizeTask.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public FinalizeTask(String      name,
			TaskManager manager) {
	super(name, manager);	
    }
  
    
    @Override
	public void onSubTaskFailed(Task task) {
	super.onSubTaskFailed(task); 

	if (task instanceof AzimuthTask) {
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
			"Skipping failed azimuth task.");
	} else if
	      (task instanceof RotatorTask) {
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
			"Skipping failed rotator task");	  
	    //	} else if
	    //(task instanceof TemporaryAutoFlatsTask) {
	    //taskList.skip(task);	
	    //taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
	    //	"Skipping failed TempAutoFlats task");	  
	} else if
	      (task instanceof DarkSlideTask) {
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
			"Skipping failed DarkSlide Close task");	  
	} else if
	    (task instanceof AgFilterTask) {
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
			"Skipping failed AgSlide Close task");
	} else if
	      (task instanceof MirrorCover_Task) {
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
			"Skipping failed MirrCover Close task");	    
	} else { 
	    failed(641001, "Temporary fail finalize operation due to subtask failure.."+task.getName(), null);
	}	
    }
    
   
    @Override
	public void onSubTaskAborted(Task task) {
	super.onSubTaskAborted(task);
    }
    
    @Override
	public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);	
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
    }


    @Override
	public void preInit() {
	
	super.preInit();
	
	doAzimuth = (config.getProperty("magic.azimuth") != null);

	if (doAzimuth)
	    azimuth = Math.toRadians(config.getDoubleValue("magic.azimuth", 0.0));

	doRotator = (config.getProperty("magic.rotator") != null);

	if (doRotator) {
	    rotAngleMin = Math.toRadians(config.getDoubleValue("magic.rotator.min", -45.0));
	    rotAngleMax = Math.toRadians(config.getDoubleValue("magic.rotator.max", 45.0));
	}

	//	doTwilight = (config.getProperty("do.twilight.calib", "false").equals("true"));

	doAgFilter  = (config.getProperty("close.ag.filter")  != null);
	doDarkSlide = (config.getProperty("close.dark.slide") != null);
    }
    
    
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(WARNING, 1, CLASS, name, "onInit",
		    "** Starting Final MirrorCover-Close and Park ");
    }
    
    /** Creates the TaskList for this TaskManager. */
    @Override
	protected TaskList createTaskList() {

	// BARRIER #1
	BarrierTask b1 = new BarrierTask(name+"/B1",this);
	taskList.addTask(b1);
	
	// TWILIGHT
	//if (doTwilight) {
	// ### Create a Twilight Task
	//  twiTask = new TemporaryAutoFlatsTask(name+"/TWI_CALIB",
	//this);
    //  taskList.addTask(twiTask); 
	    
    //    try {	
    //	taskList.sequence(twiTask, b1);	
    //    } catch (TaskSequenceException tx) {
    //	errorLog.log(1, CLASS, name, "createTaskList", 
    //		     "Failed to create Task Sequence for FinalizeTask: "+tx);
    //	failed = true;
    //	errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
    //	errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
    //	errorIndicator.setException(tx);
    //	return null;
    //    }	   
	//}
	
	// PARKING
	parkTask        = new Park_Task(name+"/PARK", 
					this, 
					PARK.DEFAULT);
	taskList.addTask(parkTask);
	
	// MIRROR COVER
	mirrorCoverTask = new MirrorCover_Task(name+"/MIR_COVER_CLOSE", 
					       this, 
					       MIRROR_COVER.CLOSE);
	taskList.addTask(mirrorCoverTask);
	try {
	    taskList.sequence(b1, mirrorCoverTask);
	    taskList.sequence(mirrorCoverTask, parkTask);
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList",
			 "Failed to create Task Sequence for FinalizeTask: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
	    errorIndicator.setException(tx);
	    return null;
	}
	
	
	// AGFILTER
	if (doAgFilter) {
            agFilterTask = new AgFilterTask(name+"/AGFILTER_IN",
					    this,
					    AGFILTER.IN);
            taskList.addTask(agFilterTask);

	    try {
		taskList.sequence(b1, agFilterTask);
		taskList.sequence(agFilterTask, parkTask);
	    } catch (TaskSequenceException tx) {
		errorLog.log(1, CLASS, name, "createTaskList",
			     "Failed to create Task Sequence for FinalizeTask: "+tx);
		failed = true;
		errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
		errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
		errorIndicator.setException(tx);
		return null;
	    }
	}
	
	// DARK SLIDE
	if (doDarkSlide) {
            darkSlideTask = new DarkSlideTask(name+"/DARKSLIDE_CLOSE",
					      this,
					      DARKSLIDE.CLOSE);
            taskList.addTask(darkSlideTask);
	    
	    try {
		taskList.sequence(b1, darkSlideTask);
		taskList.sequence(darkSlideTask, parkTask);
	    } catch (TaskSequenceException tx) {
		errorLog.log(1, CLASS, name, "createTaskList",
			     "Failed to create Task Sequence for FinalizeTask: "+tx);
		failed = true;
		errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
		errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
		errorIndicator.setException(tx);
		return null;
	    }
	}
	
	
	// MAGIC AZIMUTH
	if (doAzimuth) {
	    azimuthTask = new AzimuthTask(name+"/MAGIC_AZ", 
					  this, 
					  azimuth);
	    taskList.addTask(azimuthTask);
	    
	    try {
		taskList.sequence(b1, azimuthTask);
		taskList.sequence(azimuthTask, parkTask);
	    } catch (TaskSequenceException tx) {
		errorLog.log(1, CLASS, name, "createTaskList", 
			     "Failed to create Task Sequence for FinalizeTask: "+tx);
		failed = true;
		errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
		errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
		errorIndicator.setException(tx);
		return null;
	    }
	}
	
	// MAGIC ROT
	if (doRotator) {

	    // get a magic rotator angle
	    double magicRotator = rotAngleMin + Math.random()*(rotAngleMax-rotAngleMin);

	    rotatorTask = new RotatorTask(name+"/MAGIC_ROT", 
					  this, 
					  magicRotator,
					  ROTATOR.MOUNT);
	    taskList.addTask(rotatorTask);
	    
	    try {
		taskList.sequence(b1, rotatorTask);
		taskList.sequence(rotatorTask, parkTask);
	    } catch (TaskSequenceException tx) {
		errorLog.log(1, CLASS, name, "createTaskList", 
			     "Failed to create Task Sequence for FinalizeTask: "+tx);
		failed = true;
		errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
		errorIndicator.setErrorString("Failed to create Task Sequence for FinalizeTask.");
		errorIndicator.setException(tx);
		return null;
	    }
	}
	
	return taskList;
    }
    
}

/** $Log: FinalizeTask.java,v $
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

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

/** This Task manages the telescope mechanisms wrap. It may startup an
 * UnwrapTask for either the Azimuth, Rotator drives or both depending
 * on the state of the mechanisms when started.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: UnwrappingTask.java,v 1.3 2007/01/05 11:55:13 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/UnwrappingTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.3 $
 */
public class UnwrappingTask extends ParallelTaskImpl {

    public static final long STATUS_TIMEOUT = 8000L;

    UnwrapTask azimuthTask;
    
    UnwrapTask rotatorTask;

    /** Specifies the time required by the task's manager to perform the observation
     * about to be started. This figure is compared against the available time-to-limits
     * and used to determine whether an Unwrap is required.
     */
    protected long timeRequired;
  
    /** Create an UnwrappingTask using the supplied settings.
     * @param timeRequired The time required by the manager for the following task.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public UnwrappingTask(String      name,
			  TaskManager manager,
			  long        timeRequired) {
	super(name, manager);	
	this.timeRequired = timeRequired;
    }
  
    
    @Override
	public void onSubTaskFailed(Task task) {
	super.onSubTaskFailed(task);
    }
    
    
    @Override
	public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);
    }

    @Override
	public void preInit() {
	super.preInit();
	// Request current time-to-limits status.
	SMM_MonitorClient cLimits  = SMM_Controller.findMonitor(SHOW.LIMITS).requestStatus();

	// Wait till all requests are in or timedout.
	taskLog.log(2, CLASS, "-", "preInit",
		    "Retrieved client for LIMITS, CCT may not be running yet...");
	
        try {
            cLimits.waitFor(STATUS_TIMEOUT);
        } catch (Exception e) {
            taskLog.log(2, CLASS, "-", "preInit",
			"WARNING - Error waiting for results from axis-limit request: "+e);	    
        }

    }

    
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
    }
    
    /** Creates the TaskList for this TaskManager. */
    @Override
	protected TaskList createTaskList() {
	
	System.err.println("UWT:: TimeReqd="+timeRequired);
	// Assume these can be run in parallel for now.
	long timeToAzLimit = (long)(StatusPool.latest().limits.timeToAzLimit*1000.0);
	//System.err.println("UWT:: AZLIMT="+timeToAzLimit);
	if (timeRequired > timeToAzLimit) {
	    taskLog.log(2, CLASS, name, "createTaskList",
			"WARNING:: Time to AZ Limit: "+timeToAzLimit+" millis."+
			"\n\tRequired time:    "+timeRequired+"  millis."+
			"\n\tNOT Creating UNWRAP for AZIMUTH.");
	    //azimuthTask = new UnwrapTask(name+"/UNWRAP_AZ",
	    //			 this,
	    //			 UNWRAP.AZIMUTH);
	    //taskList.addTask(azimuthTask);
	}
	
	long timeToRotLimit = (long)(StatusPool.latest().limits.timeToRotLimit*1000.0);
	//System.err.println("UWT:: ROTLIMT="+timeToRotLimit);
	if (timeRequired > timeToRotLimit) {
	    taskLog.log(2, CLASS, name, "createTaskList",
			"WARNING:: Time to ROT Limit: "+timeToRotLimit+" millis."+
			"\n\tRequired time:    "+timeRequired+"  millis."+
			"\n\tNOT Creating UNWRAP for ROTATOR.");	
	    //rotatorTask = new UnwrapTask(name+"/UNWRAP_ROT",
	    //			 this,
	    //			 UNWRAP.ROTATOR);
	    //taskList.addTask(rotatorTask);
	}
	
	
	return taskList;
    }
    
}

/** $Log: UnwrappingTask.java,v $
/** Revision 1.3  2007/01/05 11:55:13  snf
/** Added some extar logging at Limits client retrieval
/**
/** Revision 1.2  2007/01/05 11:10:04  snf
/** Changed request to limits to fail on any exception
/**
/** Revision 1.1  2006/12/12 08:28:54  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:38  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */

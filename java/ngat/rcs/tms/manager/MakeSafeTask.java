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

/** This Task manages the making-safe of the telescope after some sort of
 * mcp or other suspension of operations..
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: MakeSafeTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/MakeSafeTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class MakeSafeTask extends ParallelTaskImpl {
  
    Park_Task parkTask;
    Track_Task trkAzTask;
    Track_Task trkAltTask;
    Track_Task trkRotTask;
    
    /** Create an MakeSafeTask.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public MakeSafeTask(String      name,
			TaskManager manager) {
	super(name, manager);	
    }
    
    
    @Override
	public void onSubTaskFailed(Task task) {
	super.onSubTaskFailed(task); 
	
	if (task == parkTask) {
	    if (parkTask.getRunCount() < 3) {
		parkTask.setDelay(15000L);
		resetFailedTask(parkTask);
	    } else {
		taskList.skip(task);
	    }
	} else {	
	    taskList.skip(task);	
	    taskLog.log(WARNING, 1, CLASS, name, "onInit",
			"Skipping failed task.");
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
	taskLog.log(INFO, 1, CLASS, name, "onInit",
		    "** Completed Telescope 'Safing' operation");
    }

    @Override
	public void preInit() {	
	super.preInit();	
    }
      
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(WARNING, 1, CLASS, name, "onInit",
		    "** Starting Telescope 'Safing' operation");
    }
    
    /** Creates the TaskList for this TaskManager. 
     * The tasks are all run in parallel but with stepped delays.
     */
    @Override
	protected TaskList createTaskList() {

	parkTask  = new Park_Task(name+"/PARK", 
				  this, 
				  PARK.DEFAULT);
	parkTask.setDelay(25000L);
	taskList.addTask(parkTask);

	trkAzTask = new Track_Task(name+"/TRK_AZ",
				   this,
				   TRACK.AZIMUTH,
				   TRACK.OFF);
	trkAzTask.setDelay(5000L);
	taskList.addTask(trkAzTask);

	trkAltTask = new Track_Task(name+"/TRK_ALT",
				    this,
				    TRACK.ALTITUDE,
				    TRACK.OFF);
	trkAltTask.setDelay(10000L);
	taskList.addTask(trkAltTask);

	trkRotTask = new Track_Task(name+"/TRK_ROT",
				    this,
				    TRACK.ROTATOR,
				    TRACK.OFF);
	trkRotTask.setDelay(15000L);
	taskList.addTask(trkRotTask);
		
	return taskList;
    }
 
}

/** $Log: MakeSafeTask.java,v $
/** Revision 1.1  2006/12/12 08:28:54  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:38  snf
/** Initial revision
/***/

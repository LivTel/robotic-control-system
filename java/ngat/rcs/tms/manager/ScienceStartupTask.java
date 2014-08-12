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

/** This Task manages the startup of Science mode observing.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: ScienceStartupTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/ScienceStartupTask.java,v $
 * </dl>
 * @author $Author $
 * @version $Revision: 1.1 $
 */
public class ScienceStartupTask extends ParallelTaskImpl implements Logging {

    /** Create a ScienceStartupTask using the supplied settings.
     * @param name    The unique name/id for this TaskImpl .
     * @param manager The Task's manager.
     */
    public ScienceStartupTask(String      name,
			      TaskManager manager) {
	super(name, manager);
	
    }

 /** Handle subtask failure.
    ** We ignore failures and just carry on*/
    @Override
	public void onSubTaskFailed(Task task) {
	
	super.onSubTaskFailed(task);
	taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed",
		    "Failure of "+task.getName()+" - IGNORED");
	taskList.skip(task);
	
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
	taskLog.log(WARNING, 1, CLASS, name, "onCompletion",
		    "Completed ScienceStartup.");	
    }
    
    @Override
	public void preInit() {
	super.preInit();
	
	// Options to switch some operations off.


    }


    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(INFO, 1, CLASS, name, "onInit",
		    "Starting ScienceStartup");
	
    }

    /** Create the TaskList. We slew the altitude axis then 
     * just send each rotator slew in sequence with an increasing time offset.
     */
    @Override
	protected TaskList createTaskList() {
  
	StopTask stopAzm = new StopTask(name+"/STOP_AZM", this, STOP.AZIMUTH);
	taskList.addTask(stopAzm);
	StopTask stopAlt = new StopTask(name+"/STOP_ALT", this, STOP.ALTITUDE);
	taskList.addTask(stopAlt);
	StopTask stopRot = new StopTask(name+"/STOP_ROT", this, STOP.ROTATOR);
	taskList.addTask(stopRot);

	Track_Task trackAzm = new Track_Task(name+"/TRK_AZM", this, TRACK.AZIMUTH,  TRACK.ON);
	trackAzm.setDelay(5000L);
	taskList.addTask(trackAzm);
	Track_Task trackAlt = new Track_Task(name+"/TRK_ALT", this, TRACK.ALTITUDE, TRACK.ON);
	trackAlt.setDelay(10000L);
	taskList.addTask(trackAlt);
	Track_Task trackRot = new Track_Task(name+"/TRK_ROT", this, TRACK.ROTATOR,  TRACK.ON);
	trackRot.setDelay(15000L);
	taskList.addTask(trackRot);

	// Do the STOPS then the TRK ONs.
	try {
	    taskList.sequence(stopAzm, trackAzm);
	    taskList.sequence(stopAlt, trackAlt);
	    taskList.sequence(stopRot, trackRot);
	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for ScienceStartup: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for ScienceStartup.");
	    errorIndicator.setException(tx);
	    return null;
	}
       
	return taskList;
    }
    
}

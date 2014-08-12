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
import ngat.tcm.DefaultAutoguiderMonitor;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/**
 * This Task manages a rotator "unsticking" correction.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id:$
 * <dt><b>Source:</b>
 * <dd>$Source:$
 * </dl>
 * 
 * @author $Author:$
 * @version $Revision:$
 */
public class AutoguiderReAcquisitionTask extends ParallelTaskImpl implements Logging {

	/** Time required in total for the observation. */
	private long timeRequired;

	/** How much time (of the required time) has elapsed so far. */
	private long timeElapsed;

	/** Contains the AG configuration information. */
	private TelescopeConfig agConfig;

	private AutoGuide_Task agTask;

	/**
	 * Create an AutoguiderReAcquisitionTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl .
	 * @param manager
	 */
	public AutoguiderReAcquisitionTask(String name, TaskManager manager, long timeRequired, long timeElapsed,
			TelescopeConfig agConfig) {
		super(name, manager);
		this.timeRequired = timeRequired;
		this.timeElapsed = timeElapsed;
		this.agConfig = agConfig;

	}

	@Override
	public void reset() {
		super.reset();

	}

	/**
	 * Handle subtask failure. We ignore failures and just carry on
	 */
	@Override
	public void onSubTaskFailed(Task task) {

		super.onSubTaskFailed(task);

		if (task == agTask) {
			// have another go..
			if (((JMSMA_TaskImpl) task).getRunCount() <= 3) {
				resetFailedTask(task);
			} else {
				// 3 goes - give up and try to continue
				taskList.skip(task);
			}
		} else {

			taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed", "Failure of " + task.getName() + " - IGNORED");
			taskList.skip(task);
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
		taskLog.log(INFO, 1, CLASS, name, "onDisposal", "Resetting autoguider monitor incase another alert occurs");
		DefaultAutoguiderMonitor am = RCS_Controller.controller.getAutoguiderMonitor();
		am.reset();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "Completed autoguider re-acquisition program.");
	}

	@Override
	public void preInit() {
		super.preInit();

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		double elapsedFraction = (double)timeElapsed/(double)timeRequired;
		taskLog.log(INFO, 1, CLASS, name, "onInit", 				
				"Starting autoguider re-acquisition program: Observation time elapsed: "+timeElapsed+
				" of "+timeRequired+
				" ("+(elapsedFraction*100)+"%)");
	}

	/**
	 * Create the TaskList.
	 */
	@Override
	protected TaskList createTaskList() {

		agTask = new AutoGuide_Task(name + "/AUTO_ON", this, agConfig, AUTOGUIDE.ON);

		taskList.addTask(agTask);

		return taskList;
	}

}

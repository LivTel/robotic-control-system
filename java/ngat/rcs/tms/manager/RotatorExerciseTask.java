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

/**
 * This Task manages a program of exercises for the rotator axis.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: RotatorExerciseTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/RotatorExerciseTask.java,v $
 * </dl>
 * 
 * @author $Author $
 * @version $Revision: 1.1 $
 */
public class RotatorExerciseTask extends ParallelTaskImpl implements Logging {

	/** Default value for altitude (degs). */
	public static final double DEFAULT_ALTITUDE = 90.0;

	/** Default value for rotator slew timeout (msec). */
	public static final long DEFAULT_SLEW_TIMEOUT = 60000L;

	/** Default value for slew limit (degs). */
	public static final double DEFAULT_LIMIT = 20.0;

	/** Default value for slew increment (degs). */
	public static final double DEFAULT_INC = 4.0;

	/** Default number of slews to perform. */
	public static final int DEFAULT_COUNT = 10;

	double altitude;
	double limitLeft;
	double limitRight;

	/** Count slews required in total. */
	int slewCount = 0;

	/** Specifies rotator slew timeout. Any slews which fail are just ignored. */
	long slewTimeout;

	/** Count successful slews. */
	int countCompleted = 0;

	AltitudeTask altitudeTask;

	/**
	 * Create an RotatorExerciseTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl .
	 * @param manager
	 *            The Task's manager.
	 */
	public RotatorExerciseTask(String name, TaskManager manager) {
		super(name, manager);

	}

	@Override
	public void reset() {
		super.reset();
		countCompleted = 0;
	}

	/**
	 * Handle subtask failure. We ignore failures and just carry on
	 */
	@Override
	public void onSubTaskFailed(Task task) {

		super.onSubTaskFailed(task);
		taskLog.log(WARNING, 1, CLASS, name, "onSubTaskFailed", "Failure of " + task.getName() + " - IGNORED");
		taskList.skip(task);

	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		countCompleted++;
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
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "Completed Rotator exercise program, Completed "
				+ countCompleted + "/" + slewCount + " slews.");
	}

	@Override
	public void preInit() {
		super.preInit();

		altitude = Math.toRadians(config.getDoubleValue("altitude", DEFAULT_ALTITUDE));

		// NOTE: These are all positive values.
		limitLeft = Math.toRadians(config.getDoubleValue("limit.left", DEFAULT_LIMIT));
		limitRight = Math.toRadians(config.getDoubleValue("limit.right", DEFAULT_LIMIT));
		slewCount = config.getIntValue("slew.count", DEFAULT_COUNT);
		slewTimeout = config.getLongValue("slew.timeout", DEFAULT_SLEW_TIMEOUT);

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(INFO, 1, CLASS, name, "onInit", "Starting Rotator exercise program with " + slewCount
				+ " rotator slews.");

	}

	/**
	 * Create the TaskList. We slew the altitude axis then just send each
	 * rotator slew in sequence with an increasing time offset.
	 */
	@Override
	protected TaskList createTaskList() {
		int il = 0;
		int ir = 0;
		try {

			altitudeTask = new AltitudeTask(name + "/ALT", this, altitude);
			taskList.addTask(altitudeTask);

			Task atask;
			long timeToRun = 0L;
			// Create alternate left and right slews and sequence these.
			for (int is = 0; is <= (slewCount / 2); is++) {

				// Left.
				il++;
				atask = new RotatorTask(name + "/L" + il, this, -limitLeft, ROTATOR.MOUNT);
				timeToRun += slewTimeout;
				atask.setDelay(timeToRun);
				taskList.addTask(atask);
				taskList.sequence(altitudeTask, atask);

				// Right.
				ir++;
				atask = new RotatorTask(name + "/R" + ir, this, limitRight, ROTATOR.MOUNT);
				timeToRun += slewTimeout;
				atask.setDelay(timeToRun);
				taskList.addTask(atask);
				taskList.sequence(altitudeTask, atask);

			}

		} catch (TaskSequenceException tx) {
			errorLog.log(1, CLASS, name, "createTaskList",
					"Failed to create Task Sequence for RotatorExercise program: " + tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for RotatorExercise program.");
			errorIndicator.setException(tx);
			return null;
		}

		return taskList;
	}

}

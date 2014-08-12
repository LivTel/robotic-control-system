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
import ngat.tcm.DefaultTrackingMonitor;
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
public class RotatorCorrectionTask extends ParallelTaskImpl implements Logging {

	public static final double ROT_CORRECTION_DEG = 0.5; // half a degree

	int rotMode;
	double rotAngle;
	double rotActual;
	double rotDemand;

	/** Required correction. */
	private double rotCorrection = ROT_CORRECTION_DEG;

	/** Where we want to back off to. */
	private double rotBackoff;

	/** Where we want to slew to for correction. */
	private double rotTarget;

	/** Backoff rotator. */
	RotatorTask rotBackoffTask;

	/**
	 * Create an RotatorCorrectionTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl .
	 * @param manager
	 *            The Task's manager.
	 * @param rotMode
	 *            The desired rotator mode setting.
	 * @param rotAngle
	 *            The desired angle (if sky-cardinal) otherwise deduced if
	 *            mount.
	 * @param rotActual
	 *            The current rotator (mount) position.
	 * @param rotDemand
	 *            The currrent rotator (mount) demand.
	 */
	public RotatorCorrectionTask(String name, TaskManager manager, int rotMode, double rotAngle, double rotActual,
			double rotDemand) {
		super(name, manager);
		this.rotMode = rotMode;
		this.rotAngle = rotAngle;
		this.rotActual = rotActual;
		this.rotDemand = rotDemand;

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

		if (task == rotBackoffTask) {
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
		taskLog.log(INFO, 1, CLASS, name, "onDisposal", "Resetting tracking monitor incase another alert occurs");
		DefaultTrackingMonitor tm = RCS_Controller.controller.getTrackingMonitor();
		tm.reset();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "Completed Rotator correction program.");
	}

	@Override
	public void preInit() {
		super.preInit();

		rotCorrection = Math.toRadians(config.getDoubleValue("rotator.correction", ROT_CORRECTION_DEG));

		// work out which direction we are going

		if (rotDemand > rotActual) {
			// we are going clockwise.
			rotBackoff = Math.toRadians(rotActual) - rotCorrection;
		} else {
			// we are going ac-wise
			rotBackoff = Math.toRadians(rotActual) + rotCorrection;
		}

		// work out the mode and thence angle requirements for final correction

		if (rotMode == TelescopeConfig.ROTATOR_MODE_MOUNT) {
			// try to get back to previous demand position and hope its close
			// enough
			rotTarget = Math.toRadians(rotDemand);
		} else if (rotMode == TelescopeConfig.ROTATOR_MODE_SKY) {
			// keep the same angle - is it already rads?
			rotTarget = rotAngle;
		}

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(INFO, 1, CLASS, name, "onInit",
				"Starting Rotator correction program with backoff from current position: " + rotActual + " by "
						+ Math.toDegrees(rotCorrection) + " to target " + Math.toDegrees(rotBackoff)
						+ " followed by slew to: " + Math.toDegrees(rotTarget));
	}

	/**
	 * Create the TaskList.
	 */
	@Override
	protected TaskList createTaskList() {

		// what mode are we in ?

		try {

			rotBackoffTask = new RotatorTask(name + "/BACKOFF", this, rotBackoff, ROTATOR.MOUNT);

			taskList.addTask(rotBackoffTask);

			if (rotMode == TelescopeConfig.ROTATOR_MODE_MOUNT) {

				// BACKOFF -> MOUNT -> FLOAT

				RotatorTask rotMountTask = new RotatorTask(name + "/MOUNT_CORR", this, rotTarget, ROTATOR.MOUNT);

				taskList.addTask(rotMountTask);

				RotatorTask rotFloatTask = new RotatorTask(name + "/FLOAT", this, 0.0, ROTATOR.FLOAT);

				taskList.addTask(rotFloatTask);

				taskList.sequence(rotBackoffTask, rotMountTask);
				taskList.sequence(rotMountTask, rotFloatTask);

			} else {
				// BACKOFF -> SKY

				RotatorTask rotSkyTask = new RotatorTask(name + "/SKY_CORR", this, rotTarget, ROTATOR.SKY);

				taskList.addTask(rotSkyTask);
				taskList.sequence(rotBackoffTask, rotSkyTask);

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

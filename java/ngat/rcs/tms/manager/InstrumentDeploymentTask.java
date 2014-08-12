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
import ngat.icm.InstrumentDescriptor;
import ngat.instrument.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/**
 * This Task is responsible for attempting to deploy each of the instruments and
 * to gather information about where the TCS thinks they are (FOLD position) and
 * default focus and rotation offsets.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: InstrumentDeploymentTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/InstrumentDeploymentTask
 * .java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class InstrumentDeploymentTask extends ParallelTaskImpl {

	/** Class name for logs. */
	public static final String CLASS = "InstrumentDeploymentTask";

	/** Timeout (millis) for asynch status requests. */
	public static final long STATUS_TIMEOUT = 5000L;

	int recCount;

	/**
	 * Create an InstrumentDeploymentTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	public InstrumentDeploymentTask(String name, TaskManager manager) {
		super(name, manager);
	}

	/**
	 * For now we just ignore any failures. Probably we should have another go
	 * or 2 then note that the thing is not deployable. (InstRegistry)
	 */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		taskList.skip(task);
	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
	}

	/**
	 * We need to gather some information about the instrument and store it
	 * (InstRegistry).
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		// Which inst was it ?
		String instName = ((InstrumentSelectTask) task).getInstName();
		
		//Instrument inst = Instruments.findInstrument(instName);

		// Ideally we should send SHOW MECH now
		SMM_MonitorClient cMechanisms = SMM_Controller.findMonitor(SHOW.MECHANISMS).requestStatus();
		try {
			cMechanisms.waitFor(STATUS_TIMEOUT);
		} catch (InterruptedException ix) {
		}
		opsLog.log(1, CLASS, name, "onSubTaskDone", "Status results are back or timed out");

		// We want: focus+offset, fold-position for now.

		int fold = StatusPool.latest().mechanisms.foldMirrorPos;
		double focus = StatusPool.latest().mechanisms.secMirrorPos;
		double focusOffset = StatusPool.latest().mechanisms.focusOffset;

		opsLog.log(1, CLASS, name, "onSubTaskDone", "Instrument: " + instName
				+ " Deployed successfully: Default settings" + "\n\tFold:     " + fold + "\n\tFocus:    " + focus
				+ "\n\tF-Offset: " + focusOffset);

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

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		String instName;

		Instrument inst;

		String alias;

		InstrumentSelectTask deployTask;

		Task prevTask = null;

		try{
		List instList = ireg.listInstruments();
		Iterator it = instList.iterator();
		while (it.hasNext()) {
			instName = (String) it.next();
			InstrumentDescriptor iid = new InstrumentDescriptor(instName);
			alias = payload.getAliasForInstrument(iid);
			deployTask = new InstrumentSelectTask(name + "/" + instName + "-SEL", this, instName, alias);
			taskList.addTask(deployTask);

			// Visit each instrument in sequence.
			if (prevTask != null) {
				try {
					taskList.sequence(prevTask, deployTask);
				} catch (TaskSequenceException tx) {
					errorLog.log(1, CLASS, name, "createTaskList",
							"Failed to create Task Sequence for InstrumentDeployment: " + tx);
					failed = true;
					errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
					errorIndicator.setErrorString("Failed to create Task Sequence for InstrumentDeployment.");
					errorIndicator.setException(tx);
					return null;
				}
			}
			prevTask = deployTask;
		}
		} catch (Exception e) {
			e.printStackTrace();
			failed(555667, "No deployable instruments found");
		}
		return taskList;
	}

}

/**
 * $Log: InstrumentDeploymentTask.java,v $ /** Revision 1.1 2006/12/12 08:28:54
 * snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:38 snf /**
 * Initial revision /**
 */

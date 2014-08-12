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
package ngat.rcs.tocs;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;

/**
 * This Task manages the closure of the dome and other mechanisms as a result of
 * an THREAT ALERT received by the RCS Controller.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCInstrumentTask.java,v 1.3 2007/07/05 11:33:49 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCInstrumentTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.3 $
 */
public class TOCInstrumentTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_INSTR = 600
	
	// String instId;

	IInstrumentConfig instConfig;

	/**
	 * Create an FinalizeTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOCInstrumentTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor,
			IInstrumentConfig instConfig) {

		super(name, manager, implementor);
		this.instConfig = instConfig;

	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650601, "Temporary fail TOC Instrument Select operation due to subtask failure.." + task.getName(), null);
		// }
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
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Instrument Selection and Configuration");
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Instrument Selection and Configuration using: "
				+ instConfig);
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {
		taskLog.log(WARNING, 1, CLASS, name, "createTaskList", "Creating ICT using config: " + instConfig);

		InstConfigTask instConfigTask = new InstConfigTask("InstCfg", this, instConfig);
		taskList.addTask(instConfigTask);

		// select aperture for instrument.
		// int number = Instruments.findApertureNumber(instId);

		InstrumentDescriptor iid = new InstrumentDescriptor(instConfig.getInstrumentName());
		try {
			int number = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload()
					.getApertureNumberForInstrument(iid);
			ApertureOffsetTask apertureOffsetTask = new ApertureOffsetTask("AppOff", this, number);

			taskList.addTask(apertureOffsetTask);

			taskLog.log(WARNING, 1, CLASS, name, "createTaskList", "Creating APT using apno: " + number);

			taskList.sequence(instConfigTask, apertureOffsetTask);
		} catch (Exception tx) {
			taskLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for TOCInstrument: " + tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for TOCInstrument.");
			errorIndicator.setException(tx);
			return null;
		}

		return taskList;
	}

}

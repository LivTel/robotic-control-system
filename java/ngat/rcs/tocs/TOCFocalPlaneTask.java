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
 * This Task sends a focal plane / Aperture Offset comamnd to the TCS. This is currently normally sent
 * as part of the TOCInstrumentTask , but we need a separate TOCS command / task for Sprat acquisition.
 * @author cjm
 */
public class TOCFocalPlaneTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_INSTR = 600
	/**
	 * The name of the instrument to send the aperture offset for.
	 */
	protected String instrumentName = null;
	
	/**
	 * Create an TOCFocalPlaneTask.
	 * @param name The unique name/id for this TaskImpl.
	 * @param manager The Task's manager.
	 * @param instrumentName The name of the instrument to send the aperture offset for.
	 * @see #instrumentName
 	 */
	public TOCFocalPlaneTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor,
				 String instrumentName)
	{

		super(name, manager, implementor);
		this.instrumentName = instrumentName;

	}

	@Override
	public void onSubTaskFailed(Task task)
	{
		super.onSubTaskFailed(task);
		failed(651101, "Temporary fail TOC Focal Plane operation due to subtask failure.." + task.getName(), null);
	}

	@Override
	public void onSubTaskAborted(Task task)
	{
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task)
	{
		super.onSubTaskDone(task);
	}

	@Override
	public void onAborting()
	{
		super.onAborting();
	}

	@Override
	public void onDisposal()
	{
		super.onDisposal();
	}

	@Override
	public void onCompletion()
	{
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Focal Plane");
	}

	/** 
	 * Overridden to carry out specific work after the init() method is called. 
	 * @see #instrumentName
	 */
	@Override
	public void onInit()
	{
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Focal Plane for instrument: "
			    + instrumentName);
	}

	/** 
	 * Creates the TaskList for this TaskManager. 
	 * @see #instrumentName
	 */
	@Override
	protected TaskList createTaskList()
	{
		taskLog.log(WARNING, 1, CLASS, name, "createTaskList", "Creating Focal Plane Task using instrument name: " + instrumentName);

		// select aperture for instrument.
		InstrumentDescriptor iid = new InstrumentDescriptor(instrumentName);

		try
		{
			int number = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload()
					.getApertureNumberForInstrument(iid);
			ApertureOffsetTask apertureOffsetTask = new ApertureOffsetTask("AppOff", this, number);

			taskList.addTask(apertureOffsetTask);
			taskLog.log(WARNING, 1, CLASS, name, "createTaskList", "Creating ApertureOffsetTask using apno: " + number);
		}
		catch (Exception tx)
		{
			taskLog.log(1, CLASS, name, "createTaskList", "Failed to create TOCFocalPlane task: " + tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create TOCFocalPlane.");
			errorIndicator.setException(tx);
			return null;
		}
		return taskList;
	}

}

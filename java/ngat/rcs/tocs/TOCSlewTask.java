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

import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.phase2.*;
import ngat.message.RCS_TCS.*;

/**
 * This Task manages a set of Tasks to Slew the telescope axes.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCSlewTask.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCSlewTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TOCSlewTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_SLEW = 900
	
	
	/** Target. */
	ExtraSolarSource source;

	/**
	 * Create a TOInitTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOCSlewTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor, ExtraSolarSource source) {

		super(name, manager, implementor);

		this.source = source;

	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650901, "Temporary fail TOC Slew operation due to subtask failure.." + task.getName(), null);
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
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Slew");
		FITS_HeaderInfo.current_ACQINST.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQIMG.setValue(new String("UNKNOWN"));
		FITS_HeaderInfo.current_ACQMODE.setValue("NONE");
		FITS_HeaderInfo.current_ACQXPIX.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQYPIX.setValue("UNKNOWN");
	}

	@Override
	public void preInit() {

		super.preInit();

		String srcname = (source != null ? source.getName() : "UNKNOWN");
		FITS_HeaderInfo.current_CAT_NAME.setValue(srcname);
		FITS_HeaderInfo.current_OBJECT.setValue(srcname);
		FITS_HeaderInfo.current_PM_RA.setValue(new Double(0.0));
		FITS_HeaderInfo.current_PM_DEC.setValue(new Double(0.0));
		FITS_HeaderInfo.current_PARALLAX.setValue(new Double(0.0));
		FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(2000.0));
		FITS_HeaderInfo.current_CAT_EQUI.setValue(new Double(2000.0));
		FITS_HeaderInfo.current_EQUINOX.setValue(new Double(2000.0));
		FITS_HeaderInfo.current_RADECSYS.setValue("FK5");
		FITS_HeaderInfo.current_RATRACK.setValue(new Double(0.0));
		FITS_HeaderInfo.current_DECTRACK.setValue(new Double(0.0));
				
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Slew");
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		Track_Task trackAzTask = new Track_Task("TrkAzOn", this, TRACK.AZIMUTH, TRACK.ON);
		taskList.addTask(trackAzTask);

		Track_Task trackAltTask = new Track_Task("TrkAltOn", this, TRACK.ALTITUDE, TRACK.ON);
		taskList.addTask(trackAltTask);

		Track_Task trackRotTask = new Track_Task("TrkRotOn", this, TRACK.ROTATOR, TRACK.ON);
		taskList.addTask(trackRotTask);

		SlewTask slewTask = new SlewTask("Slew", this, source);

		taskList.addTask(slewTask);

		try {

			taskList.sequence(trackAzTask, slewTask);
			taskList.sequence(trackAltTask, slewTask);
			taskList.sequence(trackRotTask, slewTask);

		} catch (TaskSequenceException tx) {
			errorLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for TOC Slew: " + tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for TOC Slew.");
			errorIndicator.setException(tx);
			return null;
		}

		return taskList;
	}

}

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

/**
 * This Task manages a set of Tasks to perform focal plane offets.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCOffsetTask.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCOffsetTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TOCOffsetTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_OFF = 700
	
	/** RA offset (rads). */
	double dra;

	/** Dec offset (rads). */
	double ddec;

	/**
	 * Create a TOC OffsetTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOCOffsetTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor, double dra, double ddec) {

		super(name, manager, implementor);

		this.dra = dra;
		this.ddec = ddec;

	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650701, "Temporary fail TOC Offset operation due to subtask failure.." + task.getName(), null);
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
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Offset operation");

		FITS_HeaderInfo.current_ACQINST.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQIMG.setValue(new String("UNKNOWN"));
		FITS_HeaderInfo.current_ACQMODE.setValue("NONE");
		FITS_HeaderInfo.current_ACQXPIX.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQYPIX.setValue("UNKNOWN");
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();

		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Offset operation");
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		Offset_Task offsetTask = new Offset_Task("Offset", this, dra, ddec);

		taskList.addTask(offsetTask);

		return taskList;
	}

}

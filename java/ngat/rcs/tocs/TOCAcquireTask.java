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

/**
 * This Task manages a set of Tasks to perform an exposure.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCAcquireTask.java,v 1.1 2008/03/27 12:52:48 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCAcquireTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TOCAcquireTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_ACQ = 100

	/** Target RA. */
	double ra;

	/** Target Dec. */
	double dec;

	/** The acquisition instrument. */
	String acqInstId;

	/** ACQ pixel offsets. */
	int ax;
	int ay;

	/** Acquisition mode. */
	int acqMode;

	/** record acquire image name. */
	String acqImage;

	/**
	 * Create a TOCAcquireTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOCAcquireTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor, double ra, double dec,
			String acqInstId, int ax, int ay, int acqMode) {

		super(name, manager, implementor);

		this.ra = ra;
		this.dec = dec;
		this.acqInstId = acqInstId;
		this.ax = ax;
		this.ay = ay;
		this.acqMode = acqMode;

	}

	@Override
	public void reset() {
		super.reset();
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650101, "Temporary fail TOC Acquire operation due to subtask failure.." + task.getName(), null);
		// }
	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		acqImage = ((InstrumentAcquireTask) task).getLastAcquireImageFileName();

		// TODO FUDGE, need to change TOC interface
		ISS.setCurrentAcquisitionInstrumentName("RATCAM");
	}

	@Override
	public void onAborting() {
		super.onAborting();
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	/**
	 * Super.onCompletion() sends the message back to the client so we need to
	 * append any extra data here first.
	 */
	@Override
	public void onCompletion() {

		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Acquire");

		// record ACQ flag
		String acqModeStr = "NONE";
		switch (acqMode) {
		case TelescopeConfig.ACQUIRE_MODE_BRIGHTEST:
			acqModeStr = "BRIGHTEST";
			break;
		case TelescopeConfig.ACQUIRE_MODE_WCS:
			acqModeStr = "WCS_FIT";
			break;
		}
		FITS_HeaderInfo.current_ACQINST.setValue(acqInstId);
		FITS_HeaderInfo.current_ACQMODE.setValue(acqModeStr);
		FITS_HeaderInfo.current_ACQXPIX.setValue(new Double(ax));
		FITS_HeaderInfo.current_ACQYPIX.setValue(new Double(ay));
		FITS_HeaderInfo.current_ACQIMG.setValue(acqImage);
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Acquire using: " + acqInstId);
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		// set these up as defaults
		double raRate = 0.0;
		double decRate = 0.0;
		long rateTime = 0L;
		boolean moving = false;
		InstrumentAcquireTask acquireTask = new InstrumentAcquireTask("Acquire", this, acqInstId, ra, dec, moving,
				raRate, decRate, rateTime, ax, ay, acqMode);

		taskList.addTask(acquireTask);

		return taskList;

	}

}

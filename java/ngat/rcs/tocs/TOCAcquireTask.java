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
import ngat.astrometry.Position;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.phase2.*;

/**
 * This Task manages a set of Tasks to perform an acquisition.
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

	/**
	 * How close the target pixel (ax,ay) has to be to the brightest object / 
	 * target RA/Dec for the acquisition to have succeeded. In arcseconds.
	 */
	double acquisitionThreshold;
	
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
			String acqInstId, int ax, int ay, double acquisitionThreshold,int acqMode) {

		super(name, manager, implementor);

		this.ra = ra;
		this.dec = dec;
		this.acqInstId = acqInstId;
		this.ax = ax;
		this.ay = ay;
		this.acquisitionThreshold = acquisitionThreshold;
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
	public void onCompletion() 
	{

		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Acquire");

		// record ACQ flag
		String acqModeStr = "NONE";
		switch (acqMode) 
		{
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

	/** 
	 * Overridden to carry out specific work after the init() method is called. 
	 * Here we need to set the ISS acquisition instrument, and configure the FITS header rotator sky correction,
	 * so that OFFSET_X_Y calls back to the RCS from the acquisition instrument are rotated correctly,
	 * and the FITS headers have the correct rotation so any WCS FITS have the correct input parameters.
	 */
	@Override
	public void onInit() 
	{
		String acqModeStr = null;
		InstrumentCapabilities acap = null;
		InstrumentDescriptor aid = null;
		InstrumentCapabilitiesProvider acp = null;
		
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Acquire using: " + acqInstId);
		switch (acqMode) {
		case TelescopeConfig.ACQUIRE_MODE_BRIGHTEST:
			acqModeStr = "BRIGHTEST";
			break;
		case TelescopeConfig.ACQUIRE_MODE_WCS:
			acqModeStr = "WCS_FIT";
			break;
		}

		try 
		{
			aid = ireg.getDescriptor(acqInstId);
			acp = ireg.getCapabilitiesProvider(aid);
			acap = acp.getCapabilities();
		} 
		catch (Exception e) 
		{
			failed(650102, "Unknown acquisition instrument (" + acqInstId + ") for acquisition");
			return;
		}
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "Configuring ISS current acquisition Instrument to: " + acqInstId);
		ISS.setCurrentAcquisitionInstrumentName(acqInstId);
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "Notified ISS of expected acquire instrument name: " + acqInstId);

		double rotcorr = acap.getRotatorOffset();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "Resetting instrument rotator alignment correction for: " + acqInstId + " to "
				+ Position.toDegrees(rotcorr, 2));

		FITS_HeaderInfo.setRotatorSkyCorrection(rotcorr);

		// set ACQ headers
		FITS_HeaderInfo.current_ACQINST.setValue(acqInstId);
		FITS_HeaderInfo.current_ACQIMG.setValue("NONE");
		FITS_HeaderInfo.current_ACQMODE.setValue(acqModeStr);
		FITS_HeaderInfo.current_ACQXPIX.setValue(new String("NONE"));
		FITS_HeaderInfo.current_ACQYPIX.setValue(new String("NONE"));
		
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
				raRate, decRate, rateTime, ax, ay, acquisitionThreshold, acqMode);

		taskList.addTask(acquireTask);

		return taskList;

	}

}

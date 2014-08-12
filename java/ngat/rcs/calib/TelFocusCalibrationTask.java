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
package ngat.rcs.calib;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.tcm.*;
import ngat.net.*;
import ngat.fits.*;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.instrument.*;
import ngat.astrometry.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

/**
 * This Task creates a series of InstTelFocusCalibTasks to carry out the
 * Telescope Focus calibration.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TelFocusCalibrationTask.java,v 1.4 2008/10/01 15:48:04 eng Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/calib/RCS/TelFocusCalibrationTask.java,v $
 * </dl>
 * 
 * @author $Author: eng $
 * @version $Revision: 1.4 $
 */
public class TelFocusCalibrationTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, CALIB = 70, TEL_FOC = 100

	public static final int CONFIG_ERROR = 670101;

	// RCS_Controller.ERROR_BASE + ERROR_BASE + CONFIG_ERROR;

	public static final int ILLEGAL_SOURCE_ERROR = 670102;

	public static final int TELESCOPE_ERROR = 670103;

	public static final int TELFOCUS_ERROR = 670104;

	public static final int SOURCE_NOT_VISIBLE = 670105;

	public static final int INSTRUMENT_ERROR = 670106;

	/** Calibration info. */
	TelescopeCalibration telCalib;

	/** The minimum start value for the range of Tel-Focus settings to try (mm). */
	protected double focusStart;

	/** The focus increment to use (mm). */
	protected double focusIncrement;

	/** The maximum final value for the range of Tel-Focus settings to try (mm). */
	protected double focusStop;

	/** Required Sig-Noise ratio. */
	protected double signoise;

	/** The required exposure time (millis). */
	protected double exposureTime;

	/** Name of the focus instrument. */
	protected String instrumentName;

	private InstrumentDescriptor instId;

	/** The InstrumentConfig to set up for the calibration run. */
	protected IInstrumentConfig instConfig;

	/** The Source used for the focussing task. */
	protected ExtraSolarSource calibSource;

	/** Focus star catalog. */
	protected Catalog focusStarCatalog;

	/** The magnitude of the calibration source. */
	protected double calibMagnitude;

	protected double elevationLowLimit;

	/** Current focus calculated by INIT procedure (mm). */
	protected double currentFocus;

	// Tasks

	/** Switch azimuth tracking on. */
	protected Track_Task trackOnAzTask;

	/** Switch altitude tracking on. */
	protected Track_Task trackOnAltTask;

	/** Switch rotator tracking on. */
	protected Track_Task trackOnRotTask;

	/** Switch azimuth tracking off. */
	protected Track_Task trackOffAzTask;

	/** Switch altitude tracking off. */
	protected Track_Task trackOffAltTask;

	/** Switch rotator tracking off. */
	protected Track_Task trackOffRotTask;

	/** Slew rotator onto a mount position (usually zero). */
	protected RotatorTask rotMountTask;

	/** Float the rotator at a mount position. */
	protected RotatorTask rotFloatTask;

	/** Slew onto a selected focus star. */
	protected SlewTask slewTask;

	/** COnfigure the focus instrument. */
	protected InstConfigTask instConfigTask;

	/** Tell the instrument to carry out TELFOCUS procedure. */
	protected InstTelFocusTask instTelFocusTask;

	/**
	 * Reset the focus after TELFOCUS, could be either an AutoFocus or just
	 * reFocus using a previously calculated value from INIT procedure.
	 */
	protected Task resetFocusTask;

	/**
	 * Create a TelFocusCalibrationTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this Task.
	 * @param manager
	 *            The Task's manager.
	 * @param focusStarCatalog
	 *            A catalog of stars for focussing on.
	 * @param telCalib
	 *            Calibration configuration.
	 * @param instConfig
	 *            The focus instrument configuration.
	 */
	public TelFocusCalibrationTask(String name, TaskManager manager, Catalog focusStarCatalog,
			TelescopeCalibration telCalib, IInstrumentConfig instConfig) {

		super(name, manager);

		this.telCalib = telCalib;
		this.focusStarCatalog = focusStarCatalog;
		this.instConfig = instConfig;

		focusIncrement = telCalib.getTelfocusFocusStep();
		instrumentName = telCalib.getTelfocusInstrument();
		exposureTime = telCalib.getTelfocusExposureTime();
		instId = new InstrumentDescriptor(instrumentName);

	}

	/**
	 * Overwritten to choose the best calibration source, just before the task
	 * runs.
	 */
	@Override
	public void preInit() {
		super.preInit();

		// check the instrument selection is valid.
		if (instId == null) {
			failed(CONFIG_ERROR, "Telfocus calibration failed due to: No instrument specified");
			return;
		}

		try {
			InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
			InstrumentStatus istat = isp.getStatus();
			if ((!istat.isOnline()) && (!istat.isFunctional())) {
				failed(INSTRUMENT_ERROR, "Focus instrument: " + instId + " is offline or impaired");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			failed(INSTRUMENT_ERROR, "Cannot determine focus instrument status");
			return;
		}
		/*
		 * instrument = Instruments.findInstrument(instId);
		 * 
		 * if (instrument == null) { failed(CONFIG_ERROR,
		 * "Telfocus calibration failed due to: Unknown instrument: "+instId);
		 * return; }
		 * 
		 * // check instrument's current network status if
		 * (instrument.getStatus() == Instrument.NETWORK_STATUS_OFFLINE) {
		 * failed(INSTRUMENT_ERROR, "Focus instrument: "+instId+" is offline");
		 * return; }
		 * 
		 * // check instrument's current op status if
		 * (instrument.getOperationalStatus() ==
		 * Instrument.OPERATIONAL_STATUS_FAIL) { failed(INSTRUMENT_ERROR,
		 * "Focus instrument: "+instId+" is non-operational"); return; }
		 */

		elevationLowLimit = RCS_Controller.getDomelimit();

		// Find the Focus Standard sources.
		calibSource = null;
		List targets = focusStarCatalog.listTargets();
		Iterator it = targets.iterator();
		double highestAltitude = -99.99;
		while (it.hasNext()) {
			ExtraSolarSource src = (ExtraSolarSource) it.next();
			Position target = src.getPosition();
			if (target.getAltitude() < elevationLowLimit)
				continue;
			if (target.getAltitude() > highestAltitude) {
				calibSource = src;
				highestAltitude = target.getAltitude();
			}
		}

		// Selected target should be visible
		if (calibSource == null) {
			failed(SOURCE_NOT_VISIBLE, "All focus calibration targets are too low to observe at this time");
			return;
		}

		Position target = calibSource.getPosition();

		// Work out the exposure time. - NOTE we have faked seeing and skymag
		// ExposureCalculator exposureCalculator =
		// instrument.getExposureCalculator();
		// exposureTime = (int)exposureCalculator.
		// calculateExposureTime(calibMagnitude, signoise, 20.0, 0.7);

		// Calculate focus range from previously calculated focus (INIT
		// procedure).
		currentFocus = InitializeTask.initFocus;

		focusStop = currentFocus + 0.5 * telCalib.getTelfocusFocusRange();
		focusStart = currentFocus - 0.5 * telCalib.getTelfocusFocusRange();

		ISS.setCurrentFocusOffset(0.0);

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
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		if (task instanceof InstTelFocusTask) {
			// failed(TELFOCUS_ERROR,
			// "TelFocus - InstTelfocus failed due to: "+task.getErrorIndicator().getErrorString(),
			// task.getErrorIndicator().getException());

			taskList.skip(task);

			// OR we could try to set a stored default/prev day focus.
			//
			// prevFocus = read prop from $rcs_home/defaults/ key = focus
			// ft = new FocusTask(name, this, focus);
			// tl.addTask(ft);
			// skip(instTelFocusTask);
			// --
			// if this also fails we need to fail horribly....

		} else if (task instanceof InstConfigTask) {
			InstConfigTask iTask = (InstConfigTask) task;
			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleInstConfigTaskFailed", "Task: " + task.getName() + " failed..on run "
					+ runs);
			if (runs < 3) {
				iTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {
				failed(CONFIG_ERROR, "TelFocus - InstConfig failed due to: "
						+ task.getErrorIndicator().getErrorString(), task.getErrorIndicator().getException());
			}

		} else if (task instanceof SlewTask) {
			SlewTask sTask = (SlewTask) task;

			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleSlewTaskFailed", "Task: " + task.getName() + " failed..on run " + runs);
			if (runs < 3) {
				sTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {

				failed(TELESCOPE_ERROR, "TelFocus - Slew failed due to: " + task.getErrorIndicator().getErrorString(),
						task.getErrorIndicator().getException());
			}

		} else if (task == trackOnAzTask) {

			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleTrackOnAzTaskFailed", "Task: " + task.getName() + " failed..on run "
					+ runs);
			if (runs < 3) {
				trackOnAzTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {
				failed(TELESCOPE_ERROR, "TelFocus - Track-ON AZ failed due to: "
						+ task.getErrorIndicator().getErrorString(), task.getErrorIndicator().getException());
			}
		} else if (task == trackOnAltTask) {
			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleTrackOnAltTaskFailed", "Task: " + task.getName() + " failed..on run "
					+ runs);
			if (runs < 3) {
				trackOnAltTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {
				failed(TELESCOPE_ERROR, "TelFocus - Track-ON ALT failed due to: "
						+ task.getErrorIndicator().getErrorString(), task.getErrorIndicator().getException());
			}
		} else if (task instanceof RotatorTask) {
			failed(TELESCOPE_ERROR, "TelFocus - Rotator failed due to: " + task.getErrorIndicator().getErrorString(),
					task.getErrorIndicator().getException());
		} else {
			failed(555, "Temporary fail TELFOCUS operation due to subtask failure.." + task.getName(), null);
		}

	}

	/**  */
	@Override
	public void onAborting() {
		synchronized (taskList) {
			super.onAborting();

			// always try to reset the focus incase this is a minor abort and
			// the scope
			// does not come back up thro INIT

			FocusTask abResetFocusTask = new FocusTask(name + "/TMP_FOCUS", this, InitializeTask.initFocus);
			taskList.addTask(abResetFocusTask);

			Abort_Task abTask = new Abort_Task(name + "-(TelFocusAbort)", this, instId.getInstrumentName());
			abTask.setDelay(5000L);
			taskList.addTask(abTask);

			taskList.addTask(new Track_Task(name + "-(E_TrackAltOff)", this, TRACK.ALTITUDE, TRACK.OFF));
			taskList.addTask(new Track_Task(name + "-(E_TrackAzOff)", this, TRACK.AZIMUTH, TRACK.OFF));
			taskList.addTask(new Track_Task(name + "-(E_TrackRotOff)", this, TRACK.ROTATOR, TRACK.OFF));
		}
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		opsLog.log(1, "Completed Telescope-Focus Calibration.");
	}

	@Override
	public void onFailure() {
		super.onFailure();
		opsLog.log(
				1,
				"Failed Telescope-Focus Calibration." + "\n Code:       " + errorIndicator.getErrorCode()
						+ "\n Reason:     " + errorIndicator.getErrorString() + "\n Exception: "
						+ errorIndicator.getException());
	}

	/**
	 * Overridden to carry out specific work after the TaskList is created.
	 */
	@Override
	public void onInit() {
		super.onInit();

		if (calibSource == null)
			return; // Should have failed anyway.

		taskLog.log(ENTER, 3, CLASS, name, "onInit", "Setting FITS headers.");
		opsLog.log(1, "Starting Telescope-Focus Calibration." + "\n Using Instr:  " + instId + "\n  Start focus: "
				+ focusStart + " mm." + "\n  End Focus:   " + focusStop + " mm." + "\n  Increment:   " + focusIncrement
				+ " mm." + "\n Required SNR: " + signoise + "\n  Expose Time: " + (exposureTime / 1000.0) + " secs."
				+ "\n Calib Source: " + calibSource.getName() + "\n  Magnitude:   " + calibMagnitude
				+ "\n  Altitude:    " + Position.toDegrees(calibSource.getPosition().getAltitude(), 3)
				+ "\n  Azimuth:     " + Position.toDegrees(calibSource.getPosition().getAzimuth(), 3));

		// TODO new logging.

		// START NEW LOGGING WITH CONTEXT

		// logContext.block("onInit").info().level(1).log("Starting Telescope-Focus Calibration").
		// context("instr", instId).
		// context("focus-start", focusStart+" mm").
		// context("focus-end", focusStop+" mm").
		// context("increment", focusIncrement+" mm").
		// context("required-snr", signoise).
		// context("exposure-time", (exposureTime/1000.0)+" secs").
		// context("calib-src", calibSource.getName()).
		// context("calib-mag", calibMagnitude).
		// context("calib-src-alt",
		// Position.toDegrees(calibSource.getPosition().getAltitude(), 3)).
		// context("calib-src-azm",
		// Position.toDegrees(calibSource.getPosition().getAzimuth(),3)).
		// send();

		// END NEW LOGGING WITH CONTEXT

		FITS_HeaderInfo.current_GROUPID.setValue("TEL-FOCUS-GROUP");
		FITS_HeaderInfo.current_OBSID.setValue("TEL-FOCUS-OBS");
		// FITS_HeaderInfo.current_COMPRESS.setValue("NONE");

		FITS_HeaderInfo.current_RADECSYS.setValue("FK5");

		FITS_HeaderInfo.current_EQUINOX.setValue("" + calibSource.getEquinoxLetter() + calibSource.getEquinox());
		FITS_HeaderInfo.current_CAT_RA.setValue(FITS_HeaderInfo.toHMSString(calibSource.getRA()));
		FITS_HeaderInfo.current_CAT_DEC.setValue(FITS_HeaderInfo.toDMSString(calibSource.getDec()));
		FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(calibSource.getEpoch()));
		FITS_HeaderInfo.current_CAT_NAME.setValue(calibSource.getName());
		FITS_HeaderInfo.current_OBJECT.setValue(calibSource.getName());

		FITS_HeaderInfo.current_PM_RA.setValue(new Double(calibSource.getPmRA()));
		FITS_HeaderInfo.current_PM_DEC.setValue(new Double(calibSource.getPmDec()));
		FITS_HeaderInfo.current_PARALLAX.setValue(new Double(calibSource.getParallax()));
		// FITS_HeaderInfo.current_RADVEL = calibSource.getRadialVelocity();
		FITS_HeaderInfo.current_RATRACK.setValue(new Double(0.0));
		FITS_HeaderInfo.current_DECTRACK.setValue(new Double(0.0));

		// ISS_Server.currentTelescopeConfig = teleConfig;
		// taskLog.log(1, CLASS, name, "preInit",
		// "Configured ISS with Current Telescope Config: "+teleConfig);

		// TODO MAYBE ? Setup the Instrument.
		// Save the focus-offset as it will be needed to fix subsequent
		// focus-offsets
		// by subtracting the value used during the Tel-Focus from the
		// observation's
		// requested focus-offset.

		ISS_Server.setExpectTelFocusOffsetSoon(true);
	}

	/**
	 * Creates the TaskList for this TaskManager.
	 */
	@Override
	protected TaskList createTaskList() {

		instConfigTask = new InstConfigTask(name + "/IC", this, instConfig);
		taskList.addTask(instConfigTask);

		// Slew to the Calibration source - fail if none set.
		if (calibSource == null) {
			// Major error !
			errorLog.log(1, CLASS, name, "createTaskList",
					"Failed setting source for Tel-Focus calibration: No suitable source.");
			failed = true;
			errorIndicator.setErrorCode(ILLEGAL_SOURCE_ERROR);
			errorIndicator.setErrorString("Failed setting source for Tel-Focus calibration: No suitable source.");
			return null;
		}

		// Switch Tracking on.
		trackOnAzTask = new Track_Task(name + "/TRK_ON_AZ", this, TRACK.AZIMUTH, TRACK.ON);
		taskList.addTask(trackOnAzTask);

		trackOnAltTask = new Track_Task(name + "/TRK_ON_ALT", this, TRACK.ALTITUDE, TRACK.ON);
		taskList.addTask(trackOnAltTask);

		// trackOnRotTask = new Track_Task(name+"/TRK_ON_ROT",
		// this,
		// TRACK.ROTATOR,
		// TRACK.ON);
		// taskList.addTask(trackOnRotTask);
		// taskList.skip(trackOnRotTask);

		// Slew to calibration source.
		slewTask = new SlewTask(name + "/SLEW", this, calibSource);
		slewTask.setDelay(5000L);
		taskList.addTask(slewTask);

		// BUGBUG Rotator fix
		rotMountTask = new RotatorTask(name + "/ROT_MOUNT", this, 0.0, ROTATOR.MOUNT);
		taskList.addTask(rotMountTask);

		rotFloatTask = new RotatorTask(name + "/ROT_FLOAT", this, 0.0, ROTATOR.FLOAT);
		rotFloatTask.setDelay(5000L);
		taskList.addTask(rotFloatTask);

		// Request instrument to carry out Tel-Focus calibration.
		instTelFocusTask = new InstTelFocusTask(name + "/" + instId + "_FOCUS", this, instId.getInstrumentName(),
				focusStart, focusStop, focusIncrement, (int) exposureTime);
		taskList.addTask(instTelFocusTask);

		// Switch Tracking off.
		trackOffAzTask = new Track_Task(name + "/TRK_OFF_AZ", this, TRACK.AZIMUTH, TRACK.OFF);
		taskList.addTask(trackOffAzTask);
		trackOffAltTask = new Track_Task(name + "/TRK_OFF_ALT", this, TRACK.ALTITUDE, TRACK.OFF);
		taskList.addTask(trackOffAltTask);
		trackOffRotTask = new Track_Task(name + "/TRK_OFF_ROT", this, TRACK.ROTATOR, TRACK.OFF);
		taskList.addTask(trackOffRotTask);

		// Can either do a recalc of focus using Autofocus procedure or reuse
		// previous value
		if (System.getProperty("telfocus.doautofocus") != null) {
			resetFocusTask = new AutoFocusTask(name + "/FINALAUTOFOCUS", this);
			taskList.addTask(resetFocusTask);
		} else {
			resetFocusTask = new FocusTask(name + "/TMP_FOCUS", this, InitializeTask.initFocus);
			taskList.addTask(resetFocusTask);
		}

		// (TkOnAz + TkOnAlt + TkOnRot) & (IC + (Slew & ISel) + DFoc) & ITFocus
		// & (TkOff_Az + TkOff_Alt + TkOff_Rot)
		try {
			taskList.sequence(trackOnAzTask, instConfigTask);
			taskList.sequence(trackOnAzTask, slewTask);
			taskList.sequence(trackOnAzTask, rotMountTask);

			taskList.sequence(trackOnAltTask, instConfigTask);
			taskList.sequence(trackOnAltTask, slewTask);
			taskList.sequence(trackOnAltTask, rotMountTask);

			// taskList.sequence(trackOnRotTask, instConfigTask);
			// taskList.sequence(trackOnRotTask, slewTask);

			// taskList.sequence(instSelectTask, rotMountTask);
			taskList.sequence(rotMountTask, rotFloatTask);

			taskList.sequence(slewTask, instTelFocusTask);
			taskList.sequence(rotFloatTask, instTelFocusTask);
			taskList.sequence(instConfigTask, instTelFocusTask);

			taskList.sequence(instTelFocusTask, trackOffAzTask);
			taskList.sequence(instTelFocusTask, trackOffAltTask);
			taskList.sequence(instTelFocusTask, trackOffRotTask);

			taskList.sequence(trackOffAzTask, resetFocusTask);
			taskList.sequence(trackOffAltTask, resetFocusTask);
			taskList.sequence(trackOffRotTask, resetFocusTask);

		} catch (TaskSequenceException tx) {
			errorLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for Tel-Focus calibration:"
					+ tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for Tel-Focus calibration.");
			errorIndicator.setException(tx);
			return null;
		}

		return taskList;
	}

}

/**
 * $Log: TelFocusCalibrationTask.java,v $ /** Revision 1.4 2008/10/01 15:48:04
 * eng /** added set offset focus to 0 for standard defocus /** /** Revision 1.3
 * 2008/08/21 09:28:18 eng /** added commented out enhanced logging stuff /**
 * /** Revision 1.2 2008/08/21 07:45:05 eng /** checkout /** /** Revision 1.1
 * 2007/10/30 11:36:11 snf /** Initial revision /** /** Revision 1.1 2006/12/12
 * 08:25:56 snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:31:23
 * snf /** Initial revision /** /** Revision 1.1 2002/09/16 09:38:28 snf /**
 * Initial revision /**
 */

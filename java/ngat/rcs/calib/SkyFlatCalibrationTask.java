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
import ngat.sms.GroupItem;
import ngat.tcm.*;
import ngat.icm.*;
import ngat.net.*;
import ngat.fits.*;
import ngat.phase2.*;
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
 * This task performs a SKYFLAT calibration for the specified instrument.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id$
 * <dt><b>Source:</b>
 * <dd>$Source$
 * </dl>
 * 
 * @author $Author$
 * @version $Revision$
 */
public class SkyFlatCalibrationTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, CALIB = 70, SKY_FLAT = 200
	

	public static final int CONFIG_ERROR = 670201;

	public static final int ILLEGAL_SOURCE_ERROR = 670202;

	public static final int TELESCOPE_ERROR = 670203;

	public static final int SKYFLAT_ERROR = 670204;

	public static final int SOURCE_NOT_VISIBLE = 670205;

	/** Calibration info. */
	protected InstrumentCalibration instCalib;

	/** Calibration history. */
	protected InstrumentCalibrationHistory ich;

	/** Name of the focus instrument. */
	protected String instrumentName;

	/** The focus instrument. */
	protected InstrumentDescriptor instId;

	/** The Source used for the flat. */
	protected ExtraSolarSource blankField;

	/** Instrument blank field catalog. */
	protected Catalog blankFieldCatalog;

	/** Beam setup.*/
	protected XBeamSteeringConfig beam;
	
	/** Low elevation limit of scope. */
	protected double elevationLowLimit;

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

	/** Beam setup.*/
		protected BeamSteerExecutiveTask beamTask;
	
	/** Slew onto a selected blank field. */
	protected SlewTask slewTask;

	/** Tell the instrument to carry out SKYFLAT procedure. */
	protected TwilightCalibrationTask instSkyFlatTask;

	/** How long is available to perform this calibration. */
	protected long timeAvailable;

	/** True if its morning (otherwise it should be evening). */
	protected boolean isMorning;

	private GroupItem groupItem;
	
	/**
	 * Create a TelFocusCalibrationTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this Task.
	 * @param manager
	 *            The Task's manager.
	 * @param blankFieldCatalog
	 *            A catalog of blank fields for the instrument (may be NULL).
	 * @param instCalib
	 *            Calibration configuration.
	 * @param instId
	 *            The instrument to use.
	 */
	public SkyFlatCalibrationTask(String name, TaskManager manager, Catalog blankFieldCatalog,
			InstrumentCalibration instCalib, InstrumentCalibrationHistory ich, String instrumentName,
			XBeamSteeringConfig beam, long timeAvailable, boolean isMorning) {

		super(name, manager);

		this.ich = ich;
		this.blankFieldCatalog = blankFieldCatalog;
		this.instCalib = instCalib;
		this.instrumentName = instrumentName;
		this.beam = beam;
		this.timeAvailable = timeAvailable;
		this.isMorning = isMorning;

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
			failed(CONFIG_ERROR, "SkyFlat calibration failed due to: No instrument specified");
			return;
		}
		// instrument = Instruments.findInstrument(instId);

		// if (instrument == null) {
		// failed(CONFIG_ERROR,
		// "SkyFlat calibration failed due to: Unknown instrument: " + instId);
		// return;
		// }

		try {
			InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
		} catch (Exception e) {
			e.printStackTrace();
			failed(CONFIG_ERROR, "SkyFlat calibration failed due to: Unknown instrument: " + instrumentName);
			return;
		}

		elevationLowLimit = RCS_Controller.getDomelimit();

		// Find the Focus Standard sources.
		blankField = null;

		List targets = blankFieldCatalog.listTargets();
		Iterator it = targets.iterator();
		double highestAltitude = -99.99;
		while (it.hasNext()) {
			ExtraSolarSource src = (ExtraSolarSource) it.next();
			Position target = src.getPosition();
			if (target.getAltitude() < elevationLowLimit)
				continue;
			if (target.getAltitude() > highestAltitude) {
				blankField = src;
				highestAltitude = target.getAltitude();
			}
			
			// TODO - here we could check the rotator travel distance and veto if its too far
			
			
			
		}

		// Selected target should be visible
		if (blankField == null) {
			failed(SOURCE_NOT_VISIBLE, "All blank fields are too low to observe at this time");
			return;
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
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		if (task instanceof TwilightCalibrationTask) {
			// this is an awkward one.
			failed(TELESCOPE_ERROR, "SkyFlat::Calib failed due to: " + task.getErrorIndicator().getErrorString(), task
					.getErrorIndicator().getException());

		} else if (task instanceof SlewTask) {
			SlewTask sTask = (SlewTask) task;

			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleSlewTaskFailed", "SkyFlat::Slew failed on run " + runs);
			if (runs < 3) {
				sTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {

				failed(TELESCOPE_ERROR, "SkyFlat::Slew failed due to: " + task.getErrorIndicator().getErrorString(),
						task.getErrorIndicator().getException());
			}

		} else if (task == trackOnAzTask) {

			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleTrackOnAzTaskFailed", "SkyFlat::Track enable AZM failed on run " + runs);
			if (runs < 3) {
				trackOnAzTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {
				failed(TELESCOPE_ERROR, "SkyFlat::Track enable AZM failed due to: "
						+ task.getErrorIndicator().getErrorString(), task.getErrorIndicator().getException());
			}
		} else if (task == trackOnAltTask) {
			int runs = ((JMSMA_TaskImpl) task).getRunCount();
			errorLog.log(1, CLASS, name, "handleTrackOnAltTaskFailed", "SkyFlat::Track enable ALT failed on run "
					+ runs);
			if (runs < 3) {
				trackOnAltTask.setDelay(10000L);
				resetFailedTask(task);
			} else if (runs >= 3) {
				failed(TELESCOPE_ERROR, "SkyFlat::Track enable ALT failed due to: "
						+ task.getErrorIndicator().getErrorString(), task.getErrorIndicator().getException());
			}
		} else if (task instanceof RotatorTask) {
			failed(TELESCOPE_ERROR, "SkyFlat::Rotator failed due to: " + task.getErrorIndicator().getErrorString(),
					task.getErrorIndicator().getException());
		} else {
			failed(555, "SkyFlat operation failed due to: " + task.getErrorIndicator().getErrorString(), task
					.getErrorIndicator().getException());
		}

	}

	/**  */
	@Override
	public void onAborting() {
		synchronized (taskList) {
			super.onAborting();

			Abort_Task abTask = new Abort_Task(name + "-(SkyFlatAbort)", this, instrumentName);
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
		opsLog.log(1, "Completed Instrument-SkyFlat Calibration.");

		// Log the fact its done now...
		long now = System.currentTimeMillis();
		if (isMorning) {
			ich.setLastMorningSkyflatCalibration(now);
			opsLog.log(1, "Completed MORNING SKYFLAT calibration for " + instId + ", updated calibration history");
		} else {
			ich.setLastEveningSkyflatCalibration(now);
			opsLog.log(1, "Completed EVENING SKYFLAT calibration for " + instId + ", updated calibration history");
		}
		RCS_Controller.controller.getGroupOperationsMonitor().notifyListenersGroupCompleted(groupItem, null);

	}

	@Override
	public void onFailure() {
		super.onFailure();
		opsLog.log(
				1,
				"Failed Instrument-SkyFlat Calibration." + "\n Code:       " + errorIndicator.getErrorCode()
						+ "\n Reason:     " + errorIndicator.getErrorString() + "\n Exception: "
						+ errorIndicator.getException());
		XBasicExecutionFailureContext err = new XBasicExecutionFailureContext(errorIndicator.getErrorCode(),
					errorIndicator.getErrorString());
		RCS_Controller.controller.getGroupOperationsMonitor().notifyListenersGroupCompleted(groupItem, err);

	}

	/**
	 * Overridden to carry out specific work after the TaskList is created.
	 */
	@Override
	public void onInit() {
		super.onInit();

		if (blankField == null)
			return; // Should have failed anyway.

		taskLog.log(ENTER, 3, CLASS, name, "onInit", "Setting FITS headers.");
		opsLog.log(
			   1,
			   "Starting Instrument-SkyFlat Calibration." + "\n Using Instr:  " + instrumentName + "\n Blank Field: "
			   + blankField.getName() + "\n  Altitude:    "
			   + Position.toDegrees(blankField.getPosition().getAltitude(), 3) + "\n  Azimuth:     "
			   + Position.toDegrees(blankField.getPosition().getAzimuth(), 3));
		
		FITS_HeaderInfo.current_TELMODE.setValue("CALIBRATION");

		FITS_HeaderInfo.current_TAGID.setValue("CALIB");
		FITS_HeaderInfo.current_USERID.setValue("CALIB");
		FITS_HeaderInfo.current_PROPID.setValue("CALIB");
		FITS_HeaderInfo.current_GROUPID.setValue("FLATS");
		FITS_HeaderInfo.current_OBSID.setValue("SKY_FLAT-OBS");

		//FITS_HeaderInfo.current_COMPRESS.setValue("NONE");

		FITS_HeaderInfo.current_GRPUID.setValue(new Integer(-1));
		FITS_HeaderInfo.current_GRPSEECO.setValue("NONE");
		// TODO should there be other constraints in here ?
		
		FITS_HeaderInfo.current_GRPSKYCO.setValue("NONE");
		FITS_HeaderInfo.current_GRPNUMOB.setValue(new Integer(-1));

		FITS_HeaderInfo.current_GRPTIMNG.setValue("NONE");
		FITS_HeaderInfo.current_GRPMONP.setValue(new Double(0.0));
		FITS_HeaderInfo.current_GRPMONWN.setValue(new Double(0.0));

		FITS_HeaderInfo.current_RADECSYS.setValue("FK5");
		FITS_HeaderInfo.current_EQUINOX.setValue("" + blankField.getEquinoxLetter() + blankField.getEquinox());
		FITS_HeaderInfo.current_CAT_RA.setValue(FITS_HeaderInfo.toHMSString(blankField.getRA()));
		FITS_HeaderInfo.current_CAT_DEC.setValue(FITS_HeaderInfo.toDMSString(blankField.getDec()));
		FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(blankField.getEpoch()));
		FITS_HeaderInfo.current_CAT_NAME.setValue(blankField.getName());
		FITS_HeaderInfo.current_OBJECT.setValue(blankField.getName());
		FITS_HeaderInfo.current_SRCTYPE.setValue("EXTRASOLAR");

		FITS_HeaderInfo.current_PM_RA.setValue(new Double(blankField.getPmRA()));
		FITS_HeaderInfo.current_PM_DEC.setValue(new Double(blankField.getPmDec()));
		FITS_HeaderInfo.current_PARALLAX.setValue(new Double(blankField.getParallax()));
		// FITS_HeaderInfo.current_RADVEL = blankField.getRadialVelocity();
		FITS_HeaderInfo.current_RATRACK.setValue(new Double(0.0));
		FITS_HeaderInfo.current_DECTRACK.setValue(new Double(0.0));

	}

	/**
	 * Creates the TaskList for this TaskManager.
	 */
	@Override
	protected TaskList createTaskList() {

		// Slew to the Calibration source - fail if none set.
		if (blankField == null) {
			// Major error !
			errorLog.log(1, CLASS, name, "createTaskList",
					"Failed setting source for SkyFlat calibration: No suitable source.");
			failed = true;
			errorIndicator.setErrorCode(ILLEGAL_SOURCE_ERROR);
			errorIndicator.setErrorString("Failed setting source for SkyFlat calibration: No suitable source.");
			return null;
		}

		XGroup group = new XGroup();
		group.setName(instId.getInstrumentName() + "_" + (isMorning ? "Morning" : "Evening") + "_Flats");
		// e.g. RATCAM_Morning_Flats IO:O_Evening_Flats

		XIteratorComponent seq = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));

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
		slewTask = new SlewTask(name + "/SLEW", this, blankField);
		slewTask.setDelay(5000L);
		taskList.addTask(slewTask);

		// BUGBUG Rotator fix
		rotMountTask = new RotatorTask(name + "/ROT_MOUNT", this, 0.0, ROTATOR.MOUNT);
		taskList.addTask(rotMountTask);

		rotFloatTask = new RotatorTask(name + "/ROT_FLOAT", this, 0.0, ROTATOR.FLOAT);
		rotFloatTask.setDelay(5000L);
		taskList.addTask(rotFloatTask);

		XExtraSolarTarget xblank = new XExtraSolarTarget(blankField.getName());
		xblank.setRa(blankField.getRA());
		xblank.setDec(blankField.getDec());
		XRotatorConfig xrot = new XRotatorConfig(IRotatorConfig.MOUNT, 0.0);
		seq.addElement(new XExecutiveComponent("X-Slew", new XSlew(xblank, xrot, false)));

		// Beam setup
		if (beam != null) {
			BeamSteerExecutiveTask beamTask = new BeamSteerExecutiveTask(name+"/Beam", this, beam);
			taskList.addTask(beamTask);
			seq.addElement(new XExecutiveComponent("X-Beam", beam));
		}
		
		// Request instrument to carry out SkyFlat calibration.
		instSkyFlatTask = new TwilightCalibrationTask(name + "/" + instId.getInstrumentName() + "_SKYFLAT", this,
				instrumentName, timeAvailable);
		taskList.addTask(instSkyFlatTask);

		// Switch Tracking off.
		trackOffAzTask = new Track_Task(name + "/TRK_OFF_AZ", this, TRACK.AZIMUTH, TRACK.OFF);
		taskList.addTask(trackOffAzTask);
		trackOffAltTask = new Track_Task(name + "/TRK_OFF_ALT", this, TRACK.ALTITUDE, TRACK.OFF);
		taskList.addTask(trackOffAltTask);
		trackOffRotTask = new Track_Task(name + "/TRK_OFF_ROT", this, TRACK.ROTATOR, TRACK.OFF);
		taskList.addTask(trackOffRotTask);

		// (TkOnAz + TkOnAlt + TkOnRot) & (Slew + Rot) & (Beam) & ISkyFlat & (TkOff_Az +
		// TkOff_Alt + TkOff_Rot)
		try {

			taskList.sequence(trackOnAzTask, slewTask);
			taskList.sequence(trackOnAzTask, rotMountTask);

			taskList.sequence(trackOnAltTask, slewTask);
			taskList.sequence(trackOnAltTask, rotMountTask);

			taskList.sequence(rotMountTask, rotFloatTask);
		
			if (beam != null) {
				taskList.sequence(slewTask, beamTask);
				taskList.sequence(rotFloatTask, beamTask);
				taskList.sequence(beamTask, instSkyFlatTask);
			} else {
				taskList.sequence(slewTask, instSkyFlatTask);
				taskList.sequence(rotFloatTask, instSkyFlatTask);
			}
			
			taskList.sequence(instSkyFlatTask, trackOffAzTask);
			taskList.sequence(instSkyFlatTask, trackOffAltTask);
			taskList.sequence(instSkyFlatTask, trackOffRotTask);

		} catch (TaskSequenceException tx) {
			errorLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for Sky-Flat calibration:"
					+ tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for Sky-Flat calibration.");
			errorIndicator.setException(tx);
			return null;
		}

		seq.addElement(new XExecutiveComponent("X-Sky", new XSkyFlat(instId.getInstrumentName())));
		groupItem = new GroupItem(group, seq);
		XTag useTag = new XTag();
		useTag.setName("LTOps");
		XUser usePI = new XUser("LT_RCS");
		XProgram useProgram = new XProgram("Calibration");
		XProposal useProposal = new XProposal("SkyFlats");
		useProposal.setPriority(IProposal.PRIORITY_Z);

		groupItem.setTag(useTag);
		groupItem.setUser(usePI);
		groupItem.setProgram(useProgram);
		groupItem.setProposal(useProposal);

		RCS_Controller.controller.getGroupOperationsMonitor().notifyListenersGroupSelected(groupItem);

		return taskList;
	}

}

/** $Log$ */

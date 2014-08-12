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
import ngat.icm.FocalPlaneOffset;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.instrument.*;
import ngat.tcm.SciencePayload;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;
import java.awt.geom.*;

/**
 * This Task manages the opening of mirror covers and initial focussing at start
 * of night operations.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: InitializeTask.java,v 1.4 2007/10/11 08:15:07 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/InitializeTask.java,v $
 * </dl>
 * 
 * @author $Author $
 * @version $Revision: 1.4 $
 */
public class InitializeTask extends ParallelTaskImpl implements Logging {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, FINAL = 1100
	
	public static final double DEFAULT_ALTITUDE_ERROR = 1.0;

	public static final double DEFAULT_FOCUS_ERROR = 0.05;

	public static final double DEFAULT_AG_FOCUS = 10.2;

	/** Open the Mirror cover. */
	MirrorCover_Task mirrorCoverTask;

	/** Select autoguider. */
	AgSelectTask agSelectTask;

	/** Retract AG Filter. */
	AgFilterTask agFilterTask;

	/** Retract the DarkSlide. */
	DarkSlideTask darkSlideTask;

	/** Select default instrument. */
	InstrumentSelectTask instSelectTask;

	/** Track focus off. */
	Track_Task trackFocusOffTask;

	/** Track focus on. */
	Track_Task trackFocusOnTask;

	/** Track agfocus off. */
	Track_Task trackAgFocusOffTask;

	/** Track agfocus on. */
	Track_Task trackAgFocusOnTask;

	/** AGFocus setting. */
	AgFocusTask agfocusTask;

	/** Shift to altitude. */
	AltitudeTask altitudeTask;

	/** Select focus. */
	FocusTask focusTask;

	/** Offset focus. */
	DefocusTask defocusTask;

	/** Exercise rotator. */
	RotatorExerciseTask rotatorExerciseTask;

	double altitudeError;

	double focusError;

	double altitude;

	double trussTemperature;

	/** Required focus (mm). */
	double focus;

	/** Required agfocus (mm). */
	double agfocus;

	/** Focus function slope. */
	double focusSlope;

	/** Focus fn zero point. */
	double focusZero;

	/** Focus travel rate (mm/sec). */
	double focusTravelRate;

	// Instrument inst;

	String initInstId;

	String instAlias;

	String agId;

	boolean doAltitude;

	boolean doFocus;

	boolean doAgSelect;

	boolean doInstSelect;

	boolean doMirrorOpen;

	boolean doAgFilter;

	boolean doDarkSlide;

	boolean doExercise = false;

	boolean doApertures = false;

	/** TEMP: For use by inssel during MCA mode change. */
	public static double initFocus;

	// InstrumentDeployTestTask ######

	static final SimpleDateFormat sd1 = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Create an InitializeTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl .
	 * @param manager
	 *            The Task's manager.
	 */
	public InitializeTask(String name, TaskManager manager) {
		super(name, manager);

		doAltitude = true;
		doFocus = true;
		doAgSelect = true;
		doAgFilter = false;
		doDarkSlide = false;
		doInstSelect = true;
		doMirrorOpen = true;
		doExercise = false;
		doApertures = false;
	}

	@Override
	public void reset() {
		super.reset();

		doAltitude = true;
		doFocus = true;
		doAgSelect = true;
		doAgFilter = false;
		doDarkSlide = false;
		doInstSelect = true;
		doMirrorOpen = true;
		doExercise = false;
	}

	/** Handle subtask failure. */
	@Override
	public void onSubTaskFailed(Task task) {

		super.onSubTaskFailed(task);

		if (task == instSelectTask) {

			// Compute time to focus
			double fnow = StatusPool.latest().mechanisms.secMirrorPos;
			double freq = StatusPool.latest().mechanisms.secMirrorDemand;

			double ttf = 1000.0 * (Math.abs(fnow - freq) / focusTravelRate) + 30000.0;

			if (((JMSMA_TaskImpl) task).getRunCount() <= 3) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Waiting: " + (ttf / 1000.0)
						+ " secs for SMF to go from: " + fnow + " to " + freq);

				((JMSMA_TaskImpl) task).setDelay((long) ttf);
				resetFailedTask(task);
			} else {

				if (Math.abs(fnow - freq) < focusError) {

					taskLog.log(2, CLASS, name, "onSubTaskFailed", "Inst-Select Focus: " + fnow + ", Focus Expected: "
							+ freq + " within: " + focusError + ", Close enough so carrying on");
					taskList.skip(task);
				} else {
					taskLog
							.log(2, CLASS, name, "onSubTaskFailed",
									"After 4 attempts, cannot determine if instrument is selected, ignore and hope for the best");
					taskList.skip(task);
				}

			}

		} else if (task instanceof AltitudeTask) {

			double currentAltitude = StatusPool.latest().mechanisms.altPos;
			if (Math.abs(currentAltitude - altitude) < altitudeError) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Alt: " + currentAltitude + ", Focus Alt: " + altitude
						+ " within: " + altitudeError + ", Close enough so carrying on");
				taskList.skip(task);

			} else {

				// Not close enough so try again.
				if (((JMSMA_TaskImpl) task).getRunCount() <= 3) {
					taskLog.log(2, CLASS, name, "onSubTaskFailed", "Alt diff is too large, retrying in 10 secs");
					((JMSMA_TaskImpl) task).setDelay(10000L);
					resetFailedTask(task);

				} else {
					taskLog.log(2, CLASS, name, "onSubTaskFailed",
							"After 4 attempts alt diff is still wild, ignore and hope for the best");
					taskList.skip(task);
				}

			}

		} else if (task instanceof Track_Task) {

			if (((JMSMA_TaskImpl) task).getRunCount() < 2) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 10 secs");
				((JMSMA_TaskImpl) task).setDelay(10000L);
				resetFailedTask(task);

			} else {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "After 2 failed attempts, ignore and hope for the best");
				taskList.skip(task);

			}

		} else if (task instanceof DarkSlideTask) {

			if (((JMSMA_TaskImpl) task).getRunCount() < 6) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 10 secs");
				((JMSMA_TaskImpl) task).setDelay(10000L);
				resetFailedTask(task);

			} else {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "After 6 failed attempts, ignore and hope for the best");
				taskList.skip(task);

			}
		} else if (task instanceof DefocusTask) {

			if (((JMSMA_TaskImpl) task).getRunCount() < 6) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 10 secs");
				((JMSMA_TaskImpl) task).setDelay(10000L);
				resetFailedTask(task);

			} else {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "After 6 failed attempts, ignore and hope for the best");
				taskList.skip(task);

			}
		} else if (task instanceof AgFilterTask) {

			int agFilterPos = StatusPool.latest().autoguider.agFilterPos;
			if (agFilterPos == TCS_Status.POSITION_RETRACT) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "AgFilter is " + TCS_Status.codeString(agFilterPos)
						+ ", as required so carrying on");
				taskList.skip(task);
			} else {
				if (((JMSMA_TaskImpl) task).getRunCount() < 6) {

					taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 10 secs");
					((JMSMA_TaskImpl) task).setDelay(10000L);
					resetFailedTask(task);

				} else {

					taskLog.log(2, CLASS, name, "onSubTaskFailed",
							"After 6 failed attempts, ignore and hope for the best");
					taskList.skip(task);
				}
			}

		} else if (task instanceof FocusTask) {

			if (((JMSMA_TaskImpl) task).getRunCount() < 3) {

				FocusTask ftask = (FocusTask) task;
				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 20 secs");

				ftask.setDelay(20000L);
				resetFailedTask(ftask);

				trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;

				focus = focusZero + focusSlope * trussTemperature;

				initFocus = focus;

				taskLog.log(2, CLASS, name, "onSubTaskFailed", name + ": Recalculated after failure: Focus=" + focus
						+ " at Alt=" + altitude + ", With new Truss=" + trussTemperature);

				ftask.setFocus(focus);

			} else {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "After 3 failed attempts, ignore and hope for the best");
				taskList.skip(task);

			}

		} else if (task instanceof AgFocusTask) {

			if (((JMSMA_TaskImpl) task).getRunCount() < 3) {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "Retrying after 10 secs");
				((JMSMA_TaskImpl) task).setDelay(10000L);
				resetFailedTask(task);

			} else {

				taskLog.log(2, CLASS, name, "onSubTaskFailed", "After 3 failed attempts, ignore and hope for the best");
				taskList.skip(task);
			}

		} else if (task instanceof RotatorExerciseTask) {
			taskLog.log(2, CLASS, name, "onSubTaskFailed", "Exercise program failed - ignoring");
			taskList.skip(task);

		} else if (task instanceof InstrumentSetApertureTask) {
			String instName = ((InstrumentSetApertureTask) task).getInstName();
			taskLog.log(2, CLASS, name, "onSubTaskFailed", "Set aperture failed for " + instName + "- ignoring");
			taskList.skip(task);

		} else {
			failed(641101, "Temporary fail INIT operation due to subtask failure.." + task.getName(), null);
		}

	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		if (task == trackFocusOffTask) {

			trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;

			focus = focusZero + focusSlope * trussTemperature;

			initFocus = focus;

			taskLog.log(2, CLASS, name, "onSubTaskDone", name + ": On completion TrackFOff: Calculated Focus=" + focus
					+ " at Alt=" + altitude + ", With new Truss=" + trussTemperature);
			focusTask.setFocus(focus);
		}

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
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "Completed telescope initialization.");
	}

	@Override
	public void preInit() {
		super.preInit();
		try {
			payload = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload();
		} catch (Exception e) {
			e.printStackTrace();
			failed(641102, "Unable to locate science payload");
			return;
		}

		try {
			ireg = RCS_Controller.controller.getInstrumentRegistry();
		} catch (Exception e) {
			e.printStackTrace();
			failed(641103, "Unable to locate instrument registry");
			return;
		}
		try {

			altitudeError = config.getDoubleValue("altitude.error", DEFAULT_ALTITUDE_ERROR);

			altitude = config.getDoubleValue("focus.altitude");

			// Dont bother with alt if we are near.
			double currentAltitude = StatusPool.latest().mechanisms.altPos;
			if (Math.abs(currentAltitude - altitude) < altitudeError) {
				taskLog.log(2, CLASS, name, "preInit", "Current Alt: " + currentAltitude + "- Focus Alt: " + altitude
						+ " < Max Alt.Error: " + altitudeError + " Not changing altitude");
				doAltitude = false;
			}

			focusError = config.getDoubleValue("focus.error", DEFAULT_FOCUS_ERROR);

			double focusLoLimit = config.getDoubleValue("focus.low.limit");
			double focusHiLimit = config.getDoubleValue("focus.high.limit");

			focusSlope = config.getDoubleValue("focus.function.slope");
			focusZero = config.getDoubleValue("focus.function.zero");

			focusTravelRate = config.getDoubleValue("focus.travel.rate", 0.1);

			trussTemperature = StatusPool.latest().meteorology.serrurierTrussTemperature;

			focus = focusZero + focusSlope * trussTemperature;

			initFocus = focus;

			if ((focus < focusLoLimit) || (focus > focusHiLimit)) {
				taskLog.log(2, CLASS, name, "preInit", "Calculated focus (" + focus + ") is outside valid limits: "
						+ focusLoLimit + " - " + focusHiLimit);
				doFocus = false;
			}

			double fnow = StatusPool.latest().mechanisms.secMirrorPos;
			double freq = StatusPool.latest().mechanisms.secMirrorDemand;

			double ttf = 1000.0 * (Math.abs(fnow - freq) / focusTravelRate) + 30000.0;

			// Dont bother with focus if we are near.
			if (Math.abs(fnow - focus) < focusError) {
				taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: Focus: " + fnow + " is within " + focusError
						+ " of required: " + focus);
				// doFocus = false;
				// removed temporarily as focus demand and actual are NOT both
				// virtual positions
				// one is actual other is virtual (cant recall which is which)
			}

			taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: Calculated Focus=" + focus + " at Alt="
					+ altitude + ", With Truss=" + trussTemperature);

		} catch (ParseException px) {
			taskLog.log(2, CLASS, name, "preInit", "Failed to parse Focussing parameter values from config: " + px);
			doFocus = false;
		}

		initInstId = config.getProperty("initial.instrument");

		if (initInstId != null) {
			try {
				InstrumentDescriptor iid = new InstrumentDescriptor(initInstId);
				instAlias = payload.getAliasForInstrument(iid);

				if ((iid == null) || (instAlias == null)) {
					taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: Unable to identify alias for initial instrument ("
							+ initInstId + ") - Using actual name: "+initInstId);
					doInstSelect = true;
					instAlias = initInstId;
				} else {
					taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: Initial instrument is: " + initInstId);
				}
			} catch (Exception e) {
				e.printStackTrace();
				doInstSelect = false;
			}
		} else {
			taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: There is No initial instrument selection");
			doInstSelect = false;
		}

		agfocus = config.getDoubleValue("ag.focus", DEFAULT_AG_FOCUS);
		agId = config.getProperty("initial.autoguider");

		if (agId != null) {
			taskLog.log(2, CLASS, name, "preInit", name + ": PreInit: Initial Autoguider is: " + agId);
			doAgFilter = (config.getProperty("retract.ag.filter") != null);
		} else {
			taskLog.log(2, CLASS, name, "preInit", name + ": PreInit:  There is No initial Autoguider selection");
			doAgSelect = false;
			doAgFilter = false;
		}

		doDarkSlide = (config.getProperty("retract.dark.slide") != null);
		doMirrorOpen = (config.getProperty("open.mirror.cover", "true").equals("true"));

		doExercise = (config.getProperty("exercise.rotator", "false").equals("true"));
		doApertures = (config.getProperty("set.apertures", "false").equals("true"));

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(INFO, 1, CLASS, name, "onInit", "Starting Telescope Initialization");
	}

	/** Creates the TaskList for this TaskManager. */

	// (Mirror_Open + Altitude + InstRAT) & TrackFocusOff & FocusX &
	// TrackFocusOn & Rotex

	@Override
	protected TaskList createTaskList() {

		try {

			// Some barriers (probably too many...
			BarrierTask b1 = new BarrierTask(name + "/B1", this);
			BarrierTask b2 = new BarrierTask(name + "/B2", this);
			BarrierTask b3 = new BarrierTask(name + "/B3", this);
			BarrierTask b4 = new BarrierTask(name + "/B4", this);
			BarrierTask b5 = new BarrierTask(name + "/B5", this);
			taskList.addTask(b1);
			taskList.addTask(b2);
			taskList.addTask(b3);
			taskList.addTask(b4);
			taskList.addTask(b5);

			// Always defocus
			defocusTask = new DefocusTask(name + "/DEF00", this, 0.0);
			taskList.addTask(defocusTask);
			taskList.sequence(b3, defocusTask);
			taskList.sequence(defocusTask, b4);

			// Mirror cover
			if (doMirrorOpen) {
				mirrorCoverTask = new MirrorCover_Task(name + "/MC_OPEN", this, MIRROR_COVER.OPEN);

				taskList.addTask(mirrorCoverTask);
				taskList.sequence(mirrorCoverTask, b1);
			}

			if (doInstSelect) {
				instSelectTask = new InstrumentSelectTask(name + "/INST_SEL", this, initInstId, instAlias);
				taskList.addTask(instSelectTask);

				taskList.sequence(b1, instSelectTask);
				taskList.sequence(instSelectTask, b2);
			} else {
				taskList.sequence(b1, b2);
			}

			if (doAltitude) {
				altitudeTask = new AltitudeTask(name + "/GO_ALT", this, Math.toRadians(altitude));
				taskList.addTask(altitudeTask);

				taskList.sequence(b2, altitudeTask);
				taskList.sequence(altitudeTask, b3);
			} else {
				taskList.sequence(b2, b3);
			}

			if (doFocus) {

				// Focus tracking OFF
				trackFocusOffTask = new Track_Task(name + "/TRK_FOC_OFF", this, TRACK.FOCUS, TRACK.OFF);
				taskList.addTask(trackFocusOffTask);

				// Focussing
				focusTask = new FocusTask(name + "/FOCUS", this, focus);
				focusTask.setDelay(2000L);
				taskList.addTask(focusTask);

				// Focus tracking ON again
				trackFocusOnTask = new Track_Task(name + "/TRK_FOC_ON", this, TRACK.FOCUS, TRACK.ON);
				trackFocusOnTask.setDelay(3000L);
				taskList.addTask(trackFocusOnTask);

				taskList.sequence(trackFocusOffTask, focusTask);
				taskList.sequence(focusTask, trackFocusOnTask);

				// AGFocus tracking OFF
				trackAgFocusOffTask = new Track_Task(name + "/TRK_AGFOC_OFF", this, TRACK.AGFOCUS, TRACK.OFF);
				trackAgFocusOffTask.setDelay(2000L);
				taskList.addTask(trackAgFocusOffTask);

				// AGFocussing
				agfocusTask = new AgFocusTask(name + "/AGFOCUS_SET", this, agfocus);
				taskList.addTask(agfocusTask);

				// AGFocus tracking ON again
				trackAgFocusOnTask = new Track_Task(name + "/TRK_AGFOC_ON", this, TRACK.AGFOCUS, TRACK.ON);
				trackAgFocusOnTask.setDelay(3000L);
				taskList.addTask(trackAgFocusOnTask);

				taskList.sequence(trackAgFocusOffTask, agfocusTask);
				taskList.sequence(agfocusTask, trackAgFocusOnTask);

				// Focus then Agfocus
				taskList.sequence(trackFocusOnTask, trackAgFocusOffTask);
				taskList.sequence(b4, trackFocusOffTask);
				taskList.sequence(trackAgFocusOnTask, b5);

			} else {
				taskList.sequence(b4, b5);
			}

			if (doAgSelect) {
				agSelectTask = new AgSelectTask(name + "/AGSELECT", this, AGSELECT.CASSEGRAIN);
				taskList.addTask(agSelectTask);
				taskList.sequence(b1, agSelectTask);

				// ### create and link the dark slide open in here also
				if (doAgFilter) {
					agFilterTask = new AgFilterTask(name + "/AGFILTER", this, AGFILTER.OUT);
					taskList.addTask(agFilterTask);
					taskList.sequence(agSelectTask, agFilterTask);
					taskList.sequence(agFilterTask, b2);
				} else {
					taskList.sequence(agSelectTask, b2);
				}
			}

			if (doDarkSlide) {
				darkSlideTask = new DarkSlideTask(name + "/DARKSLIDE", this, DARKSLIDE.OPEN);
				taskList.addTask(darkSlideTask);
				taskList.sequence(b1, darkSlideTask);
				taskList.sequence(darkSlideTask, b2);
			}

			// instDeployTask checks all the insts can be deployed and gathers
			// information about them.
			// rotatorExerciseTask carries out configurable exercise program for
			// rotator.

			if (doApertures) {
				// setup instrument apertures.
				try {
					int ii = 0;
					Task lastApTask = null;

					List instList = ireg.listInstruments();
					Iterator iinst = instList.iterator();
					while (iinst.hasNext()) {
						InstrumentDescriptor iid = (InstrumentDescriptor) iinst.next();
						// get this from science payload
						int number = payload.getApertureNumberForInstrument(iid);

						// Point2D offsets =
						// Instruments.findApertureOffset(instId, null);
						// get these from ireg and basic instr
						InstrumentSetApertureTask apTask = null;
						try {
							FocalPlaneOffset aperture = ireg.getCapabilitiesProvider(iid).getCapabilities()
									.getApertureOffset();
							double x = aperture.getFocalPlaneOffsetX();
							double y = aperture.getFocalPlaneOffsetY();
							apTask = new InstrumentSetApertureTask(name + "/" + iid.getInstrumentName() + "_SETAP", this, iid
									.getInstrumentName(), number, x, y);
							taskList.addTask(apTask);
						} catch (Exception e) {
							e.printStackTrace();
							failed(641104, "Unable to locate instrument capabilities provider for: " + iid.getInstrumentName());
							return null;
						}
						if (ii == 0)
							taskList.sequence(b3, apTask);
						else
							taskList.sequence(lastApTask, apTask);
						lastApTask = apTask;
						ii++;

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// exercise the rotator after all else is done...
			if (doExercise) {
				rotatorExerciseTask = new RotatorExerciseTask(name + "/ROTEX", this);
				taskList.addTask(rotatorExerciseTask);
				taskList.sequence(b5, rotatorExerciseTask);
			}

		} catch (TaskSequenceException tx) {
			errorLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for Observation_Sequence: "
					+ tx);
			failed = true;
			errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
			errorIndicator.setErrorString("Failed to create Task Sequence for Telescope_Init_Sequence.");
			errorIndicator.setException(tx);
			return null;
		}

		return taskList;
	}

}

/**
 * $Log: InitializeTask.java,v $ /** Revision 1.4 2007/10/11 08:15:07 snf /**
 * addedd tracking for AGfocus in tandem (but slightly delayed) with focus
 * tacking . Agfocus is tracked on after focus tracking and off before focus
 * tracking is off in both cases by about 5 sec - this may need configurization
 * but will complexify the sequencing decisions which are already a bit manic.
 * /** /** Revision 1.3 2007/07/05 11:29:52 snf /** focus on startup problem /**
 * /** Revision 1.2 2007/02/21 19:51:27 snf /** added defocus 0 /** /** Revision
 * 1.1 2006/12/12 08:28:54 snf /** Initial revision /** /** Revision 1.1
 * 2006/05/17 06:33:38 snf /** Initial revision /** /** Revision 1.1 2002/09/16
 * 09:38:28 snf /** Initial revision /** /** Revision 1.1 2001/04/27 17:14:32
 * snf /** Initial revision /**
 */

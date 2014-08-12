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
package ngat.rcs.sciops;

import ngat.rcs.*;
import ngat.rcs.telemetry.DefaultGroupOperationsMonitor;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.iss.*;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.GroupItem;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.tcm.AutoguiderMonitor;
import ngat.tcm.AutoguiderStatusListener;
import ngat.tcm.TrackingMonitor;
import ngat.tcm.TrackingStatusListener;
import ngat.tcm.BasicTelescopeSystem;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;
import ngat.message.RCS_TCS.*;

import java.util.*;

/**
 * Performs the execution of a group sequence.
 */
public class GroupExecutionTask extends ParallelTaskImpl implements AutoguiderStatusListener, TrackingStatusListener,
		InstrumentStatusListener {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, GRP_EXEC = 900
	
	static final String CLASS = "GroupExec";

	/** The Group to be executed. */
	private GroupItem group;
	
	/** Records measurements of Group's QOS requirements. */
	private Set qosMetrics;

	/** Records resource usage. */
	// private ExecutionResourceBundle erb;

	private Map erb;

	/** The Group's observing sequence. */
	private ISequenceComponent sequence;

	/** Monitors auto-guider lock errors. */
	private AutoguiderMonitor agMonitor;

	/** Monitors tracking errors. */
	private TrackingMonitor trackingMonitor;

	/** Mointors instrument state.*/
	private InstrumentMonitor instMonitor;

	/** Monitors group operations. */
	private DefaultGroupOperationsMonitor gom;

	/** Tracks changes in execution process. */
	private ChangeTracker collator;

	/**
	 * Create a GroupExecutionTask.
	 * 
	 * @param name
	 *            The unique name/id for this Task.
	 * @param manager
	 *            The Task's manager.
	 */
	public GroupExecutionTask(String name, TaskManager manager, GroupItem group) {
		super(name, manager);
		this.group = group;
		sequence = group.getSequence();
		// erb = new ExecutionResourceBundle();
		erb = new HashMap();
		qosMetrics = new TreeSet();

		trackingMonitor = RCS_Controller.controller.getTrackingMonitor();
		agMonitor = RCS_Controller.controller.getAutoguiderMonitor();
		instMonitor = RCS_Controller.controller.getInstrumentMonitor();
		gom = RCS_Controller.controller.getGroupOperationsMonitor();
		collator = new ChangeTracker(group.getName());

		// TODO MAYBE?
		// register with all ISPs as a status listener - we can then see if we
		// loose instrument while exposing.
		// RCS_Controller.controller
		// .getInstrumentRegistry()
		// .getStatusProvider(new InstrumentDescriptor("AAA"))
		// .addInstrumentStatusUpdateListener(this);
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// hopefully this wont happen ?
		if (task instanceof AutoGuide_Task) {
			taskList.skip(task);
		} else {
			failed(task.getErrorIndicator());
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
	public void onAborting() {
		super.onAborting();
		opsLog.log(1, "Aborting Group: Attempting to stop axes");
		StopTask stopAzTask = new StopTask(name + "/ABORT_STOP_AZM", this, STOP.AZIMUTH);
		taskList.addTask(stopAzTask);
		StopTask stopAltTask = new StopTask(name + "/ABORT_STOP_ALT", this, STOP.ALTITUDE);
		taskList.addTask(stopAltTask);
		StopTask stopRotTask = new StopTask(name + "/ABORT_STOP_ROT", this, STOP.ROTATOR);
		taskList.addTask(stopRotTask);

		opsLog.log(1, "Aborting Group: Attempting to stop autoguider");
		AutoGuide_Task agStopTask = new AutoGuide_Task(name + "/ABORT_STOP_AG", this, TelescopeConfig.getDefault(),
				AUTOGUIDE.OFF);
		taskList.addTask(agStopTask);

		IExecutionFailureContext error = new XBasicExecutionFailureContext(abortCode, abortMessage);
		gom.notifyListenersGroupCompleted(group, error);
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
		try {
			trackingMonitor.removeTrackingStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			agMonitor.removeGuideStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			instMonitor.removeInstrumentStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// stop adjustments
		taskLog.log(3, "Stopping tweak offsets");
		try {

		    BasicTelescopeSystem bts = (BasicTelescopeSystem)RCS_Controller.controller.
			getTelescope().getTelescopeSystem();
		    bts.getAdjuster().stopAdjustments();
		} catch (Exception e) {
		    taskLog.log(3, "Unable to stop tweak offsets");
		    e.printStackTrace();
		}


	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		gom.notifyListenersGroupCompleted(group, null);
	}

	@Override
	public void onFailure() {
		super.onFailure();
		int errCode = (errorIndicator != null ? errorIndicator.getErrorCode() : -1);
		String errMsg = (errorIndicator != null ? errorIndicator.getErrorString() : "UNKNOWN");
		IExecutionFailureContext error = new XBasicExecutionFailureContext(errCode, errMsg);
		gom.notifyListenersGroupCompleted(group, error);
	}

	@Override
	public void preInit() {
		super.preInit();
		if (sequence == null)
			failed = true;

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting execution of group: " + group.getName());

		// TEMP display group sequence for execution
		taskLog.log(INFO, 1, CLASS, name, "onInit", DisplaySeq.display(1, sequence));

		// Timing headers.
		FITS_HeaderInfo.fillFitsTimingHeaders(group);

		// Constraints headers.
		FITS_HeaderInfo.fillFitsConstraintHeaders(group);

		// TODO setup any required QOS metric collators here.
		// TODO setup any ERs here for ERB.

		// Identity headers.
		FITS_HeaderInfo.fillIdentHeaders(group);

		// till we know better
		FITS_HeaderInfo.current_USRDEFOC.setValue(new Double(0.0));

		//FITS_HeaderInfo.current_COMPRESS.setValue("PROFESSIONAL");

		// Set AcquImage to Blank as standard - this should be overridden by any
		// actual acquisitions when they run.
		FITS_HeaderInfo.current_ACQIMG.setValue("NONE");

		// TODO This doesnt make sense anymore unless we can count actual
		// exposures
		// i.e. we need to count iterator condition times no of exposes maybe
		// times more iterators
		// FITS_HeaderInfo.current_GRPNUMOB.setValue(new Integer(nseq));

		try {
		    TemporaryResourceUsageEstimator tre = new TemporaryResourceUsageEstimator();
		    
		    ExecutionResourceBundle erb = tre.getEstimatedResourceUsage(group);
		    ExecutionResource erv = erb.getResource("TIME");
		    System.err.printf("Execution resource: %15.15s : %4.2f \n", "TIME",  erv.getResourceUsage());
		    
		    erv = erb.getResource("EXPOSURE");
		    System.err.printf("Execution resource: %15.15s : %4.2f \n", "EXPOSURE",  erv.getResourceUsage());
		    
		    erv = erb.getResource("OBSCOUNT");
		    int obsCount = (int)(erv.getResourceUsage());
		    System.err.printf("Execution resource: %15.15s : %4.2f \n", "OBSCOUNT",  erv.getResourceUsage());
		    
		    erv = erb.getResource("EXPCOUNT");
		    System.err.printf("Execution resource: %15.15s : %4.2f \n", "EXPCOUNT",  erv.getResourceUsage());

		    FITS_HeaderInfo.current_GRPNUMOB.setValue(new Integer(obsCount));
		    // current_GRPSHTIM (EXPOSURE)
		    // current_GRPNUMEX (EXPCOUNT)

		} catch (Exception e) {
		    e.printStackTrace();
		}
		// how long will this group take to execute its root sequence ?
		// ExecutionTimingCalculator execTimingCalc = new
		// ExecutionTimingCalculator();
		ExecutionResourceUsageEstimationModel execTimingCalc = new TestResourceUsageEstimator();
		// long runTime =
		// execTimingCalc.calcExecTimeOfSequence((XIteratorComponent) sequence);
		long runTime = (long) execTimingCalc.getExecTime(sequence);
		FITS_HeaderInfo.current_GRPNOMEX.setValue(new Double(runTime / 1000)); // in
		// seconds

		// Send report to TEA - this will change later

		// register for Tracking alerts - the alert thresholds will have been
		// setup elsewhere hopefully !
		try {
			trackingMonitor.addTrackingStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// register for Auto-guider alerts - the alert thresholds will have been
		// setup elsewhere hopefully !
		try {
			agMonitor.addGuideStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// register for instrument status alerts - the alert thresholds will have been
		// setup elsewhere hopefully !
		try {
			instMonitor.addInstrumentStatusListener(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		gom.notifyListenersGroupSelected(group);
	}

	/**
	 * Creates the TaskList for this TaskManager.
	 */
	@Override
	protected TaskList createTaskList() {

		String strScm = System.getProperty("slew.sequence.control.mode", "normal");

		taskLog.log(WARNING, 1, CLASS, name, "CreateTaskList", "Creating initial tasklist: Sequence control mode: "
				+ strScm);

		// Switch off the autoguider incase it was already on
		TelescopeConfig teleConfig = new TelescopeConfig();
		teleConfig.setAutoGuiderStarSelectionMode(TelescopeConfig.STAR_SELECTION_RANK);
		teleConfig.setAutoGuiderStarSelection1(1);
		AutoGuide_Task autoguideOffTask = new AutoGuide_Task(name + "/PRE_AG_OFF", this, teleConfig, AUTOGUIDE.OFF);
		taskList.addTask(autoguideOffTask);

		// Stop the axes
		StopAxesTask stopAxesTask = new StopAxesTask(name + "/PreStopAxes", this, true, true, true);		
		taskList.addTask(stopAxesTask);		
		
		// Enable axis tracking azm/alt
		EnableAxisTrackingTask enableTrackingTask = null;
		if (strScm.equalsIgnoreCase("normal")) {		
			enableTrackingTask = new EnableAxisTrackingTask(name + "/TrackEnableAzmAlt", this, true, true, false);
			taskList.addTask(enableTrackingTask);
		}

		IteratorControlTask sequenceTask = null;
		if (sequence instanceof XIteratorComponent) {
			XIteratorComponent xiter = (XIteratorComponent) sequence;
			sequenceTask = new IteratorControlTask(name + "/Seq(" + sequence.getComponentName() + ")", this, group,
					xiter, collator);
			taskList.addTask(sequenceTask);

		} else {
			failed(660901, "Invalid root sequence component class: " + sequence.getClass().getName());
		}

		try {
			taskList.sequence(autoguideOffTask, stopAxesTask);
			if (strScm.equalsIgnoreCase("normal")) {
				taskList.sequence(stopAxesTask, enableTrackingTask);
				taskList.sequence(enableTrackingTask, sequenceTask);
			} else {
			    taskList.sequence(stopAxesTask, sequenceTask);
			}
		} catch (TaskSequenceException tsx) {
			failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tsx);
		}
		return taskList;
	}

	/**
	 * @return The group assigned to this task.
	 */
	public GroupItem getGroup() {
		return group;
	}

	/**
	 * @return the set of QOS metrics for this execution.
	 */
	public Set getQosMetrics() {
		return qosMetrics;
	}

	// /**
	// * @return the ExecutionResourceBundle for this execution.
	// */
	// public ExecutionResourceBundle getExecutionResourceBundle() {
	// return erb;
	// }

	/**
	 * @return the ExecutionResourceBundle for this execution.
	 */
	public Map getExecutionResourceBundle() {
		return erb;
	}

	public void guideLockLost() {
		taskLog.log(1, CLASS, name, "guideLockLost", "GXT - received [AG_LOCK_LOST] notification");
		taskLog.log(1, CLASS, name, "guideLockLost", "GXT - Not attempting recovery");
		taskLog.log(1, CLASS, name, "guideLockLost", "GXT - Not attempting to abort");
		
		// TODO this bloack is what we want to use
		/*if (manager instanceof ModalTask) {
			setAbortCode(600221, "AG_LOCK_LOST");
			abort();
		} else {
			((ParallelTaskImpl) manager).setAbortCode(660902, "AG_LOCK_LOST");
			((Task) manager).abort();
		}*/
		
		
		
		// IAutoguiderConfig autoguider = collator.getAutoguide();
		// TelescopeConfig agConfig = new TelescopeConfig();
		// agConfig.setAutoGuiderUsageMode(TelescopeConfig.AGMODE_MANDATORY);
		// agConfig.setAutoGuiderStarSelectionMode(TelescopeConfig.STAR_SELECTION_RANK);
		// agConfig.setAutoGuiderStarSelection1(1);

		// long timeRequired = 1L; // this is meant to be time-elapsed in actual
		// exposure - obviously we dont know
		// could be clever here and decide if its really worth the bother or
		// not...
		// AutoguiderReAcquisitionTask autoguiderReStartTask = new
		// AutoguiderReAcquisitionTask(name + "/AutoReAcquire",
		// this, timeRequired, System.currentTimeMillis() - runStartTime,
		// agConfig);

		// messageQueue.add(new TaskEvent(autoguiderReStartTask,
		// SUBTASK_ADDED));

		// taskList.addTask(autoguiderReStartTask);

		// taskLog.log(1, CLASS, name, "guideLockLost",
		// "GXT - attempting autoguider re-acquisition after lock lost...");

	}

	public void trackingLost() {

		taskLog.log(1, CLASS, name, "trackingLost", "GXT - Axis tracking was lost");
		taskLog.log(1, CLASS, name, "trackingLost", "GXT - Not attempting recovery");
		taskLog.log(1, CLASS, name, "trackingLost", "GXT - Not attempting to abort");

		// Abort the SciopsSeqTask which is manager. it has access to Phase2
		// Updater.
		
		// TODO this bloack is what we want to use
		/*if (manager instanceof ModalTask) {
			setAbortCode(600223, "TRACKING_LOST");
			abort();
		} else {
			((ParallelTaskImpl) manager).setAbortCode(660903, "TRACKING_LOST");
			((Task) manager).abort();
		}*/
		
		
		
		// IRotatorConfig rotator = collator.getRotator();
		// System.err.println("GXT::Collator has rot config: " + rotator);
		//
		// if (rotator == null) {
		// taskLog
		// .log(1, CLASS, name, "trackingLost",
		// "Rotator tracking was lost, unable to determine previous rotator settings, not attempting recovery");
		// failed(555777,
		// "Rotator tracking was lost, unable to determine previous rotator settings");
		// return;
		// }
		//
		// int rotMode = TelescopeConfig.ROTATOR_MODE_SKY;
		// int rotNewMode = rotator.getRotatorMode();
		// switch (rotNewMode) {
		// case IRotatorConfig.CARDINAL:
		// rotMode = TelescopeConfig.ROTATOR_MODE_SKY;
		// break;
		// case IRotatorConfig.MOUNT:
		// rotMode = TelescopeConfig.ROTATOR_MODE_MOUNT;
		// break;
		// default:
		// String rotModeStr = XRotatorConfig.getRotatorModeName(rotNewMode);
		// failed(555777, "Rotator tracking was lost, unknown rotator mode: " +
		// rotModeStr);
		// return;
		// }
		//
		// double rotAngle = rotator.getRotatorAngle();
		//
		// double rotActual = StatusPool.latest().mechanisms.rotPos;
		// double rotDemand = StatusPool.latest().mechanisms.rotDemand;
		//		
		// RotatorCorrectionTask rotCorrTask = new RotatorCorrectionTask(name +
		// "/RotatorCorrection", this, rotMode, rotAngle,
		// rotActual, rotDemand);
		//
		// taskLog.log(1, CLASS, name, "trackingLost",
		// "Attempting rotator correction after tracking lost: RotMode: "
		// + XRotatorConfig.getRotatorModeName(rotNewMode)
		// + " RotSkyAngle: " + Position.toDegrees(rotAngle, 2)
		// + " RotMountDemand: " + Position.toDegrees(rotDemand, 2)
		// + " RotMountActual: " + Position.toDegrees(rotActual, 2));
		//
		// //taskLog.log(1, CLASS, name, "trackingLost",
		// "GXT - Invoke failure");
		// //failed(555777, "Tracking was lost - not attempting recovery");
		//
		// taskList.addTask(rotCorrTask);
		//
		// taskLog.log(1, CLASS, name, "trackingLost",
		// "Attempting rotator correction after tracking lost...");

	}

	public void instrumentLost(InstrumentDescriptor instId) {
		// TODO Auto-generated method stub
		taskLog.log(1, CLASS, name, "instrumentOffline", "GXT - Instrument ["+instId.getInstrumentName()+"] went offline");
		taskLog.log(1, CLASS, name, "instrumentOffline", "GXT - Not attempting recovery");
		String instName = collator.getInstrumentName();
		taskLog.log(1, CLASS, name, "instrumentOffline", "GXT - Current instrument: "+instName);
		
		if (instName.equals(instId.getInstrumentName())) {					
			taskLog.log(1, CLASS, name, "instrumentOffline", "GXT - Not attempting to abort due current inst offline");
		} else {
			taskLog.log(1, CLASS, name, "instrumentOffline", "GXT - No action as instrument not in use");
			return;
		}
		
		
		// Abort the SciopsSeqTask which is manager. it has access to Phase2
		// Updater.
		// TODO this bloack is what we want to use
		/*if (manager instanceof ModalTask) {
			setAbortCode(600224, "INSTRUMENT_LOST_"+instId.getInstrumentName());
			abort();
		} else {
			((ParallelTaskImpl) manager).setAbortCode(660904, "INSTRUMENT_LOST_"+instId.getInstrumentName());
			((Task) manager).abort();
		}*/
	}

}

/**
 * 
 */
package ngat.rcs.sciops;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatusProvider;
import ngat.phase2.IApertureConfig;
import ngat.phase2.XPositionOffset;
import ngat.rcs.RCS_Controller;
import ngat.rcs.scm.collation.StatusPool;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.ApertureOffsetTask;
import ngat.rcs.tms.executive.TweakTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.tcm.BasicTelescopeAlignmentAdjuster;
import ngat.tcm.BasicTelescopeSystem;

/**
 * @author eng
 * 
 */
public class ApertureTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, ACQ = 1800
	
	/** Aperture configuration.*/
	private IApertureConfig apertureConfig;
	
	/** Keeps track of changes. */
	private ChangeTracker collator;

	/** Tweak X offset (asec). */
	private double tweakOffsetX;

	/** Tweak Y offset (asec). */
	private double tweakOffsetY;
	
	private String targetInstName;
	
	/**
	 * @param name
	 * @param manager
	 */
	public ApertureTask(String name, TaskManager manager, IApertureConfig apertureConfig, ChangeTracker collator) {
		super(name, manager);
		this.apertureConfig = apertureConfig;
		this.collator = collator;
	}

	@Override
	public void preInit() {
		super.preInit();
		
		// TODO targetInstName = apertureConfig.getApertureInstrument()

		taskLog.log(1, "Starting focal plane offset, applying offsets for: " + targetInstName);

		// obtain the current rotator position and add a tweak here
		double rotatorPosition = StatusPool.latest().mechanisms.rotPos;

		BasicTelescopeSystem bts = null;
		BasicTelescopeAlignmentAdjuster bta;
	
		try {
			bts = (BasicTelescopeSystem) RCS_Controller.controller.getTelescope().getTelescopeSystem();
		} catch (Exception e) {
			failed = true;

			failed(661801, "Unable to locate telescope system", e);
			return;
		}

		try {
			bta = bts.getAdjuster();
		} catch (Exception e) {
			failed(661802, "Unable to locate telescope alignment adjuster", e);
			return;
		}
		try {
			// rotator is in degrees
			XPositionOffset offset = bta.interpolate(rotatorPosition);
			// these are in arcsecs
			tweakOffsetX = offset.getRAOffset();
			tweakOffsetY = offset.getDecOffset();
			taskLog.log(1, CLASS, name, "onInit", "Tweak offsets for rotator: " + rotatorPosition + "degs are: x: "
					+ tweakOffsetX + " asec, y: " + tweakOffsetY + " asec");
			// these are still in arcsecs here
		} catch (Exception e) {
			failed(661803, "Unable to determine valid alignment solution", e);
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {

		// TODO String targetInstrumentName = apertureConfig.getApertureInstrument();
		String targetInstrumentName = null; // REMOVE
		if (targetInstrumentName == null || targetInstrumentName.equals("")) {
			failed(661804, "No target instrument specified for aperture");
			return null;
		}

		InstrumentDescriptor tid = null;
		try {
			tid = ireg.getDescriptor(targetInstrumentName);
		} catch (Exception e) {
			failed(661805, "Failed to locate descriptor for target instrument (" + targetInstrumentName
					+ ") for aperture");
			return null;
		}

		InstrumentStatusProvider tsp = null;
		try {
			tsp = ireg.getStatusProvider(tid);
			if (tsp == null) {
				failed(661806, "Unknown target instrument (" + targetInstrumentName + ") for aperture");
				return null;
			}
		} catch (Exception e) {
			failed(661807, "Unknown target instrument (" + targetInstrumentName + ") for aperture");
			return null;
		}

		int apNumber = payload.getApertureNumberForInstrument(tid);

		ApertureOffsetTask apertureTask = new ApertureOffsetTask(name + "/AP_" + targetInstrumentName + "(" + apNumber
				+ ")", this, apNumber);
		taskList.addTask(apertureTask);

		// convert the offsets to rads first
		TweakTask tweakTask = new TweakTask(name + "/TWEAK(" + tweakOffsetX + "," + tweakOffsetY + ")", this,
				Math.toRadians(tweakOffsetX / 3600.0), Math.toRadians(tweakOffsetY / 3600.0));

		// they will be converted back to arcsecs by the command sender !
		taskList.addTask(tweakTask);

		try {
			taskList.sequence(apertureTask, tweakTask);
		} catch (TaskSequenceException tse) {
			failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error: " + tse);
			return null;
		}

		return taskList;

	}
	
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Aperture offset failure");	
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onCompletion()
	 */
	@Override
	public void onCompletion() {		
		super.onCompletion();
		collator.setApertureInstrument(targetInstName);
	}
	
	
	
}

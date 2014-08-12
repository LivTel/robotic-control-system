/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.ITarget;
import ngat.phase2.Source;
import ngat.phase2.XSlew;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.iss.ISS;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.SlewTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class TargettingControlTask2 extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, TARG_CTRL2 = 1700
	
	/** A slew action - selects target, rotator setting and tracking mode. */
	private XSlew slew;

	/** The target. */
	private ITarget target;

	/** The tracking mode. */
	private boolean nstrack;

	/** Collates changes. */
	private ChangeTracker collator;

	/**
	 * Time this task would have been created ie when the enclosing iterator
	 * started.
	 */
	private long iteratorStartTime;

	/**
	 * Create a task to perform slew on all axes and set tracking mode.
	 * 
	 * @param name
	 *            Name of the Task.
	 * @param manager
	 *            Manager of task.
	 * @param slew
	 *            A slew object (contains target, rotator setting and tracking
	 *            mode.
	 * @param collator
	 *            Keeps track of changes to date.
	 * @param runTime
	 *            How long are we expecting to track the target.
	 */
	public TargettingControlTask2(String name, TaskManager manager, XSlew slew, ChangeTracker collator) {
		super(name, manager);
		this.slew = slew;
		this.collator = collator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onCompletion()
	 */
	@Override
	public void onCompletion() {
		super.onCompletion();
		collator.setLastTarget(target);
		collator.setNonSiderealTracking(nstrack);
		collator.clearOffset();
		ISS.setUserOffsets(0.0, 0.0);
		collator.setAcquired(false); // we have definitely lost this
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onInit()
	 */
	@Override
	public void onInit() {
		// TODO Auto-generated method stub
		super.onInit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		super.preInit();

		target = slew.getTarget();
		nstrack = slew.usesNonSiderealTracking();

		if (target == null) {
			failed(661701, "No target specified for Slew");
			return;
		}
		FITS_HeaderInfo.fillFitsTargetHeaders(target);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskDone(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		if (task instanceof SlewTask) {
			collator.setLastTarget(target);
			collator.setNonSiderealTracking(nstrack);
			collator.clearOffset();
			ISS.setUserOffsets(0.0, 0.0);
			collator.setAcquired(false); // we have definitely lost this
			collator.clearApertureInstrument();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskFailed(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		failed(err.getErrorCode(), "Slew failure");
	}

	@Override
	protected TaskList createTaskList() {

		// this will be the old-style source we want...
		Source source = null;

		try {
			source = TargetTranslator.translateToOldStyleSource(target);
		} catch (TargetTranslationException tx) {
			failed(661702, "TargetTranslator failed: " + tx);
			return null;
		}
		
		EnableAxisTrackingTask enableTrackingTask = new EnableAxisTrackingTask(name + "/TrackEnableAzmAlt", this,
				true, true, false);
				taskList.addTask(enableTrackingTask);
		SlewTask slewTask = new SlewTask(name + "/SLEW(" + source.getName() + ")", this, source);
		slewTask.setNsTracking(nstrack);
		slewTask.setDelay(1000L);
		taskList.addTask(slewTask);
		
		try {
			 taskList.sequence(enableTrackingTask, slewTask);		
		} catch (TaskSequenceException tsx) {
			failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tsx);
		}
		return taskList;
	}

}

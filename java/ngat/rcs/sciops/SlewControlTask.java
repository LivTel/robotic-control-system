/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.IRotatorConfig;
import ngat.phase2.ITarget;
import ngat.phase2.XSlew;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class SlewControlTask extends ParallelTaskImpl {

	/** A slew action - selects target, rotator setting and tracking mode. */
	private XSlew slew;

	/** The target. */
	private ITarget target;

	/** Tracks changes. */
	private ChangeTracker collator;
	private ChangeTracker subtracker;

	/** The rotator setting. */
	private IRotatorConfig rotator;

	// TODO this is currently an OVER-ESTIMATE
	/** How long we expect to be tracking the target for. */
	private long runTime;

	/**
	 * Time this task would have been created ie when the enclosing iterator
	 * started.
	 */
	private long iteratorStartTime;

	/**
	 * Create a SlewControlTask to handle the slew and rotation.
	 * 
	 * @param name
	 *            The name of this task.
	 * @param manager
	 *            The manager.
	 * @param slew
	 *            Slew information.
	 * @param collator
	 *            Tracks changes.
	 */
	public SlewControlTask(String name, TaskManager manager, XSlew slew, ChangeTracker collator, long runTime) {
		super(name, manager);
		this.slew = slew;
		this.collator = collator;
		this.runTime = runTime;
		iteratorStartTime = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {
		
		subtracker = collator.clone("subtracker");
		subtracker.setLastTarget(target);
		
		StopAxesTask stopAxesTask = new StopAxesTask(name+"/PreStopAxes", this, true, true, true);
		taskList.addTask(stopAxesTask);
		
		TargettingControlTask2 targettingControlTask2 = new TargettingControlTask2(name + "/Targetting(TCT2)", this, slew,
				collator);
		targettingControlTask2.setDelay(2000L);
		taskList.addTask(targettingControlTask2);
		
		RotatorControlTask rotatorControlTask = new RotatorControlTask(name + "/Rotate", this, rotator, subtracker,
				runTime);
		rotatorControlTask.setDelay(2000L);
		taskList.addTask(rotatorControlTask);
		
		try {
			taskList.sequence(stopAxesTask, targettingControlTask2);
			taskList.sequence(stopAxesTask, rotatorControlTask);
		} catch (TaskSequenceException tsx) {
			failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tsx);
		}
		
		return taskList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskDone(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskDone(Task task) {
		// TODO Auto-generated method stub
		super.onSubTaskDone(task); 
		if (task instanceof RotatorControlTask) {
			// collate subtracker rotator info to master
			if (subtracker != null)
				collator.setRotator(subtracker.getRotator());
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
		// TODO Auto-generated method stub
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();	
		failed(err.getErrorCode(), "Slew failure");
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
		rotator = slew.getRotatorConfig();

		long timeSinceStart = System.currentTimeMillis() - iteratorStartTime;
		long timeToRun = iteratorStartTime + runTime - System.currentTimeMillis();
		taskLog.log(3, String.format("Initializing slew. Enclosing iterator started at: %tF %tT, (%4d s ago), Total Exec time: %4d s, Time to run: %4d s",
				iteratorStartTime, iteratorStartTime, (timeSinceStart / 1000),
				(runTime / 1000), (timeToRun / 1000)));

	}

}

/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.XBeamSteeringConfig;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.BeamSteerExecutiveTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 *
 */
public class BeamSteeringTask extends ParallelTaskImpl {

	private XBeamSteeringConfig beamConfig;
	
	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public BeamSteeringTask(String name, TaskManager manager, XBeamSteeringConfig beamConfig, ChangeTracker collator) {
		super(name, manager);
		this.beamConfig = beamConfig;
		this.collator = collator;
	}
	
	@Override
	protected TaskList createTaskList() {

		//IBeamSteeringConfig currentBeamConfig = collator.get
	
		BeamSteerExecutiveTask beamExecutiveTask = new BeamSteerExecutiveTask(name+"/BEAM", this, beamConfig);
		taskList.addTask(beamExecutiveTask);

		return taskList;
	}
	
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		 //collator.applyBeamConfig(..)
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Beam steering failure");	
	}
	
	
}

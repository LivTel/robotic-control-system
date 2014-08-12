/**
 * 
 */
package ngat.rcs.sciops;

import ngat.message.RCS_TCS.TRACK;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Default_TaskImpl;
import ngat.rcs.tms.executive.Track_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 *
 */
public class DisableAxisTrackingTask extends ParallelTaskImpl {
	private boolean enableAzm;
	private boolean enableAlt;
	private boolean enableRot;
	
	
	/**
	 * @param name
	 * @param manager
	 * @param enableAzm
	 * @param enableAlt
	 * @param enableRot
	 */
	public DisableAxisTrackingTask(String name, TaskManager manager, boolean enableAzm, boolean enableAlt, boolean enableRot) {
		super(name, manager);
		this.enableAzm = enableAzm;
		this.enableAlt = enableAlt;
		this.enableRot = enableRot;
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {
		
		if (enableAzm) {
		Track_Task trackAzimuthOnTask = new Track_Task(name+"/TrackAzmOn", this,TRACK.AZIMUTH, TRACK.ON);
		taskList.addTask(trackAzimuthOnTask);
		}
		
		if (enableAlt) {
		Track_Task trackAltitudeOnTask = new Track_Task(name+"/TrackAltOn", this,TRACK.ALTITUDE, TRACK.ON);
		taskList.addTask(trackAltitudeOnTask);
		}
		
		if (enableRot) {
		Track_Task trackRotatorOnTask = new Track_Task(name+"/TrackRotOn", this,TRACK.ROTATOR, TRACK.ON);
		taskList.addTask(trackRotatorOnTask);
		}
		
		return taskList;
	}
	
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(2, "Disabling axis tracking for: "+
				(enableAzm ? "AZM ": "")+
				(enableAlt ? "ALT ": "")+
				(enableRot ? "ROT": ""));
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskFailed(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskFailed(Task task) {	
		super.onSubTaskFailed(task);
		
		if (((Default_TaskImpl)task).getRunCount() < 2) {
			resetFailedTask(task);
		} else {		
			ErrorIndicator err = task.getErrorIndicator();
			failed(err.getErrorCode(), "Axis track disable failure");		
		}
	}
	
}

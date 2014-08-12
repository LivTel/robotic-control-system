/**
 * 
 */
package ngat.rcs.sciops;

import ngat.message.RCS_TCS.STOP;
import ngat.message.RCS_TCS.TRACK;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.Default_TaskImpl;
import ngat.rcs.tms.executive.StopTask;
import ngat.rcs.tms.executive.Track_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * Disables the named axes.
 * 
 * @author eng
 * 
 */
public class StopAxesTask extends ParallelTaskImpl {

	/** True if AZM should be stopped. */
	private boolean stopAzm;

	/** True if ALT should be stopped. */
	private boolean stopAlt;

	/** True if CAS should be stopped. */
	private boolean stopRot;

	/**
	 * @param name
	 * @param manager
	 * @param stopAzm
	 * @param stopAlt
	 * @param stopRot
	 */
	public StopAxesTask(String name, TaskManager manager, boolean stopAzm, boolean stopAlt, boolean stopRot) {
		super(name, manager);
		this.stopAzm = stopAzm;
		this.stopAlt = stopAlt;
		this.stopRot = stopRot;
	}

	@Override
	protected TaskList createTaskList() {

		if (stopAzm) {
			StopTask stopAzmTask = new StopTask(name + "/PreStopAzm", this, STOP.AZIMUTH);
			taskList.addTask(stopAzmTask);
			Track_Task trackAzmOffTask = new Track_Task(name + "/TrackAzmOff", this, TRACK.AZIMUTH, TRACK.OFF);
			taskList.addTask(trackAzmOffTask);
			try {
				taskList.sequence(stopAzmTask, trackAzmOffTask);
			} catch (TaskSequenceException tsx) {
				taskLog.log(3, "Error occurred during sequencing of axis pre-disable tasks: " + tsx);
				failed(TaskList.TASK_SEQUENCE_ERROR, "Failed to link task sequence: " + tsx);
			}
		}

		if (stopAlt) {
			StopTask stopAltTask = new StopTask(name + "/PreStopAlt", this, STOP.ALTITUDE);
			taskList.addTask(stopAltTask);

			Track_Task trackAltOffTask = new Track_Task(name + "/TrackAltOff", this, TRACK.ALTITUDE, TRACK.OFF);
			taskList.addTask(trackAltOffTask);
			try {
				taskList.sequence(stopAltTask, trackAltOffTask);
			} catch (TaskSequenceException tsx) {
				taskLog.log(3, "Error occurred during sequencing of axis pre-disable tasks: " + tsx);
				failed(TaskList.TASK_SEQUENCE_ERROR, "Failed to link task sequence: " + tsx);
			}

		}

		if (stopRot) {
			StopTask stopRotTask = new StopTask(name + "/PreStopRot", this, STOP.ROTATOR);
			taskList.addTask(stopRotTask);

			Track_Task trackRotOffTask = new Track_Task(name + "/TrackRotOff", this, TRACK.ROTATOR, TRACK.OFF);
			taskList.addTask(trackRotOffTask);
			try {
				taskList.sequence(stopRotTask, trackRotOffTask);
			} catch (TaskSequenceException tsx) {
				taskLog.log(3, "Error occurred during sequencing of axis pre-disable tasks: " + tsx);
				failed(TaskList.TASK_SEQUENCE_ERROR, "Failed to link task sequence: " + tsx);
			}
		}
		
		return taskList;
	}
	
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(2, "Stopping axes and disabling tracking for: "+
				(stopAzm ? "AZM ": "")+
				(stopAlt ? "ALT ": "")+
				(stopRot ? "ROT": ""));
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskFailed(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskFailed(Task task) {	
		super.onSubTaskFailed(task);
		// retry then ignore...
		if (((Default_TaskImpl)task).getRunCount() < 2) {
			resetFailedTask(task);
		} else {
			taskList.skip(task);
		}
	}
}

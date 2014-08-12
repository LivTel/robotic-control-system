/**
 * 
 */
package ngat.rcs.sciops;

import ngat.message.RCS_TCS.AGFILTER;
import ngat.message.RCS_TCS.MOVE_FOLD;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.AgFilterTask;
import ngat.rcs.tms.executive.FoldTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class ScienceFoldPositionTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, SCI_FOLD = 1300
	
	/** Instrument port number. */
	private int port;

	/**
	 * @param name
	 * @param manager
	 * @param port
	 *            The instrument port number.
	 */
	public ScienceFoldPositionTask(String name, TaskManager manager, int port) {
		super(name, manager);
		this.port = port;
	}

	@Override
	protected TaskList createTaskList() {

		int portMapping = getPortMapping(port);
		if (portMapping == -1) {
			failed(661301, "Unable to determine port-mapping for instrument");
			return null;
		}

		// stow or deploy and select port
		if (port == 0) {
			AgFilterTask agfTask = new AgFilterTask(name + "/SFStow", this, AGFILTER.IN);
			taskList.addTask(agfTask);

		} else {
			FoldTask foldPositionTask = new FoldTask(name + "/SFMove(Port-" + port + ")", this, portMapping);
			taskList.addTask(foldPositionTask);

			AgFilterTask agfTask = new AgFilterTask(name + "/SFDeploy", this, AGFILTER.OUT);
			taskList.addTask(agfTask);

			try {
				taskList.sequence(agfTask, foldPositionTask);
			} catch (TaskSequenceException tsx) {
				failed(TaskList.TASK_SEQUENCE_ERROR, "Failed to link task sequence: " + tsx);
				return null;
			}

		}
		return taskList;
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Science fold  failure");		
	}

	private int getPortMapping(int targetPort) {

		switch (targetPort) {
		case 0:
			return MOVE_FOLD.STOWED;
		case 1:
			return MOVE_FOLD.POSITION1;
		case 2:
			return MOVE_FOLD.POSITION2;
		case 3:
			return MOVE_FOLD.POSITION3;
		case 4:
			return MOVE_FOLD.POSITION4;
		case 5:
			return MOVE_FOLD.POSITION5;
		case 6:
			return MOVE_FOLD.POSITION6;
		case 7:
			return MOVE_FOLD.POSITION7;
		case 8:
			return MOVE_FOLD.POSITION8;
		default:
			return -1;
		}

	}

}

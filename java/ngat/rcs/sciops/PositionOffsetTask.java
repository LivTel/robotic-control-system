/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.IMosaicOffset;
import ngat.phase2.XPositionOffset;
import ngat.rcs.iss.ISS;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Offset_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class PositionOffsetTask extends ParallelTaskImpl {

	private XPositionOffset offset;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public PositionOffsetTask(String name, TaskManager manager, XPositionOffset offset, ChangeTracker collator) {
		super(name, manager);
		this.offset = offset;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {

		IMosaicOffset currentOffset = collator.getOffset();
		// NOTE. Collator's current offset is ALWAYS absolute.

		double dra = 0.0;
		double ddec = 0.0;
		// NOTE: collator is now created with a (0,0) ABS offset so should never be NULL
		if (currentOffset == null) {
			dra = offset.getRAOffset();
			ddec = offset.getDecOffset();
		} else {			
			if (offset.isRelative()) {
			    dra = offset.getRAOffset() + currentOffset.getRAOffset();
			    ddec = offset.getDecOffset() + currentOffset.getDecOffset();
		
			} else {
			   dra = offset.getRAOffset();
			   ddec = offset.getDecOffset();
			}			
		}

		String sra = "" + Math.rint(Math.toDegrees(dra) * 3600.0);
		String sdec = "" + Math.rint(Math.toDegrees(ddec) * 3600.0);
		Offset_Task offbyTask = new Offset_Task(name + "/OFFBY(" + sra + "," + sdec + ")", this, dra, ddec);
		taskList.addTask(offbyTask);

		return taskList;
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		 collator.applyOffset(offset);
		 XPositionOffset userOffset = collator.getOffset();
		 ISS.setUserOffsets(userOffset.getRAOffset(), userOffset.getDecOffset());
		 collator.setAcquired(false); // we have definitely lost this
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Position offset failure");	
	}

}

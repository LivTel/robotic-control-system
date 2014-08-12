/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.*;
import ngat.rcs.iss.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;

/**
 * @author eng
 * 
 */
public class FocusOffsetTask extends ParallelTaskImpl {

	private XFocusOffset offset;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public FocusOffsetTask(String name, TaskManager manager, XFocusOffset offset, ChangeTracker collator) {
		super(name, manager);
		this.offset = offset;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {

		double dfocus = offset.getOffset();// swap this out if we uncomment next block

		// TODO either use this or add to collator's cumulative focus offset if relative
		 if (offset.isRelative())
			 dfocus = collator.getFocusOffset().getOffset() + offset.getOffset();
		 else
			 dfocus = offset.getOffset(); 

		// see what the current instrument last requested
		double instrumentFocusOffset = ISS.getInstrumentFocusOffset();

		DefocusTask defocusTask = new DefocusTask(name + "/DEFOCUS(" + dfocus + ")", this, dfocus
				+ instrumentFocusOffset);
		taskList.addTask(defocusTask);

		return taskList;
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		collator.applyFocusOffset(offset);
		ISS.setCurrentFocusOffset(collator.getFocusOffset().getOffset());
		FITS_HeaderInfo.current_USRDEFOC.setValue(new Double(collator.getFocusOffset().getOffset()));
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Focus offset failure");		
	}

}

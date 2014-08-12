/**
 * 
 */
package ngat.rcs.sciops;

import ngat.icm.InstrumentDescriptor;
import ngat.phase2.XFocusControl;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.FocusControlExecutiveTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 *
 */
public class FocusControlTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, FOC_CTRL = 800
	
	private XFocusControl control;
	
	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public FocusControlTask(String name, TaskManager manager, XFocusControl control, ChangeTracker collator) {
		super(name, manager);
		this.control = control;
		this.collator = collator;
	}
	
	@Override
	protected TaskList createTaskList() {

		//IFocCon curcon = collator.getFosCcontr
		String instrumentName = control.getInstrumentName();
		InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
		
		try {			
			ireg.getStatusProvider(instId);
		} catch (Exception e) {
			failed(660801, "Instrument not found: "+instrumentName);
			return null;
		}
		FocusControlExecutiveTask focusControlExecutiveTask = new FocusControlExecutiveTask(name+"/FOCCON", this, control);
		taskList.addTask(focusControlExecutiveTask);
		
		return taskList;
	}
	
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		 //collator.applyFCCConfig(..)
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Focus control selection failure");	
	}
	
	
}

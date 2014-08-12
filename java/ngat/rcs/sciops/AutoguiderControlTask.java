/**
 * 
 */
package ngat.rcs.sciops;

import ngat.message.RCS_TCS.AUTOGUIDE;
import ngat.phase2.IAutoguiderConfig;
import ngat.phase2.TelescopeConfig;
import ngat.phase2.XAutoguiderConfig;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.AutoGuide_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class AutoguiderControlTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, AUTO = 300
	
	/** The autoguider control action to take. */
	private IAutoguiderConfig autoConfig;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 *            The name of the task.
	 * @param manager
	 *            The task's manager.
	 * @param autoConfig
	 *            The autoguider control action to take.
	 * @param collator
	 *            Keeps track of changes.
	 */
	public AutoguiderControlTask(String name, TaskManager manager, IAutoguiderConfig autoConfig, ChangeTracker collator) {
		super(name, manager);
		this.autoConfig = autoConfig;
		this.collator = collator;
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		collator.setAutoguide(autoConfig);
	}
	
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task); 
		
		// depends on what command option we had
		int command = autoConfig.getAutoguiderCommand();
		switch (command) {
		case IAutoguiderConfig.ON:
			// disaster - we could have another go if time were available
			failed(660301,"Mandatory autoguider acquisition failed");
			// this is a good example of a veto for 30 minutes or so type error
			break;
		case IAutoguiderConfig.OFF:
			// oh well, never mind !
			taskList.skip(task);
			break;
		case IAutoguiderConfig.ON_IF_AVAILABLE:
			// pity, but not a big deal it was only optional
			taskList.skip(task);
			break;
		}
		// if we didnt switch on then its off presumably...
		collator.setAutoguide(new XAutoguiderConfig(IAutoguiderConfig.OFF, "autoff"));
	}
	
	@Override
	protected TaskList createTaskList() {

		// there can be no autoguiding if we are NS tracking.
		//if (collator.isNonSiderealTracking()) {
			// empty list OR we can switch it off but it should be anyway...
			//collator.setAutoguide(new XAutoguiderConfig(IAutoguiderConfig.OFF, "autoff"));
			//return taskList;
		//}
		
		// TODO maybe this way ?? 
	/*	if (auto-optional and ag-hot) {
			collator.setAutoguide(new XAutoguiderConfig(IAutoguiderConfig.OFF, "autoff"));
			return taskList;
		}*/
		
		// see if we need to or maybe.
		int command = autoConfig.getAutoguiderCommand();

		// make up a teleconfig to carry the usual info down..
		TelescopeConfig teleConfig = new TelescopeConfig();
		teleConfig.setAutoGuiderStarSelectionMode(TelescopeConfig.STAR_SELECTION_RANK);
		teleConfig.setAutoGuiderStarSelection1(1);

		switch (command) {
		case IAutoguiderConfig.ON:
			// switch it on here - failure is bad..
			AutoGuide_Task agOnTask = new AutoGuide_Task(name + "/AG_MAND_ON", this, teleConfig, AUTOGUIDE.ON);
			taskList.addTask(agOnTask);
			break;
		case IAutoguiderConfig.OFF:
			// switch it off here
			AutoGuide_Task agOffTask = new AutoGuide_Task(name + "/AG_OFF", this, teleConfig, AUTOGUIDE.OFF);
			taskList.addTask(agOffTask);
			break;
		case IAutoguiderConfig.ON_IF_AVAILABLE:
			// try to switch it on here - failure is no big thing
			AutoGuide_Task agOnOptTask = new AutoGuide_Task(name + "/AG_OPT_ON", this, teleConfig, AUTOGUIDE.ON);
			taskList.addTask(agOnOptTask);
			break;
		}

		return taskList;

	}

}

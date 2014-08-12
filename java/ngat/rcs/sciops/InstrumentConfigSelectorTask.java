package ngat.rcs.sciops;

import ngat.phase2.IInstrumentConfig;
import ngat.phase2.IInstrumentConfigSelector;
import ngat.phase2.XInstrumentConfig;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.InstConfigTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class InstrumentConfigSelectorTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, INST_CFG_SEL = 1000
	
	/** The config selection. */
	IInstrumentConfigSelector configSelector;

	/** The instrument config to set. */
	IInstrumentConfig config;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public InstrumentConfigSelectorTask(String name, TaskManager manager, IInstrumentConfigSelector configSelector,
			ChangeTracker collator) {
		super(name, manager);
		this.configSelector = configSelector;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {

		// The new style config
		config = configSelector.getInstrumentConfig();

		if (config == null) {
			failed(661001, "No instrument config selected");
			return null;
		}
		String instrumentName = config.getInstrumentName();
		if (instrumentName == null || instrumentName.equals("")) {
			failed(661002, "No instrument name in config");
			return null;
		}

		// reset to capital case...just in case
		((XInstrumentConfig) config).setInstrumentName(instrumentName.toUpperCase());

		taskLog.log(2, "ConfigSelectTask:createTaskList(): with " + config);

		// An old style config
		// InstrumentConfig uconfig = null;

		// try {
		// uconfig = ConfigTranslator.translateToOldStyleConfig(config);
		// } catch (Exception e) {
		// failed(444, "Config translator failed", e);
		// return null;
		// }

		InstConfigTask configTask = new InstConfigTask(name + "/CONFIG(" + config.getInstrumentName() + ":"
				+ config.getName() + ")", this, config);
		taskList.addTask(configTask);

		return taskList;

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		if (task instanceof InstConfigTask) {
			collator.setLastConfig(config);
			collator.setInstrumentName(config.getInstrumentName());
		}
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		String instName = config.getInstrumentName();
		failed(err.getErrorCode(), "Configuration failure ("+instName+")");
	}

}

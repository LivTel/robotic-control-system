/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.ISS_INST.FOCUS_CONTROL;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.XFocusControl;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 * 
 */
public class FocusControlExecutiveTask extends Default_TaskImpl {

	private XFocusControl control;

	/**
	 * @param name
	 * @param manager
	 */
	public FocusControlExecutiveTask(String name, TaskManager manager, XFocusControl control) {
		super(name, manager, "ISS");
		this.control = control;
	}

	/** Carry out subclass specific initialization. */
	@Override
	public void onInit() {
		super.onInit();

		String instrumentName = control.getInstrumentName();

		// Set up the appropriate COMMAND.
		FOCUS_CONTROL fcontrol = new FOCUS_CONTROL(name);
		fcontrol.setInstrumentName(instrumentName);
		command = fcontrol;

		logger.log(1, CLASS, name, "onInit", "Starting focus-control selection for: " + instrumentName);

	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	@Override
	public void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Focus control selection completed");
	}

}

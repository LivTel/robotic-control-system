/**
 * 
 */
package ngat.rcs.sciops;

import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.net.UnknownResourceException;
import ngat.phase2.IExecutionFailureContext;
import ngat.rcs.tms.BasicErrorIndicator;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Default_TaskImpl;
import ngat.sms.GroupItem;

/**
 * @author eng
 * 
 */
public class ExecutionUpdateTask extends Default_TaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, EXEC_UPD = 600
	
	private IExecutionFailureContext efc;

	private GroupItem group;
	
	private int vetoLevel;
	
	public ExecutionUpdateTask(String name, TaskManager manager, GroupItem groupItem, boolean success,
			IExecutionFailureContext efc, int vetoLevel) {
		super(name, manager);	
		this.group = group;
		this.efc = efc;
		this.vetoLevel = vetoLevel;
		try {
			createConnection("SMS_COMMAND");
		} catch (UnknownResourceException e) {
			logger.log(1, "Schedule_Task", name, "Constructor",
					"Unable to establish connection to subsystem: SMS_COMMAND: " + e);
			failed = true;
			errorIndicator = new BasicErrorIndicator(660601, "Creating connection: Unknown resource SMS_COMMAND", e);
			return;
		}

		logger.log(1, "ExecUpdateTask", name, "Constructor",
				"Creating ExecUpdateTask for: Group: "+groupItem+", Success: "+success+", EFC: "+efc);
		
		EXECUTION_UPDATE exec = new EXECUTION_UPDATE(name);
		exec.setGroupId(groupItem.getID());
		exec.setHistoryId(groupItem.getHId());
		exec.setSuccess(success);
		exec.setExecutionFailureContext(efc);
		exec.setTime(System.currentTimeMillis());
		exec.setVetoLevel(vetoLevel);
		command = exec;
		
	}

	
	/**
	 * @return the group
	 */
	public GroupItem getGroup() {
		return group;
	}



	/**
	 * @return the efc
	 */
	public IExecutionFailureContext getExecutionFailureContext() {
		return efc;
	}

}

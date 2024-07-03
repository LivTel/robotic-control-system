package ngat.rcs.tocs;

import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.phase2.*;

/**
 * This Task manages a set of Tasks to perform an ARC.
 * @author $Author: cjm$
 */
public class TOCArcTask extends TOOP_ControlTask
{
	// ERROR_BASE: RCS = 6, TOCS = 50, T_ACQ = 100
	/**
	 * Which instrument is to do the Arc.
	 */
	String instrumentName = null;
	/**
	 * Which lamp to do the ARC with.
	 */
	String lamp = null;

	/**
	 * Create a TOCArcTask.
	 * 
	 * @param name The unique name/id for this TaskImpl.
	 * @param manager The Task's manager.
	 * @param implementor The instance of TOC_GenericCommandImpl requiring this task to be performed.
	 * @param instrumentName Which instrument is to do the Arc.
	 * @param lampString The name of the lamp to be used for the ARC.
	 */
	public TOCArcTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor,
			      String instrumentName, String lampString)
	{

		super(name, manager, implementor);
		this.instrumentName = instrumentName;
		this.lamp = lampString;
	}

	@Override
	public void reset()
	{
		super.reset();
	}

	@Override
	public void onSubTaskFailed(Task task)
	{
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(651201, "Temporary fail TOC ARC operation due to subtask failure.." + task.getName(), null);
		// }
	}
	
	@Override
	public void onSubTaskAborted(Task task)
	{
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task)
	{
		super.onSubTaskDone(task);
	}

	@Override
	public void onAborting()
	{
		super.onAborting();
	}

	@Override
	public void onDisposal()
	{
		super.onDisposal();
	}

	/**
	 * Super.onCompletion() sends the message back to the client so we need to
	 * append any extra data here first.
	 */
	@Override
	public void onCompletion()
	{
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "** Completed TOC ARC");
	}

	/** 
	 * Overridden to carry out specific work after the init() method is called. 
	 */
	@Override
	public void onInit()
	{
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC ARC using lamp: " + lamp);
	}
	
	/** 
	 * Creates the TaskList for this TaskManager. 
	 * @see #lamp
	 * @see #instrumentName
	 */
	@Override
	protected TaskList createTaskList()
	{
		Arc_Task arcTask = new Arc_Task("Arc", this, lamp, instrumentName);
		taskList.addTask(arcTask);

		return taskList;
	}
}

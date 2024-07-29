package ngat.rcs.tocs;

import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.message.GUI_RCS.ExposureInfo;
import ngat.message.GUI_RCS.ReductionInfo;
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
	 * Counts files received from the Arc exposure. 
	 */
	int countFiles = 0;

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
	
	/**
	 * Catch any DP/non-DP processed filenames from an Arc Exposure. 
	 */
	@Override
	public void sigMessage(Task source, int type, Object message) 
	{
		System.err.println("SIGMESG: Type: " + type + " From: " + source + " : " + message);
		switch (type) {
		case Exposure_Task.EXPOSURE_DATA:

			if (message instanceof ReductionInfo) 
			{
				countFiles++;

				ReductionInfo rinfo = (ReductionInfo) message;
				taskLog.log(WARNING, 1, CLASS, name, "sigMessage", "TOC ARC received reduced filename: "+
							rinfo.getFileName());
				if (countFiles == 1)
					concatCompletionReply(" file1=" + rinfo.getFileName());
				else
					concatCompletionReply(" ,file" + countFiles + "=" + rinfo.getFileName());

			} 
			else if (message instanceof ExposureInfo) 
			{
				//countFiles++;

				ExposureInfo einfo = (ExposureInfo) message;
				taskLog.log(WARNING, 1, CLASS, name, "sigMessage", "TOC ARC received filename: "+
						einfo.getFileName());
			}
			break;
		case Exposure_Task.EXPOSURE_FILE:
			//String tagName = (String) FITS_HeaderInfo.current_TAGID.getValue();
			//String userName = (String) FITS_HeaderInfo.current_USERID.getValue();
			//String propName = (String) FITS_HeaderInfo.current_PROPID.getValue();
			//String grpName = (String) FITS_HeaderInfo.current_GROUPID.getValue();
			//String obsName = exposure.getActionDescription();

			//obsLog.log(1, "TOCA Program:" + tagName + " : " + userName + " : " + propName + " : " + grpName + " : "
			//		+ obsName + ": Exposure Completed, File: " + message);
			break;
		default:
			super.sigMessage(source, type, message);
		}
	}

}

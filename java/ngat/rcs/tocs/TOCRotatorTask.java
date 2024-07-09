// TOCRotatorTask.java
package ngat.rcs.tocs;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.sms.GroupItem;
import ngat.util.PersistentUniqueInteger;
import ngat.phase2.*;
import ngat.message.RCS_TCS.*;

/**
 * This Task allows us to override the default TOCA mode rotator mode 
 * (configured as part of the INIT / TOCInitTask.java).
 * This is needed for certain instruments (i.e. Sprat, where the slit must be aligned to a certain mount angle to 
 * ensure the slit is aligned to the zenith).
 * @author $Author: cjm $
 */
public class TOCRotatorTask extends TOOP_ControlTask
{
	/** 
	 * Option: Not configured the rotator mode. 
	 */
	public static final int ROTATOR_MODE_NONE = 0;
	/** 
	 * Option: Rotator to SKY angle. 
	 */
	public static final int ROTATOR_MODE_SKY = 1;
	/** 
	 * Option: Rotator to MOUNT angle and FLOAT. 
	 */
	public static final int ROTATOR_MODE_MOUNT = 2;
	/** 
	 * Option: Rotator to FLOAT. 
	 */
	public static final int ROTATOR_MODE_FLOAT = 3;

	/**
	 * Rotator mode.
	 */
	protected int rotatorMode = ROTATOR_MODE_NONE;
	/**
	 * Rotator mount angle to use, in radians,  when mode is ROTATOR_MODE_MOUNT.
	 */
	protected double mountAngle = 0.0;

	/**
	 * Create a TOCRotatorTask.
	 * @param name The unique name/id for this TaskImpl.
	 * @param manager The Task's manager.
	 * @param rotatorMode Rotator mode option.
	 * @param mountAngle The angle to drive the rotator to, in radians, if the rotatorMode is ROTATOR_MODE_MOUNT.
	 */
	public TOCRotatorTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor,
			      int rotatorMode, double mountAngle)
	{
		super(name, manager, implementor);
		this.rotatorMode = rotatorMode;
		this.mountAngle = mountAngle;
	}
	
	@Override
	public void onSubTaskFailed(Task task)
	{
		super.onSubTaskFailed(task);
		failed(651201, "Temporary fail TOCRotator operation due to subtask failure.." + task.getName(), null);
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

	@Override
	public void onCompletion()
	{
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onCompletion", "** Completed TOC Rotator");
	}

	/** 
	 * Overridden to carry out specific work after the init() method is called. 
	 */
	@Override
	public void onInit()
	{
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Rotator with mode: "
			    + rotatorMode + " and mount angle "+ mountAngle);
	}

	/**
	 * Creates the TaskList for this TaskManager. 
	 */
	@Override
	protected TaskList createTaskList()
	{

		RotatorTask rotatorTask = null;
		RotatorTask rotatorFloatTask = null;

		// ROT SKY is not feasible in TOCA mode without implementing cardinal pointing, so
		// we just fail if that is selected at the moment.

		switch (rotatorMode)
		{
			case ROTATOR_MODE_SKY:
				// rot sky 0 could drive into a limit due to rotator pipe restrictions
				// Therefore return an error instead.
				//rotatorTask = new RotatorTask("RotSky", this, 0.0, ROTATOR.SKY);
				//taskList.addTask(rotatorTask);
				errorLog.log(1, CLASS, name, "createTaskList",
					     "Failed to create Task Sequence for TOC Rotator: Mode was set to SKY.");
				failed = true;
				// perhaps we should add a new error code here?
				// robotic-control-system/java/ngat/rcs/tms/TaskList.java
				errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
				errorIndicator.setErrorString("Failed to create Task Sequence for TOC Rotator: Mode was set to SKY.");
				//errorIndicator.setException(tx);
				return null;
				//break;
			case ROTATOR_MODE_FLOAT:
				rotatorTask = new RotatorTask("RotFlt", this, 0.0, ROTATOR.FLOAT);
				taskList.addTask(rotatorTask);
				break;

			case ROTATOR_MODE_MOUNT:
				// RotatorTask angle is in radians.
				rotatorTask = new RotatorTask("RotMount", this, mountAngle, ROTATOR.MOUNT);
				taskList.addTask(rotatorTask);

				rotatorFloatTask = new RotatorTask("RotFloat", this, 0.0, ROTATOR.FLOAT);
				rotatorFloatTask.setDelay(5000L);
				taskList.addTask(rotatorFloatTask);
				try
				{
					taskList.sequence(rotatorTask, rotatorFloatTask);
				}
				catch (TaskSequenceException tx)
				{
					errorLog.log(1, CLASS, name, "createTaskList",
						     "Failed to create Task Sequence for TOC Rotator: " + tx);
					failed = true;
					errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
					errorIndicator.setErrorString("Failed to create Task Sequence for TOC Rotator.");
					errorIndicator.setException(tx);
					return null;
				}
				break;
		}
		return taskList;
	}
}

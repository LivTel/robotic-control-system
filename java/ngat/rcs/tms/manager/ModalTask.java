/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 * Generic class for implementing Modal behaviour. ModalTasks only ever control
 * a single subTask (i.e. there is only one (or zero) Tasks on its TaskList at
 * any instant. These Tasks are generally named as Agents for carrying out a
 * specific role e.g. XXXModeControlAgent or the likes.
 * 
 * Subclasses - i.e. Specific-mode Controllers should provide implementations of
 * the following signature methods from the TaskSequencer interface:-
 * <dl>
 * <dt>nextJob()
 * <dd>Should return a new subTask for the ModalTask to control.
 * <dt>acceptControl()
 * <dd>Should return <i>true</i> if the ModalTask is in an appropriate state to
 * assume control.
 * </dl>
 * 
 */
public abstract class ModalTask extends ParallelTaskImpl implements
		EventSubscriber, Logging {

	/** Default time to wait before yielding to control election (millis). */
	public static final long DEFAULT_POLLING_DELAY = 1000L;

	/** Time to wait before yielding to control election (millis). */
	protected final long pollingDelay;

	/** The currently controlled subTask. */
	protected volatile Task currentTask;

	/** True if this modal is currently running a subtask. */
	protected volatile boolean active;

	/**
	 * Description of what the agent is doing. Subtasks can set this to indicate
	 * what is goig on.
	 */
	protected String activity;

	/** AgentID used in Root name */
	protected String agentId;

	/** Version number of this ModalControlAgent. */
	protected String agentVersion;

	/** Description of this ModalControlAgent. */
	protected String agentDesc;

	/** Priority setting for this MCA. */
	protected int agentPriority;

	/** Records time used by this MCA in current instantiation. */
	protected long timeUsed;

	/** Records the start of the current active session for this MCA. */
	protected long startSession;

	/**
	 * Create a ModalTask.
	 * 
	 * @param name
	 *            The name of this Task.
	 * @param manager
	 *            This Task's manager.
	 */
	public ModalTask(String name, TaskManager manager) {
		super(name, manager);
		pollingDelay = DEFAULT_POLLING_DELAY;
		setExitOnCompletion(false);
		timeUsed = 0;
	}

	/**
	 * Configure from File.
	 * 
	 * @param file
	 *            Configuration file.
	 * @exception IOException
	 *                If any problem occurs reading the file or does not exist.
	 * @exception IllegalArgumentException
	 *                If any config information is dodgy.
	 */
	public abstract void configure(File file) throws IOException,
			IllegalArgumentException;

	/**
	 * Initializes the single instance of the ModalControlAgent. Subclasses must
	 * override this method to set their own static instance variable as it
	 * cannot be done generically here.
	 */
	public void initialize(ModalTask mt) {
	}

	/**
	 * Load history settings from File. Each MCA will want to override the
	 * method
	 * 
	 * @param history
	 *            History info.
	 * @exception IOException
	 *                If any problem occurs reading the file or does not exist.
	 * @exception IllegalArgumentException
	 *                If any history information is dodgy.
	 */
	public abstract void loadHistory(Map history) throws IOException,
			IllegalArgumentException;

	/**
	 * Returns a reference to the singleton instance. Subclasses must override
	 * this method to return their own static instance variable
	 */
	public static ModalTask getInstance() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#init()
	 */
	@Override
	public void init() {
		// NO TODO reset the task descriptor here - this may be changed
		// but is for TaskLifecycle handler glitch
		descriptor = new TaskDescriptor(name, getClass().getSimpleName());
		super.init();
	}

	/** Sets the agent ID. */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/** Returns the AgentID for this ModalControlAgent. */
	public String getAgentId() {
		return agentId;
	}

	/** Sets the agent version. */
	public void setAgentVersion(String agentVersion) {
		this.agentVersion = agentVersion;
	}

	/** Returns the Agent Version for this ModalControlAgent. */
	public String getAgentVersion() {
		return agentVersion;
	}

	/** Sets the agent Description. */
	public void setAgentDesc(String agentDesc) {
		this.agentDesc = agentDesc;
	}

	/** Returns the Agent Description for this ModalControlAgent. */
	public String getAgentDesc() {
		return agentDesc;
	}

	/** Sets the priority of this ModalControlAgent. */
	public void setAgentPriority(int agentPriority) {
		this.agentPriority = agentPriority;
	}

	/** Returns the priority of this ModalControlAgent. */
	public int getAgentPriority() {
		return agentPriority;
	}

	/** Returns the time used by this MCA since instantiation. */
	public long getTimeUsed() {
		return timeUsed;
	}

	/** Returns the reason for non-acceptance of control. */
	public abstract String getNonAcceptanceReason();

	/** Returns agent-specific properties. */
	public abstract Properties getAgentProperties();

	/**
	 * Request this ModalTask to take control of the specified subTask. If this
	 * Task is being/has been aborted or has not yet been initialized then no
	 * action is taken and returns false. If the task submitted is null then
	 * nothing happens - this is useful if e.g. the Agent needs to read from say
	 * a queue and nothing is currently on it. The Agent will need to let
	 * something attached to the queue know that it wants a job e.g. by
	 * attaching a blockable thread to the end of the queue. When something
	 * becomes available the thread reads it and signals back to the Agent via
	 * its
	 * 
	 * @param subTask
	 *            The subtask to be controlled.
	 */
	public boolean manage(Task subTask) {
		taskLog.log(3, CLASS, name, "manage", "Requested to manage Task: "
				+ subTask + " While: ["
				+ (aborting ? " Aborting" : " NOT Aborting")
				+ (aborted ? " Aborted" : " NOT Aborted")
				+ (initialized ? " Initialized" : " NOT Initialized") + "]");
		if (aborting || aborted || !initialized)
			return false;
		taskLog.log(3, CLASS, name, "manage", "Subtask class: "
				+ (subTask != null ? subTask.getClass().getName() : "NULL"));
		if (subTask == null)
			return false;
		// synchronized (taskList) {
		currentTask = subTask;
		// taskList.addTask(subTask);
		// worker.interrupt();
		messageQueue.add(new TaskEvent(subTask, SUBTASK_ADDED));
		// }
		return true;
	}

	/**
	 * Request this ModalTask to remove the specified Task. Only succeeds if the
	 * nominated Task is the currentTask.
	 * 
	 * @param task
	 *            The Task to remove.
	 */
	public void unmanage(Task subTask) {
		taskLog.log(3, CLASS, name, "unmanage", "Requested to unmanage Task: "
				+ subTask);

		if (subTask == null)
			return;
		// synchronized (taskList) {
		if (currentTask == subTask)
			currentTask = null;
		// taskList.removeTask(subTask);
		// worker.interrupt();
		messageQueue.add(new TaskEvent(subTask, SUBTASK_REMOVE));
		// }
	}

	/** Returns the currently executing task or null. */
	public Task getCurrentTask() {
		return currentTask;
	}

	/** Returns True if this task is controlling a subtask at this point. */
	public boolean isActive() {
		return active;
	}

	/** Returns the current activity. */
	public String getActivity() {
		return activity;
	}

	/** Set the current activity. */
	public void setActivity(String activity) {
		this.activity = activity;
	}

	/**
	 * Override to return <i>true</i> if this ModalTask is willing to accept
	 * control at this instant.
	 */
	public abstract boolean acceptControl();

	/**
	 * Override to return the time at which this ModalControlAgent will next
	 * request control. I.e. at this point it will want to take control from any
	 * lower ranked MCA if possible.
	 * 
	 * @return Time when this MCA will next want control (millis 1970).
	 */
	public abstract long demandControlAt();

	/**
	 * Override to return an appropriate subTask to be controlled by this
	 * ModalTask.
	 */
	public abstract void nextJob();

	/**
	 * Send a YIELD request after a short delay in order to present for
	 * re-election.
	 */
	protected void yieldControl() {
		taskLog.log(3, CLASS, name, "yield", "About to yield control.");
		Runnable r = new Runnable() {
			public void run() {
				try {
					Thread.sleep(pollingDelay);
					TaskOperations
							.getInstance()
							.getOperationsManager()
							.operationsModeYieldControl("From " + getAgentId());
									
				} catch (InterruptedException e) {
				}

			}
		};
		(new Thread(r)).start();
	}
	
	/**
	 * Send a YIELD request with NO delay in order to present for
	 * re-election.
	 */
	/*
	 * protected void yieldControl() { taskLog.log(3, CLASS, name, "yield",
	 * "About to yield control.");
	 * TMM_TaskSequencer.getInstance().getOperationsManager
	 * ().notifyEvent(RCS_ControlTask.TASK_YIELD_MESSAGE, "From "+getAgentId());
	 * }
	 */

	/**
	 * Overriden to create an empty TaskList for this TaskManager.
	 * 
	 * @return The (initially empty) TaskList for this Task.
	 */
	@Override
	protected TaskList createTaskList() {
		return taskList;
	}

	/**
	 * Override to handle recovery from failure of a subTask. Typically just
	 * rerun the subtask - i.e. put a copy of it back into the TaskList. This
	 * method should only perform a small amount of processing to avoid
	 * overreaching as it runs in the worker thread for the failed Task.
	 * Subclasses which override this should check for stopping and return
	 * immediately to let the worker be interrupted.
	 * 
	 * @param task
	 *            The subTask which has failed.
	 */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		unmanage(currentTask);
		active = false;
		// EventQueue.postEvent(RCS_ControlTask.TASK_YIELD_MESSAGE);
		yieldControl();
		// #####SEND the JOB_REQUEST code for RCS Controller
	}

	/**
	 * Override to handle completion of a subTask. This method should only
	 * perform a small amount of processing to avoid overreaching as it runs in
	 * the worker thread for the completed Task. Subclasses which override this
	 * should check for stopping and return immediately to let the worker be
	 * interrupted.
	 * 
	 * @param task
	 *            The subTask which has done.
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		unmanage(currentTask);
		active = false;
		// #####SEND the JOB_REQUEST code for RCS Controller
		// EventQueue.postEvent(RCS_ControlTask.TASK_YIELD_MESSAGE);
		yieldControl();
	}

	/**
	 * Override to handle an aborted subTask - called after the subTask was
	 * aborted (usually by us) and has carried out its own abort handling, and
	 * called back to indicate so.
	 * 
	 * @param task
	 *            The subTask which has been aborted.
	 */
	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
		unmanage(currentTask);
		active = false;
		// If we are NOT aborting then carry on as for Done/Failed
		if (!aborting)
			yieldControl();
		// EventQueue.postEvent(RCS_ControlTask.TASK_YIELD_MESSAGE);
	}

	/**
	 * Override to allow subclasses to perform setup <i>just before</i> creating
	 * TaskList.
	 */
	@Override
	public void preInit() {
		super.preInit();
		startSession = System.currentTimeMillis();
	}

	/**
	 * Override to allow subclasses to perform setup <i>just after</i> creating
	 * TaskList.
	 */
	@Override
	public void onInit() {
		super.onInit();
	}

	/**
	 * Override to allow subclasses to perform any setup <i>just after</i> the
	 * worker thread has started but before any other action. Calls manage() on
	 * next available subtask..
	 */
	@Override
	public void onStartup() {
		super.onStartup();
		nextJob();
	}

	/** Override to allow subclasses to perform any disposal operations. */
	@Override
	public void onDisposal() {
		super.onDisposal();
		timeUsed += (System.currentTimeMillis() - startSession);
	}

	/** EventSubscriber method. */
	@Override
	public void notifyEvent(String eventId, Object data) {

		System.err.println("Agent: " + agentId + " receiving notification: ["
				+ eventId + "]");

	}

	/**
	 * EventSubscriber method.
	 */
	@Override
	public String getSubscriberId() {
		return name;
	}

	/** Returns readable description. */
	@Override
	public String toString() {
		return "[ModalControlAgent: " + name + ", Manager=" + manager
				+ ", AID=" + agentId + ", Version=" + agentVersion + ", Desc="
				+ agentDesc + ", Priority=" + agentPriority + ", TimeUsed="
				+ (timeUsed / 1000) + " secs" + "]";
	}

}

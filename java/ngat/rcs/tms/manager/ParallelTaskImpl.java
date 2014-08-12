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
import ngat.rcs.emm.*;
import ngat.net.*;
import ngat.tcm.SciencePayload;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.icm.InstrumentRegistry;

import java.util.*;
import java.text.*;

/**
 * ManagerTaskAgent implementation:
 * 
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: ParallelTaskImpl.java,v 1.2 2007/09/07 20:29:05 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/ParallelTaskImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public abstract class ParallelTaskImpl implements Task, TaskManager, Logging, EventSubscriber {

	/** The id of the Class. */
	public String CLASS;

	/**
	 * Error code: Indicates that a Task has exceeded its imposed time limit
	 * from startup.
	 */
	public static final int TIME_OVERRUN = 600888;

	/** Message code - signifies that a subtask has completed. */
	public static final int SUBTASK_DONE = 1;

	/** Message code - signifies that a subtask has failed. */
	public static final int SUBTASK_FAILED = 2;

	/** Message code - signifies that a subtask has aborted. */
	public static final int SUBTASK_ABORTED = 3;

	/** Message code - signifies that a subtask has been added. */
	public static final int SUBTASK_ADDED = 4;

	/** Message code - signifies that a subtask should be removed. */
	public static final int SUBTASK_REMOVE = 5;

	/** Default interval for main exec loop to poll (millis). */
	public static final long DEFAULT_POLLING_INTERVAL = 3000L;

	/** Default exit-on-completion status. */
	public static final boolean DEFAULT_EXIT_ON_COMPLETION = true;

	/** Standard ISO8601 DateTime format. */
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss z");

	public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/** A name/id for the Task - used in logging messages. */
	protected String name;

	/** The Task's manager - may be null. */
	protected TaskManager manager;

	/** ConnectionFactory. */
	protected static ConnectionFactory connectionFactory;

	/** Recovery registry. */
	protected static TaskRecoveryRegistry recovery;

	/** Config registry. */
	protected static TaskConfigRegistry configRegistry;

	protected static SciencePayload payload;

	protected static InstrumentRegistry ireg;

	/** The list of subtasks which this TaskManager will spawn. */
	protected TaskList taskList;

	/** Task's config. */
	protected ConfigurationProperties config;

	/**
	 * List of messages (from subtasks) awaiting processing. Access to MQ should
	 * be synchronized.
	 */
	protected List messageQueue;

	/** Interval for main exec loop to poll (millis). */
	protected static long pollingInterval;

	/** A Worker thread to execute this Task. */
	protected TaskWorker worker;

	/** Tasklist lock. */
	protected BooleanLock lock;

	/** Object for locking on. */
	protected Object livelock;

	/** Object for locking on. */
	protected Object suspendlock;

	/** Indicates the current ErrorState of this Task (if any). */
	protected ErrorIndicator errorIndicator;

	/** Set to indicate that this Task has completed abort sequence. */
	protected volatile boolean aborted;

	/** Set to indicate that this Task has been stopped. */
	protected volatile boolean stopped;

	/** Set to indicate that this Task has completed. */
	protected volatile boolean done;

	/** Set to indicate that this Task has failed. */
	protected volatile boolean failed;

	/** Set to indicate that this Task has been initialized. */
	protected volatile boolean initialized;

	/** Set to indicate that this Task has started execution. */
	protected volatile boolean started;

	/** Set to indicate that this Task is suspended. */
	protected volatile boolean suspended;

	/** Set to indicate that this task has started its stop sequence. */
	protected volatile boolean stopping;

	/** Set to indictate that this Task has started abort sequence. */
	protected volatile boolean aborting;

	/** Set to indicate that the task is failing. */
	protected volatile boolean failing;

	/** Signals that the manager wishes this task to abort. */
	protected volatile boolean abortFlag;

	/** Signals that the manager wishes this task to abort. */
	protected volatile boolean failFlag;

	/** Stores the code used to signal an abort of this task. */
	protected volatile int abortCode;

	/** Stores the message sent with an abort of this task. */
	protected String abortMessage;

	/** Signals that the manager wishes this task to stop. */
	protected boolean stopFlag;

	/** Time by which all subtasks must have stopped. */
	protected long stopTime;

	/**
	 * Set to indicate that this task should exit its execution when the
	 * TaskList has been completed. If this is false, the Task will hang on
	 * completion of its tasklist until told explicitly to die off - e.g. it may
	 * need to remain in force while new Tasks are added to the list at
	 * intervals.
	 */
	protected boolean exitOnCompletion;

	/** set to indicate that subtasks should be removed after disposing. */
	protected boolean removeTaskAfterDisposal;

	/** Set to indicate that this Task has an imposed execution time-limit. */
	protected volatile boolean timeConstrained;

	/** The execution time-limit (millis) from Startup (if timeConstrained). */
	protected volatile long timeLimit;

	/** Logs TASK info. */
	protected Logger taskLog;

	/** Logs ERROR info. */
	protected Logger errorLog;

	/** General Operations Log. */
	protected Logger opsLog;

	/** Observing summary log. */
	protected Logger obsLog;

	/** Initial delay (millis). */
	protected long delay;

	/** Time the run was started. */
	protected long runStartTime;

	protected TaskDescriptor descriptor;
	
	/**
	 * Create a ParallelTaskImpl with the specified name and manager. The task
	 * is created with a TaskMonitor generated from the static
	 * TaskMonitorFactory belonging to this Class. The default behaviour is for
	 * exit-on-completion of taskList. Subclasses may reset this if they need to
	 * hang about waiting for new tasks to be added.
	 * 
	 * @param name
	 *            The name/id of this Task.
	 * @param manager
	 *            The manager of this Task.
	 */
	public ParallelTaskImpl(String name, TaskManager manager) {
		this.name = name;
		this.manager = manager;
		descriptor = new TaskDescriptor(name, getClass().getSimpleName());
		// TODO lose this ....
		TaskEvent tev = new TaskEvent(null, 0);

		lock = new BooleanLock(false);
		livelock = new Object();
		suspendlock = new Object();

		taskList = new TaskList();
		messageQueue = new Vector();

		errorIndicator = new BasicErrorIndicator(0, "OK", null);

		taskLog = LogManager.getLogger("TASK");
		errorLog = LogManager.getLogger("ERROR");
		opsLog = LogManager.getLogger("OPERATIONS");
		obsLog = LogManager.getLogger("OBSERVING");

		sdf.setTimeZone(UTC);

		CLASS = getClass().getName();

		if (pollingInterval == 0L)
			pollingInterval = DEFAULT_POLLING_INTERVAL;
		exitOnCompletion = DEFAULT_EXIT_ON_COMPLETION;

		removeTaskAfterDisposal = false;

		timeConstrained = false;
		timeLimit = 0L;

		done = false;
		failed = false;
		started = false;
		aborted = false;
		stopped = false;
		suspended = false;
		initialized = false;
		aborting = false;
		failing = false;
		stopping = false;

		try {			
		    // somehow need to get modal notification in here !
		    RCS_Controller.controller.getTaskMonitor()
			.notifyListenersTaskCreated(
						    manager.getDescriptor(),
						    descriptor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the descriptor
	 */
	public TaskDescriptor getDescriptor() {
		return descriptor;
	}

	/** Sets the delay before starting after init. */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * Return the name of this TaskImpl.
	 * 
	 * @return The name of this TaskImpl.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the task's manager.
	 * 
	 * @return The manager for this task - null if this is the top level Task.
	 */
	public TaskManager getManager() {
		return manager;
	}

	/**
	 * Returns this Task's Worker Thread. The Worker thread is generally
	 * allocated when the Task's exec() method is called.
	 */
	public Thread getWorker() {
		return worker;
	}

	/**
	 * Set the ConnectionFactory.
	 * 
	 * @param cf
	 *            The ConnectionFactory to use.
	 */
	public static void setConnectionFactory(ConnectionFactory cf) {
		connectionFactory = cf;
	}

	/** Set the Recovery Registry. */
	public static void setRecoveryRegistry(TaskRecoveryRegistry r) {
		recovery = r;
	}

	/** Returnsa reference to the TaskConfigRegistry. */
	public static TaskConfigRegistry getTaskConfigRegistry() {
		return configRegistry;
	}

	/** Set the Config Registry. */
	public static void setTaskConfigRegistry(TaskConfigRegistry r) {
		configRegistry = r;
	}

	public static void setSciencePayload(SciencePayload p) {
		payload = p;
	}

	public static void setInstrumentRegistry(InstrumentRegistry i) {
		ireg = i;
	}

	/**
	 * Sets the main exec loop polling interval.
	 * 
	 * @param delay
	 *            Polling interval (millis).
	 */
	public static void setPollingInterval(long poll) {
		pollingInterval = poll;
	}

	/** Returns the main exec loop polling interval (millis). */
	public static long getPollingInterval() {
		return pollingInterval;
	}

	// -------------
	// Own methods.
	// -------------

	/**
	 * Set true if this task should exit from its execution when its tasklist is
	 * completed.
	 */
	public void setExitOnCompletion(boolean exit) {
		exitOnCompletion = exit;
	}

	/**
	 * Returns true if this task should exit from its execution when its
	 * tasklist is completed.
	 */
	public boolean getExitOnCompletion() {
		return exitOnCompletion;
	}

	public void setRemoveTaskAfterDisposal(boolean remove) {
		this.removeTaskAfterDisposal = remove;
	}

	/**
	 * Set True if this Task has an imposed time limit from startup.
	 * 
	 * @param timeLimit
	 *            The timeLimit from startup (millis.
	 */
	public void setTimeConstrained(boolean timeConstrained) {
		this.timeConstrained = timeConstrained;
	}

	/** Returns True if this Task has an imposed time limit from startup. */
	public boolean getTimeConstrained() {
		return timeConstrained;
	}

	/**
	 * Sets the time limit from startup.
	 * 
	 * @param timeLimit
	 *            The time limit from startup (millis).
	 */
	public void setTimeLimit(long timeLimit) {
		this.timeLimit = timeLimit;
	}

	/** Returns the time limit from startup (millis). */
	public long getTimeLimit() {
		return timeLimit;
	}

	public String getManagerName() {
		return name;
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
	public void onSubTaskFailed(Task task) {
		ErrorIndicator err = task.getErrorIndicator();
		taskLog.log(ERROR, 1, CLASS, name, "onSubTaskFailed", "Subtask Failed: Id=" + task.getName() + ", Class="
				+ task.getClass().getName() + ", Error-code=" + err.getErrorCode() + ", Message="
				+ err.getErrorString() + (err.getException() != null ? "Exception=" + err.getException() : ""));
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
	public void onSubTaskDone(Task task) {
		taskLog.log(INFO, 3, CLASS, name, "onSubTaskDone", "MTask: " + name + ": Completed subtask: Id="
				+ task.getName() + ", Class=" + task.getClass().getName());

	}

	/**
	 * Override to handle an aborted subTask - called after the subTask was
	 * aborted (usually by us) and has carried out its own abort handling, and
	 * called back to indicate so.
	 * 
	 * @param task
	 *            The subTask which has been aborted.
	 */
	public void onSubTaskAborted(Task task) {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": onSubTaskAborted: " + task.getName());
	}

	/**
	 * Override to allow subclasses to perform setup <i>just prior</i> to
	 * initialization.
	 */
	public void preInit() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": Pre-Initializing");
	}

	/**
	 * Override to allow subclasses to perform setup <i>just after</i>
	 * initialization.
	 */
	public void onInit() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": Post-Initializing");
	}

	/**
	 * Override to allow subclasses to perform any setup <i>just after</i> the
	 * worker thread has started but before any other action. This default
	 * implementation does nothing.
	 */
	public void onStartup() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": Worker-Startup");
	}

	/**
	 * Override to handle recovery after this Task is aborted.<br>
	 * NOTE: It is called by abort() <b>before</b> the subtasks are aborted.
	 * This method should only perform a small amount of processing as it runs
	 * in the manager's worker thread for this Task. Alternatively we can insert
	 * new subTasks in the TaskList after clearing it. These will just execute
	 * as normal in the Manager's Worker thread for this Task.
	 */
	public void onAborting() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": onAborting");
	}

	/**
	 * Override to allow subclasses to carry out any specific clearing up after
	 * the generic clearup has been performed.
	 */
	public void onDisposal() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": onDisposing");
	}

	/**
	 * Override to allow subclasses to carry out any specific work after they
	 * have completed all thier subtasks, prior to disposal.
	 */
	public void onCompletion() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": onCompleting");
	}

	/**
	 * Override to allow subclasses to carry out any specific work after they
	 * have failed, prior to disposal.
	 */
	public void onFailure() {
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": onFailure");
	}

	/**
	 * Override to create the TaskList for this TaskManager.
	 * 
	 * @return The modified (initial) TaskList for this Task.
	 */
	protected abstract TaskList createTaskList();

	// -------------
	// Task Methods.
	// -------------

	/**
	 * When a Task is initialized - just before its TaskManager's Worker thread
	 * starts it up, If it's a TaskManager itself it calls
	 * TaskManager.createTaskList(). If this were done any sooner i.e. when the
	 * Task is instantiated, the entire Task tree would have to be generated
	 * when the RootTask is created. <br>
	 * Note: Calls preInit() before creating TaskList.<br>
	 * Note: Calls onInit() after creating TaskList.
	 */
	public void init() {

		// Locate our personal Task Config.
		config = configRegistry.getTaskConfig(this);

		initialized = true;

		try {
		    RCS_Controller.controller.getTaskMonitor().notifyListenersTaskInitialized(
											      descriptor);
                } catch (Exception e) {
		    e.printStackTrace();
                }

		preInit();
		// Failed during initialization.
		// TODO - shouldnt this be if (failing) NOT failed as it cant have done
		// yet ?
		// if (failed) {
		if (failing) {
			if (manager != null) {
				taskLog.log(3, CLASS, name, "init", "MTask: " + name
						+ ": Failed during init. - About to signal manager failed.");
				manager.sigTaskFailed(this);
			}
			return;
		}
		createTaskList();
		onInit();

	}

	/**
	 * Called by a Task's manager to cause the Task to carry out its defined set
	 * of operations - as follows:
	 * 
	 * <dl>
	 * <dt>Phase I. (ABORT TEST).
	 * <dd>Testing for abort signal - aborting any runners.
	 * <dt>Phase II. (MESSAGE_PROCESSING).
	 * <dd>Processing any queued messages.
	 * <dt>Phase III. (SWEEP).
	 * <dd>Start any PENDING tasks.
	 * <dt>Phase IV. (COMPLETION TEST).
	 * <dd>Check for completion/failure/abort and signal to manager.
	 * </dl>
	 */
	public void perform() {
		worker = (TaskWorker) Thread.currentThread();
		started = true;
		runStartTime = System.currentTimeMillis();
		try {
			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskStarted(
				descriptor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}

		// Template - Startup behaviour.
		onStartup();

		boolean hit = false;

		// Loop until completion.
		while (!isDone() && !isAborted() && !isFailed()) {

			// See if we've overrun yet - this is TEMP ###############
			// Check for [aborting] or we will end up sending this every loop
			if (timeConstrained && (!aborting)) {
				if (System.currentTimeMillis() > (runStartTime + timeLimit)) {
					// failed(TIME_OVERRUN,
					// "MTask: "+name+": Exceeded available time ("+timeLimit+" millis.");
					// setAbortCode(TIME_OVERRUN,
					// "Time overrun ("+timeLimit+" millis.)");
					// abort();

					// ### TEMP. send a ??? signal - YIELD will work ? if we do
					// the aborting test ?
					if (!hit) {
						taskLog.log(1, CLASS, name, "exec", "MTask: " + name + ": Hit TimeConstraint: Exec Started: "
								+ sdf.format(new Date(runStartTime)) + ", Limit after: " + (timeLimit / 1000)
								+ " secs.");
						EventQueue.postEvent("SIG_INT", "mta-timelimit");
						hit = true;
						// only send one of these...
					}
				}
			}

			// Loop delay -
			try {
				Thread.sleep(pollingInterval);
			} catch (InterruptedException ix) {
			}

			// Phase 1.
			taskLog.log(5, CLASS, name, "exec", "MTask: " + name + ": Starting Phase I.");

			if (abortFlag) {
				aborting = true;

				taskLog.log(3, CLASS, name, "exec", "MTask: " + name + ": Starting abort sequence:");

				abortExecutingTasks();

				// Template - Abort behaviour.
				onAborting();

				abortFlag = false;
			}

			// if (failFlag) {
			// failing = true;

			// taskLog.log(3, CLASS, name, "exec", "MTask: " + name +
			// ": Starting failure abort sequence:");

			// abortExecutingTasks();

			// Template - Abort behaviour.
			// onAborting();

			// failFlag = false;
			// }

			// Phase 2.
			taskLog.log(5, CLASS, name, "exec", "MTask: " + name + ": Starting Phase II.");

			if (!messageQueue.isEmpty()) {

				TaskEvent tev = (TaskEvent) messageQueue.remove(0);

				taskLog.log(5, CLASS, name, "exec", "Checking MQ: >> " + tev);

				int mesg = tev.getMessageCode();
				Task task = tev.getTask();

				// See if we are trying to add a subtask to the TL here.
				if (mesg == SUBTASK_ADDED) {
					taskList.addTask(task);
					taskLog.log(5, CLASS, name, "exec", "MTask: " + name + ": Added Task: " + task.getName());
				}

				TaskInfo tinfo = taskList.getInfo(task);

				if (tinfo != null) {
					switch (mesg) {

					case SUBTASK_DONE:
						if (aborting || failing) {
							tinfo.setState(TaskInfo.ABORTED);
							task.dispose();
						} else {
							tinfo.setState(TaskInfo.DONE);

							// Template - Subtask completion behaviour.
							onSubTaskDone(task);

							task.dispose();
						}
						if (removeTaskAfterDisposal)
							messageQueue.add(new TaskEvent(task, SUBTASK_REMOVE));
						break;
					case SUBTASK_FAILED:
						if (aborting || failing) {
							tinfo.setState(TaskInfo.ABORTED);
							task.dispose();
						} else {
							tinfo.setState(TaskInfo.FAILED);

							onSubTaskFailed(task);
							task.dispose(); // added to handle weird lack of
							// disposal of failed STs
						}
						if (removeTaskAfterDisposal)
							messageQueue.add(new TaskEvent(task, SUBTASK_REMOVE));
						break;
					case SUBTASK_ABORTED:
						tinfo.setState(TaskInfo.ABORTED);

						// Template - Subtask aborted behaviour.
						onSubTaskAborted(task);

						task.dispose();

						if (removeTaskAfterDisposal)
							messageQueue.add(new TaskEvent(task, SUBTASK_REMOVE));
						break;
					case SUBTASK_REMOVE:
						// ####### THIS LINE IS TEMPORARY MAY NOT WORK !!!!!!!!!
						if (!removeTaskAfterDisposal)
							task.dispose();
						taskList.removeTask(task);
					}
				}

			}

			// Phase 3.
			taskLog.log(5, CLASS, name, "exec", "MTask: " + name + ": Starting Phase III.");

			// Start any pending which are triggerred.
			Iterator it = taskList.listAllTasks();
			while (it.hasNext()) {
				Task t = (Task) it.next();
				TaskInfo tinfo = taskList.getInfo(t);

				if (tinfo != null) {
					switch (tinfo.getState()) {

					case TaskInfo.PENDING:
						if (taskList.canRun(t)) {
							tinfo.setState(TaskInfo.RUNNING);
							// Initialize.
							t.init();
							// Start its exec thread.
							new TaskWorker(t.getName(), t).beginJob();
							taskLog.log(3, CLASS, name, "exec", "MTask: " + name + ": Found and started: "
									+ t.getName());
						}
						break;
					}
				}
			}

			// Phase 4.
			taskLog.log(5, CLASS, name, "exec", "MTask: " + name + ": Starting Phase IV.");
			// Check to see if ALL subtasks are in one of these states.
			// DONE, FAILED, ABORTED, SKIPPED - if any ONE of them is NOT
			// then we have NOT finished yet !

			int taskCount = 0;
			int finishCount = 0;
			it = taskList.listAllTasks();
			while (it.hasNext()) {
				Task t = (Task) it.next();
				TaskInfo tinfo = taskList.getInfo(t);
				taskCount++;
				if (tinfo != null) {
					switch (tinfo.getState()) {
					case TaskInfo.DONE:
					case TaskInfo.FAILED:
					case TaskInfo.ABORTED:
					case TaskInfo.SKIPPED:
					case TaskInfo.CANCELLED:
						finishCount++;
						break;
					}
				}
			}

			taskLog.log(4, CLASS, name, "exec", "MTask: " + name + ": Checked TaskList: Finished [" + finishCount + "/"
					+ taskCount + "] Tasks:");

			// We are either done, failed or aborted.

			if (finishCount == taskCount) {

				taskLog.log(4, CLASS, name, "exec", "MTask: " + name + ": Finished current list of Tasks:");

				// Task has Failed.
				if (failing) {

					failed = true;

					// Template - Failure behaviour.
					onFailure();

					// TODO notify statusListeners
					try {
						RCS_Controller.controller.getTaskMonitor().notifyListenersTaskFailed(
							descriptor, errorIndicator);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (manager != null) {
						taskLog.log(3, CLASS, name, "exec", "Task: " + name
								+ ": Failed - About to signal manager failed.");
						manager.sigTaskFailed(this);
					}
					return;
				}

				// Task has Aborted.
				// System.err.println(name+" Checking my aborting status: "+aborting);
				if (aborting) {
					aborted = true;
					// TODO notify statusListeners
					try {
						RCS_Controller.controller.getTaskMonitor().notifyListenersTaskAborted(
								descriptor, errorIndicator);
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.err.println(name + " Checking my manager to signal: " + manager);
					if (manager != null) {
						taskLog.log(3, CLASS, name, "exec", "MTask: " + name
								+ ": Aborted - About to signal manager aborted.");
						manager.sigTaskAborted(this);
						return;
					}
				}

				// Task is Done.
				if (exitOnCompletion) {

					done = true;

					// Template - Completion behaviour.
					onCompletion();
					// TODO notify statusListeners
					try {
						RCS_Controller.controller.getTaskMonitor().notifyListenersTaskCompleted(
								descriptor);
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (manager != null) {
						taskLog
								.log(3, CLASS, name, "exec", "MTask: " + name
										+ ": Done - About to signal manager done.");
						manager.sigTaskDone(this);
						return;
					}
				} else {
					taskLog.log(4, CLASS, name, "exec", "MTask: " + name + ": Done but still alive:");

				}
			}
			// Be nice to crappy Linux threading.
			// Thread.yield();
		}

	}

	/** Sends abort() to any running subtasks and cancels any pending. */
	protected void abortExecutingTasks() {

		Iterator it = taskList.listAllTasks();
		while (it.hasNext()) {
			Task t = (Task) it.next();
			TaskInfo tinfo = taskList.getInfo(t);

			if (tinfo != null) {
				switch (tinfo.getState()) {
				case TaskInfo.RUNNING:
					tinfo.setState(TaskInfo.ABORTING);
					if (t instanceof ParallelTaskImpl)
						((ParallelTaskImpl) t).setAbortCode(abortCode, abortMessage);
					t.abort();
					taskLog
							.log(3, CLASS, name, "abortSubtasks", "MTask: " + name + ":Aborting subtask: "
									+ t.getName());
					break;
				case TaskInfo.PENDING:
					tinfo.setState(TaskInfo.CANCELLED);
					taskLog.log(3, CLASS, name, "abortSubtasks", "MTask: " + name + ":Cancelled subtask: "
							+ t.getName());
					try {						
						RCS_Controller.controller.getTaskMonitor().notifyListenersTaskCancelled(
								t.getDescriptor());
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
	}

	/** Resets the specified subtask and sets its info-state to PENDING. */
	protected void resetFailedTask(Task task) {
		TaskInfo tinfo = taskList.getInfo(task);
		if (tinfo != null) {
			tinfo.setState(TaskInfo.PENDING);
			task.reset();
		}
	}

	/**
	 * Default subtask failure recovery handling.
	 * 
	 * This method looks up the recovery-context map to determine the specific
	 * action sequence for the subtask which failed. If no action is found then
	 * the default method is to call onSubTaskFailed().
	 */
	protected void defaultSubTaskFailed(Task task) {
		// Get execution context info.
		TaskInfo tinfo = taskList.getInfo(task);

		String key = (task != null ? task.getClass().getName() + ":" + task.getErrorIndicator().getErrorCode()
				: "no-task");

		ErrorIndicator err = task.getErrorIndicator();
		int errorCode = err.getErrorCode();
		String errMsg = err.getErrorString();

		// TaskRecoveryInfo rinfo = recovery.getTaskRecoveryInfo((Task) this,
		// task, errorCode);

		// taskLog.log(INFO, 4, CLASS, name, "defaultSubtaskFailed",
		// "Recovery context: " + rinfo + ", Not used for: ["
		// + key + "]");

		// /if (rinfo != null) {

		// if (rinfo.isDefault()) {
		// onSubTaskFailed(task);
		// return;
		// }
		//
		// / 1. Try retry (see how many times so far).
		// if (rinfo.isRetryEnabled()) {
		// if (tinfo.getRunCount() < rinfo.getMaxTries()) {
		//
		// resetFailedTask(task);
		// task.setDelay(rinfo.getDelay());
		// tinfo.incRunCount();
		// taskLog.log(INFO, 4, CLASS, name, "defaultSubtaskFailed", "MTask: " +
		// name + ": Retrying "
		// + task.getName() + " for the " + tinfo.getRunCount() + " of " +
		// rinfo.getMaxTries()
		// + " times");
		// return;
		// }
		// }

		// 2. See if we can skip.
		// if (rinfo.isSkipEnabled()) {
		// taskList.skip(task);
		// return;
		// 3. We have to fail.
		// } else {
		// // Are we passing the code onwards.
		// if (rinfo.isPassCode()) {
		// failed(err);
		// } else {
		// failed(rinfo.getFailCode(), "Task: " + name +
		// ": Eventually failed due to: " + task.getName()
		// + " : " + errMsg);
		// }
		// }
		// } else {

		// // Only call if there is no configured recovery ...
		onSubTaskFailed(task);
		// }

	}

	/**
	 * Called by this Task's manager to force this Task to abandon its execution
	 * and the execution of any sub Tasks it may have started.
	 */
	public void abort() {
		if (aborting || failing)
			return;
		taskLog.log(INFO, 3, CLASS, name, "MTask: " + name + ": Received abort signal");
		abortFlag = true;
	}

	/** Overridden temporarily - calls stop(timeout) on executing subtasks. */
	public void stop(long timeout) {
		if (aborting || failing)
			return;
		stopTime = System.currentTimeMillis() + timeout;
		taskLog.log(2, CLASS, name, "stop", "MTask: " + name + ": Received stop signal - must finish by: "
				+ sdf.format(new Date(stopTime)));
		stopFlag = true;
	}

	/**
	 * Get rid of any resources. Calls onDisposal() to allow subclasses to carry
	 * out any Task-specific clearing up. This method is usually called by a
	 * TaskManager not by a Task itself. onDisposal() must NOT make any
	 * reference to the taskList which has already been deleted.
	 */
	public void dispose() {
		taskList.clear();

		// Template - Disposal behaviour.
		onDisposal();
	}

	/**
	 * Puts the Task back into its pre-initialized state. DO NOT call this until
	 * the Worker has died off !
	 */
	public void reset() {
		taskLog.log(3, CLASS, name, "reset", "MTask: " + name + ": Resetting");
		done = false;
		failed = false;
		started = false;
		aborted = false;
		stopped = false;
		suspended = false;
		initialized = false;
		aborting = false;
		failing = false;
		stopping = false;
		worker = null;
		timeLimit = 0L;
		timeConstrained = false;

	}

	/**
	 * Calls failed with the supplied ErrorContext.
	 * 
	 * @param errorIndictator
	 *            An ErrorIndicator.
	 */
	protected void failed(ErrorIndicator errorIndicator) {
		if (aborting)
			return;
		this.errorIndicator = errorIndicator;
		failing = true;
		setAbortCode(errorIndicator.getErrorCode(), errorIndicator.getErrorString());
		taskLog.log(3, CLASS, name, "failed", "MTask: " + name + ": Starting failure-abort sequence: " + "\n Code: "
				+ errorIndicator.getErrorCode() + "\n Msg:  " + errorIndicator.getErrorString());
		abortExecutingTasks(); // do we want this here ????

		// maybe thats it ?

		// Template - Abort behaviour.
		// onAborting();

		// if (aborting)
		// return;
		// this.errorIndicator = errorIndicator;

		// setAbortCode(errorIndicator.getErrorCode(),
		// errorIndicator.getErrorString());
		// taskLog.log(3, CLASS, name, "failed", "MTask: " + name +
		// ": Starting failure-abort sequence: " + "\n Code: "
		// + errorIndicator.getErrorCode() + "\n Msg:  " +
		// errorIndicator.getErrorString());

		// failFlag = true;

	}

	/** Calls failed with the supplied ErrorContext. No exception is set. */
	protected void failed(int errorCode, String errorMessage) {
		failed(errorCode, errorMessage, null);
	}

	/**
	 * Sets the failed flag, builds an ErrorIndicator using the supplied
	 * parameters sets abortcode and calls abort() to terminate this Task and
	 * abort subtasks. If the Task has already failed at this point or is
	 * already aborting then returns silently without doing anything.
	 * 
	 * @param errorCode
	 *            The code number of the error.
	 * @param errorMessage
	 *            An associated descriptive String.
	 * @param ex
	 *            An associated Exception which caused the error - may be null.
	 */
	protected void failed(int errorCode, String errorMessage, Exception ex) {
		failed(new BasicErrorIndicator(errorCode, errorMessage, ex));
	}

	/**
	 * Returns this Task's ErrorIndicator if any.
	 * 
	 * @return This Task's ErrorIndicator, if the Task has failed this will have
	 *         been set (or copied from a subTask which failed). If the Task has
	 *         completed correctly then this will probably be null.
	 */
	public ErrorIndicator getErrorIndicator() {
		return errorIndicator;
	}

	/**
	 * Suspend execution of the current task. May be resumed later via a call to
	 * resume(). Note: This is NOT the same as block() which prevents a Task
	 * from being started. If TaskList.block(Task) is called on a running Task
	 * there is no effect.
	 */
	public void suspend() {
	}

	/**
	 * Resume execution of the current Task after a suspension (if it can be
	 * resumed - it may have completed anyway (this will be true for the lowest
	 * level Tasks which are <b>not</b> TaskManagers). This method should
	 * <b>only</b> be called from a Thread acting as a Worker for this Task and
	 * not e.g. by a SubTask's worker indirectly via a call to any of the
	 * sigXXX() methods.
	 */
	public void resume() {
	}

	/**
	 * Set the abort code and message - called just prior to abort() by manager.
	 */
	public void setAbortCode(int abortCode, String abortMessage) {
		this.abortCode = abortCode;
		this.abortMessage = abortMessage;
		taskLog.log(INFO, 3, " Setting abort status: Code: " + abortCode + ", Message: " + abortMessage);
	}

	/** Returns true if this Task is allowed to be aborted - defaults to true.. */
	public boolean canAbort() {
		return true;
	}

	/**
	 * Returns the current aborted state of this Task.
	 * 
	 * @return True if this Task is aborted.
	 */
	public boolean isAborted() {
		return aborted;
	}

	/**
	 * Returns the current completion state of this Task.
	 * 
	 * @return True if this Task is done.
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Returns the current error state of this Task.
	 * 
	 * @return True if this Task has failed.
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Returns the current suspension state of this Task.
	 * 
	 * @return True if this Task is suspended.
	 */
	public boolean isSuspended() {
		synchronized (suspendlock) {
			return suspended;
		}
	}

	/**
	 * Returns the current initialization state of this Task. This should return
	 * true if the init() method has been called.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Returns the current execution state of this Task. This should return true
	 * if the exec_task() method has been called.
	 */
	public boolean isStarted() {
		return started;
	}

	// --------------------------------------------------------------------------
	// TaskManager methods.
	// --------------------------------------------------------------------------
	// These methods are used to signal state changes to the TaskManager - in
	// the
	// case of TaskImpl these all just interrupt the single Worker and let it
	// decide for itself what the signal was.
	// --------------------------------------------------------------------------

	/**
	 * Called by a subTask to indicate to this (Manager) Task that the subTask
	 * has now completed and that the manager can safely dispose of it and move
	 * onto the next Task. The subTask should have set its <i>done</i> flag to
	 * indicate that it is complete.
	 * 
	 * @param task
	 *            The subTask which has completed.
	 */
	public void sigTaskDone(Task task) {
		messageQueue.add(new TaskEvent(task, SUBTASK_DONE));
	}

	/**
	 * Called by a subTask to indicate to this (Manager) Task that the subTask
	 * has failed for some reason. The subTask should have set its <i>failed</i>
	 * flag to indicate that this is the case and also its ErrorIndicator to
	 * reveal the nature of the problem.
	 * 
	 * @param task
	 *            The subTask which has failed.
	 */
	public void sigTaskFailed(Task task) {
		messageQueue.add(new TaskEvent(task, SUBTASK_FAILED));
	}

	/**
	 * Called by a subTask to indicate to this (Manager) Task that the subTask
	 * has 'successfully' aborted. The subTask should have set its
	 * <i>aborted</i> flag to indicate that this is so. Note this method calls
	 * onSubTaskAborted() which can be overwritten to carry out subclass
	 * specific operations - note however that dispose() is called on the
	 * subtask AFTER this.
	 * 
	 * @param task
	 *            The subTask which has aborted.
	 */
	public void sigTaskAborted(Task task) {
		messageQueue.add(new TaskEvent(task, SUBTASK_ABORTED));
	}

	/**
	 * Called by subTask to signal a message to its manager. The manager may use
	 * the information or just pass it on up the hierarchy or even throw it
	 * away. By convention - ParallelTaskImpl just passes the message up the
	 * hierarchy. Subclasses may intercept or throw away. Call
	 * super.sigMessage(..) before or after special handling as required to pass
	 * it upwards unmodified. Note also that this method is cascade called from
	 * a low level (executing) task so do not carry out time hungry processing
	 * which may risk a low level timeout here if this is neccessary consider
	 * asynchronous handoff.
	 * 
	 * @param task
	 *            The subTask which originated the message.
	 * @param category
	 *            An identifier to distinguish the type of message.
	 * @param message
	 *            The object carrying the message.
	 */
	public void sigMessage(Task source, int category, Object message) {
		if (manager != null)
			manager.sigMessage(source, category, message);
	}

	// ------------------------------------------------------------------------
	/**
	 * EventSubscriber method. (does nothing by default). Use this method to
	 * respond to event notifications. Typically use the onInit() method to
	 * register against with the EventRegistry for any alert clear and other
	 * events. Use the onDisposal() method to deregister from these same events
	 * - otherwise the ER will have a hanging reference to this Task after it
	 * has effectively died off.
	 */
	// ------------------------------------------------------------------------
	public void notifyEvent(String eventId, Object data) {
	}

	/**
	 * EventSubscriber method.
	 */
	public String getSubscriberId() {
		return name;
	}

}

/**
 * $Log: ParallelTaskImpl.java,v $ /** Revision 1.2 2007/09/07 20:29:05 snf /**
 * changed condition in init() to test for a task that fails to initialize
 * properly e.g during preInit(). /** /** Revision 1.1 2006/12/12 08:28:54 snf
 * /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:38 snf /** Initial
 * revision /** /** Revision 1.2 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.1 2001/06/08 16:27:27 snf /** Initial revision
 * /**
 */

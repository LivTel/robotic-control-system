/**
 * 
 */
package ngat.rcs.tms.executive;

import java.rmi.RemoteException;

import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskWorker;
import ngat.tcm.Telescope;
import ngat.tcm.TelescopeResponseHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public abstract class TelescopeExecutiveTask implements Task, TelescopeResponseHandler {

	LogGenerator logger;
	
	private TaskManager manager;

	private String name;

	private Telescope telescope;

	/** The Worker Thread (XT) for this Task. */
	protected TaskWorker worker;

	/** Holds an indicator of the Task's current error state (if any). */
	protected ErrorIndicator errorIndicator;

	/** Indicates whether the Task has been aborted by its manager. */
	protected volatile boolean aborted;

	/** Indicates whether the Task has been completed successfully. */
	protected volatile boolean done;

	/** Indicates whether the Task has failed for some reason. */
	protected volatile boolean failed;

	/** Indicates whether the Task has been suspended by its manager. */
	protected volatile boolean suspended;

	/** Indicates whether the Task has been initialized. */
	protected volatile boolean initialized;

	/** Indicates whether the Task has started execution. */
	protected volatile boolean started;

	/** Indicates whether the Task is to remain alive. */
	protected volatile boolean keepAlive;

	/** The time until this Task is expected to complete - may be extended. */
	protected long timeToComplete;

	/**
	 * Optional delay period this task should wait before implementing its
	 * exec_task() method after being called.
	 */
	protected long delay;

	/**
	 * Counts the number of times this Task has been initialized. Incremented by
	 * init(), but NOT zeroed by reset().
	 */
	protected int runCount;

	protected long startTime = 0L;
	protected long waitTime = 0L;

	/**
	 * Create a TelescopeExecutiveTask.
	 * 
	 * @param manager
	 *            The manager of the executive task.
	 * @param name
	 *            The task's name.
	 * @param telescope
	 *            A telescope to send executive actions to.
	 */
	public TelescopeExecutiveTask(TaskManager manager, String name, Telescope telescope) {
		super();
		this.manager = manager;
		this.name = name;
		this.telescope = telescope;
		Logger alogger = LogManager.getLogger("TASK");
		logger = alogger.generate()
			.system("RCS")
			.subSystem("TMM")
			.srcCompClass(this.getClass().getName())
			.srcCompId(name);

	}

	public void abort() {
		// note we should be careful here as we may be reset and get the
		// callback before we send !
		if (worker != null)
			worker.interrupt();
	}

	/** Returns true if this Task is allowed to be aborted - defaults to true. */
	public boolean canAbort() {
		return true;
	}

	public void dispose() {
		onDisposal();
	}

	public ErrorIndicator getErrorIndicator() {
		// TODO Auto-generated method stub
		return null;
	}

	public TaskManager getManager() {
		return manager;
	}

	public String getName() {
		return name;
	}

	public Thread getWorker() {
		return worker;
	}

	public void init() {
		runCount++;
		onInit();
		initialized = true;
		logger.create().extractCallInfo().info().level(3).msg("Completed initialization").send();
	}

	public boolean isAborted() {
		return aborted;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isFailed() {
		return failed;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void perform() {
		// TODO Auto-generated method stub
		long timeout = calculateTimeout();

		worker = (TaskWorker) Thread.currentThread(); // #### HOPEFULLY !!
		started = true;
		int count = 0;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
		startTime = System.currentTimeMillis();

		// now make a call supplying a timeout to the telcon method
		// need to supply something to handle the callback, then this thing
		// needs to go into
		// a wait loop timing out if nothing returns - in theory the telcon
		// should throw a
		// timeout itself if its well bahaved !!
		try {
			execTelescopeAction(this, timeout);
		} catch (Exception e) {
			// sonmething went wrong we failed...
			failed = true;
			// errorIndicator = new BasicErrorIndicator(errorCode, errorString,
			// exception);
		}
		// e.g. telescope.slew(target, timeout);

		while (!isAborted() && !isDone() && !isFailed()) {

			count++;
			try {

				waitTime = System.currentTimeMillis();
				Thread.sleep(timeout+20000L); // wait longer than actual timeout

			} catch (InterruptedException e) {
				// Either: done, failed, aborted, timedout
			}

		}

		// At this point we have timed out - if were NOT done then
		// we need to set a timeout error unless we've already been
		// told the failure reason by handleDone() with an errorcode.
		// Exit the loop immediately and thence this exec_task() call.

		if (isAborted()) {
			if (manager != null)
				manager.sigTaskAborted(this);
			return;
		}

		if (isDone()) {
			if (manager != null)
				manager.sigTaskDone(this);
			return;
		}

		if (isFailed()) {
			if (manager != null)
				manager.sigTaskFailed(this);
			return;
		}

		failed = true;
		// errorIndicator.setErrorCode(TIMEOUT_ERROR);
		// errorIndicator.setErrorString("Connection to server (" + connectionId
		// + ") timed-out after "
		// + (System.currentTimeMillis() - startTime) + " millis.");
		if (manager != null)
			manager.sigTaskFailed(this);

	}

	public void reset() {
		// TODO Auto-generated method stub
		aborted = false;
		done = false;
		failed = false;
		suspended = false;
		started = false;
		initialized = false;
	}

	public void resume() {
		// TODO Auto-generated method stub

	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void stop(long timeout) {
		// TODO Auto-generated method stub

	}

	public void suspend() {
		// TODO Auto-generated method stub

	}
  
    
    protected abstract long calculateTimeout();
    
	/** Override to carry out executive action. */
	protected abstract void execTelescopeAction(TelescopeResponseHandler handler, long timeout);

	/** Override to carry out initialization. */
	protected abstract void onInit();

	/** Override to handle completion. */
	//protected abstract void onCompletion(COMMAND_DONE response);

	/** Override to carry out disposal work. */
	protected abstract void onDisposal();

	/**
	 * Notification that the telescope control operation completed successfully.
	 * 
	 * @see ngat.tcm.TelescopeResponseHandler#telescopeOperationCompleted()
	 */
	public void telescopeOperationCompleted() throws RemoteException {
		// TODO Auto-generated method stub
		// interrupt the worker and set completion falg
		done = true;
		worker.interrupt();
	}

	/**
	 * Notification that the telescope control operation failed.
	 * 
	 * @see ngat.tcm.TelescopeResponseHandler#telescopeOperationFailed(int,
	 *      java.lang.String)
	 */
	public void telescopeOperationFailed(int errorCode, String errorMessage) throws RemoteException {
		// TODO Auto-generated method stub
		// interrupt the worker and set the failure status.
		failed = true;
		errorIndicator.setErrorCode(errorCode);
		errorIndicator.setErrorString(errorMessage);
		worker.interrupt();
	}

}

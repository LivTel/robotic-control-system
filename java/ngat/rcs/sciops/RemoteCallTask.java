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
package ngat.rcs.sciops;

import ngat.rcs.tms.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This base class represents a task which makes a call on a java.rmi remote
 * server. The method makeRemoteCall() should be overridden to make a call on
 * the specialized remote server which this task should have generated probably
 * during its init() call. The task needs some sort of specialized callback
 * object to register with the remote server to receive the callback. The
 * callBackCompleted() method should be called by the specific callback method
 * of the internal object, this frees up the wait lock and sets the completion
 * flag.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id:$
 * <dt><b>Source:</b>
 * <dd>$Source:$
 * </dl>
 * 
 * @author $Author:$
 * @version $Revision:$
 */
public abstract class RemoteCallTask implements Task {

	/** Stores the classname for this task. */
	protected String CLASS;

	/** Constant indicating that the remote call failed. */
	public static final int REMOTE_ERROR = 600007;

	/** Constant indicating that the remote call timed-out. */
	public static final int TIMEOUT_ERROR = 600006;

	/** The unique name/id for this TaskImpl. */
	protected String name;

	/** The Task's manager. */
	protected TaskManager manager;

	/** The Worker Thread (XT) fro this Task. */
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

	/** Startup delay setting. */
	private long delay;

	/** Records when the task is started. */
	private long startTime;

	/** Counts the number of restarts. */
	private volatile int runCount = 0;

	/** Personnal log-generator. */
	private LogGenerator logger;

	/**
	 * Signal object used bu internal Callback object to signal completion of
	 * operation.
	 */
	private BooleanLock keepAlive;

	private long timeout;

	private volatile boolean timedout;

	/** Create RemoteCallTask with supplied name and manager. */
	public RemoteCallTask(String name, TaskManager manager) {
		this.name = name;
		this.manager = manager;

		Logger alogger = LogManager.getLogger("TASK");
		logger = alogger.generate().system("RCS").subSystem("TMM").srcCompClass(this.getClass().getName()).srcCompId(
				name);

		keepAlive = new BooleanLock(true);
		CLASS = getClass().getName();
		errorIndicator = new BasicErrorIndicator(0, "OK", null);
	}

	/**
	 * Called by a Task's manager. Enters a loop waiting for the remote call to
	 * either timeout or complete. This is neccessary for the Task's Invokation
	 * Thread to remain alive.
	 */
	public void perform() {
		worker = (TaskWorker) Thread.currentThread(); // #### HOPEFULLY !!
		started = true;

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
		startTime = System.currentTimeMillis();

		// set the lock now, the callback may occur before we even enter the
		// wait section...
		setLock();

		// make the remote call, this can just fail so catch any exception and
		// then fail
		try {
			logger.create().extractCallInfo().info().level(3).msg("Making remote call").send();
			makeRemoteCall();
		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
			errorIndicator.setErrorCode(REMOTE_ERROR);
			errorIndicator.setErrorString("Call to remote server failed with exception: " + e);
			if (manager != null)
				manager.sigTaskFailed(this);
			return;
		}

		// Testing for completion condition - set by callback mechanism...
		// wait for -

		if (!isAborted() && !isDone() && !isFailed()) {

			try {
				logger.create().extractCallInfo().info().level(3).msg("Callback monitor Waiting for: " + timeout);

				waitOnLock(timeout);

			} catch (InterruptedException e) {
				// We are only interrupted if the mgr aborts us, we are done
				// or we have failed somehow. The test will cause us to
				// fall out if any of these happen and thence return.

				logger.create().extractCallInfo().info().level(3).msg("Interrupted while waiting on signal").send();

				timedout = true;

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

		// maybe were done...
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

		// synchronized (failLock) {
		failed = true;
		errorIndicator.setErrorCode(TIMEOUT_ERROR);
		errorIndicator.setErrorString("Cqll to remote server timed-out after "
				+ (System.currentTimeMillis() - startTime) + " millis.");
		if (manager != null)
			manager.sigTaskFailed(this);
		// }

	}

	/**
	 * Overwrite to make the special remote call, we already need to have the
	 * remote server reference (probably from the init() call)..
	 */
	protected abstract void makeRemoteCall() throws Exception;

	/** Wait on the signal for timeout. */
	public void waitOnLock(long timeout) throws InterruptedException {
		keepAlive.waitUntilFalse(timeout);
	}

	/** Sets the lock value false and notifies waiting thread (worker). */
	public void freeLock() {
		keepAlive.setValue(false);
	}

	/** Sets the lock value true and notifies waiting thread (worker). */
	public void setLock() {
		keepAlive.setValue(true);
	}

	/** Returns true if this Task is allowed to be aborted - defaults to true.. */
	public boolean canAbort() {
		return true;
	}

	/**
	 * Called by a Task's manager to force the Task to abandon its execution
	 */
	public void abort() {
		logger.create().extractCallInfo().info().level(3).msg("Got Abort signal").send();
		aborted = true;

		// may need to test if worker has died here ?
		if (worker != null)
			worker.interrupt();
	}

	/**
	 * Called by a Task's manager to cause a Task to abandon its execution
	 * 
	 * @param timeout
	 *            The period by which the Task should have reached a 'safe'
	 *            state.
	 */
	public void stop(long timeout) {
	}

	/**
	 * Called by a Task's manager to cause a Task to clear up its resources and
	 * prepare to be killed off. A Task need not be killed off by the manager as
	 * soon as it has completed its execution - there may be useful data stored
	 * in it. Subclasses may override onDisposal() to carry out specific clearup
	 * after the generic disposal has been performed.
	 */
	public void dispose() {
		onDisposal();
	}

	/** Cause the Task to suspend operation. ## NOT IMPLEMENTED ##. */
	public void suspend() {
	}

	/** Restart the Task after a suspend(). ## NOT IMPLEMENTED ##. */
	public void resume() {
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
		return suspended;
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

	/**
	 * Returns this Task's ErrorIndicator. If the Task has failed this should
	 * have been set.
	 * 
	 * @return This Task's ErrorIndicator.
	 */
	public ErrorIndicator getErrorIndicator() {
		return errorIndicator;
	}

	/**
	 * Return the name of this Task.
	 * 
	 * @return The name of this Task.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the task's manager.
	 * 
	 * @return The non null manager for this task.
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
	 * Reset the task parameters to allow this task to be reinserted into the
	 * taskList.
	 */
	public void reset() {
		runCount++;
		aborted = false;
		done = false;
		failed = false;
		suspended = false;
		started = false;
		initialized = false;

	}

	/**
	 * Override to carry out subclass specific initialization after main
	 * initialization.
	 */
	protected abstract void onInit();

	/** Override to carry out subclass specific disposal work. */
	protected abstract void onDisposal();

	/**
	 * Set the initial delay on startup (msecs).
	 * 
	 * @param delay
	 *            The delay period on startup (call on exec_task().
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * Returns the number of times this task has been initialized and probably
	 * executed..
	 */
	public int getRunCount() {
		return runCount;
	}

}

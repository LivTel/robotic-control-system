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
package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.base.*;

import javax.swing.tree.*;

/**
 * This base class represents a mapping from the basic JMSMA_ClientImpl
 * implementation of JMSMA_Client and Task. These are leaf Task's they do not
 * create any subtasks. They use an IConnection to send a COMMAND to some server
 * and handle the resulting COMMAND_DONE or error. Any ACKs received can be used
 * to keepAlive the Task and are propagated up its management hierarchy.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: JMSMA_TaskImpl.java,v 1.2 2007/09/06 11:28:14 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/JMSMA_TaskImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public abstract class JMSMA_TaskImpl extends JMSMA_ClientImpl implements Task {

	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, JMS = 0

	/** Stores the classname for this task. */
	protected String CLASS;

	/** The fudge factor for passing timeouts back up the Task Tree. */
	public static final double FUDGE = 1.1;

	/** Standard timeout to add to any passed on timeouts. */
	public static long STANDARD_TIMEOUT = 10000L;

	/** Constant indicating an unknown resource for the ConnectionFactory. */
	public static final int CONNECTION_RESOURCE_ERROR = 640001;

	/** Constant indicating an error during connection to the server. */
	public static final int CONNECT_ERROR = 640002;

	/**
	 * Constant indicating a general protocol error while talking to the server.
	 */
	public static final int GENERAL_ERROR = 640003;

	/**
	 * Constant indicating an error while waiting for a response from the
	 * server.
	 */
	public static final int RESPONSE_ERROR = 640004;

	/** Constant indicating an error during command despatch to server. */
	public static final int DESPATCH_ERROR = 640005;

	/** Constant indicating that the connection timed-out. */
	public static final int TIMEOUT_ERROR = 640006;

	
	/** Signal category indicating that an ACK has been received. */
	public static final int ACK_RECEIVED = 640001;

	/** The unique name/id for this TaskImpl. */
	protected String name;

	/** The Task's manager. */
	protected TaskManager manager;

	/** The Worker Thread (XT) fro this Task. */
	protected TaskWorker worker;

	/** Task's own config. */
	protected ConfigurationProperties config;

	/** Holds an indicator of the Task's current error state (if any). */
	protected ErrorIndicator errorIndicator;

	/** Task Config Registry. */
	protected static TaskConfigRegistry configRegistry;

	/** ConnectionFactory. */
	protected static ConnectionFactory connectionFactory;

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

	/** Synchronization object. */
	protected Object failLock;

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

	private TaskDescriptor descriptor;
	
	
	/**
	 * Create a JMSMA_TaskImpl with the specified parameters.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public JMSMA_TaskImpl(String name, TaskManager manager) {
		super(connectionFactory);
		piFactory = JMSMA_ProtocolImplFactory.getInstance();
		this.name = name;
		this.manager = manager;
		descriptor = new TaskDescriptor(name, getClass().getSimpleName());
		errorIndicator = new BasicErrorIndicator();
		failLock = new Object();
		logger = LogManager.getLogger("TASK");
		
		delay = 0L;
		runCount = 0;
		try {			
			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskCreated(
				manager.getDescriptor(),
				descriptor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a JMSMA_TaskImpl with the specified parameters.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 * @param cid
	 *            The connection resource id.
	 */
	public JMSMA_TaskImpl(String name, TaskManager manager, String cid) {
		this(name, manager);
		try {
			createConnection(cid);
		} catch (UnknownResourceException urx) {
			logger.log(1, CLASS, name, "Constructor", "No such connection resource: " + cid);
			failed = true;
			errorIndicator.setErrorCode(CONNECTION_RESOURCE_ERROR);
			errorIndicator.setErrorString("No such connection resource: " + cid);
			return;
		}
	}

	/**
	 * @return the descriptor
	 */
	public TaskDescriptor getDescriptor() {
		return descriptor;
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

	/** Set the Config Registry. */
	public static void setTaskConfigRegistry(TaskConfigRegistry r) {
		configRegistry = r;
	}

	// -------------
	// Task methods.
	// -------------

	/**
	 * Returns a snapshot of this Task's state and other information.
	 * 
	 * @param buffer
	 *            A buffer wherein to put the information.
	 * @param level
	 *            A tabulator level - can ignore.
	 */
	/*
	 * public void snapshot(StringBuffer buffer, int level) { String tabs =
	 * StringUtilities.pad(level * 3); String runTime = ""; if (startTime > 0L)
	 * { int ss = (int) ((System.currentTimeMillis() - startTime) / 1000); int
	 * mm = (int) (ss / 60); ss = ss - 60 * mm; runTime = "" + mm + "M " + ss +
	 * "S"; } else { runTime = "Not started"; } buffer.append("\n" + tabs +
	 * "Started:  " + runTime + "\n" + tabs + "Runcount: " + runCount + "\n" +
	 * tabs + "Timeout:  " + timeout + "\n" + tabs + "TTC:      " +
	 * timeToComplete); }
	 */

	/**
	 * Returns a snapshot of this Task's state and other information.
	 * 
	 * @param node
	 *            A TreeNode wherein to put the information.
	 */
	public void snapshot(TreeNode node) {
		// Add any useful info here
	}

	/**
	 * Called by a Task's manager to prepare the Task for imminent execution.
	 * The onInit() method is first called to allow subclasses to carry out any
	 * specific setup -they should use the method to create an appropriate
	 * connection using the connFactory via
	 * JMSMA_ClientImpl.createConnection(String) and to set any parameters
	 * needed for the ensuing call to calculateTimeToComplete() which works out
	 * the initial estimate of the Task's execution time.
	 */
	public void init() {

		// Locate our personal Task Config.
		config = configRegistry.getTaskConfig(this);

		runCount++;
		try {
			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskInitialized(descriptor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		onInit();
		keepAlive(true);
		timeout = calculateTimeToComplete();
		timeToComplete = STANDARD_TIMEOUT + (long) (FUDGE * timeout);
		initialized = true;
		logger.log(3, CLASS, name, "init", "JMSTask: " + name + ": Completed initialization");
		
	}

	/**
	 * Called by a Task's manager. Calls JMSMA_ClientImpl.exec() to start the
	 * CLientConnectionThread then enters a loop waiting for the CCT to either
	 * timeout or complete. This is neccessary for the Task's Invokation Thread
	 * to remain alive.
	 */
	public void perform() {
		worker = (TaskWorker) Thread.currentThread(); // #### HOPEFULLY !!
		started = true;
		try {
			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskStarted(descriptor);				
		} catch (Exception e) {
			e.printStackTrace();
		}
		int count = 0;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
		startTime = System.currentTimeMillis();
		logger.log(3, CLASS, name, "exec_task", "JMSTask:" + name + ": Starting execution thread using connection: "+connection);
		
		exec();

		logger.log(3, CLASS, name, "exec_task", "JMSTask:" + name + ": Started execution thread:");

		while (keepAlive() && !isAborted() && !isDone() && !isFailed()) {
			keepAlive(false);
			count++;
			try {
				logger.log(3, CLASS, name, "exec_task", "JMSTask:" + name + ": Waiting execution thread: cycle #" + count + " for: "
						+ timeToComplete+" ms");

				// Let the CCT timeout.
				waitTime = System.currentTimeMillis();

				waitFor(timeToComplete);

				logger.log(3, CLASS, name, "exec_task", "JMSTask:" + name + ": Joined execution thread: cycle #" + count
						+ " after: " + (System.currentTimeMillis() - waitTime) + " ms, time so far: "
						+ (System.currentTimeMillis() - startTime) + " ms");

			} catch (InterruptedException e) {
				// We are only interrupted if the mgr aborts us, we are done
				// or we have failed somehow. The loop test will cause us to
				// fall out if any of these happen and thence return.
				logger.log(3, CLASS, name, "exec_task", CLASS + " interrupted: " + e);
			}

		}

		// At this point we have timed out - if were NOT done then
		// we need to set a timeout error unless we've already been
		// told the failure reason by handleDone() with an errorcode.
		// Exit the loop immediately and thence this exec_task() call.

		if (isAborted()) {
			try {
				RCS_Controller.controller.getTaskMonitor().notifyListenersTaskAborted(
						descriptor, errorIndicator);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (manager != null)
				manager.sigTaskAborted(this);
			return;
		}

		if (isDone()) {
			try {
				RCS_Controller.controller.getTaskMonitor().notifyListenersTaskCompleted(
						descriptor);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (manager != null)
				manager.sigTaskDone(this);
			return;
		}

		if (isFailed()) {
			try {
				RCS_Controller.controller.getTaskMonitor().notifyListenersTaskFailed(
						descriptor, errorIndicator);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (manager != null)
				manager.sigTaskFailed(this);
			return;
		}

		synchronized (failLock) {
			failed = true;
			errorIndicator.setErrorCode(TIMEOUT_ERROR);
			errorIndicator.setErrorString("Connection to server (" + connectionId + ") timed-out after "
					+ (System.currentTimeMillis() - startTime) + " millis.");
			try {
				RCS_Controller.controller.getTaskMonitor().notifyListenersTaskFailed(
						descriptor, errorIndicator);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (manager != null)
				manager.sigTaskFailed(this);
		}

	}

	/** Returns true if this Task is allowed to be aborted - defaults to true.. */
	public boolean canAbort() {
		return true;
	}

	/**
	 * Called by a Task's manager to force the Task to abandon its execution
	 */
	public void abort() {
		logger.log(3, CLASS, name, "abort", "JMSTask:" + name + ": ** Just Got Abort signal **");
		aborted = true;
		// manager.sigTaskAborted(this);
		if (cct != null) {
			cct.terminate(); // Set the terminated flag.
			cct.abort(); // Cancel the JMSProtocolImplementor and zap I/O streams.
			cct.interrupt(); // Interrupt client connection thread - may not be needed.
		}

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
		// command = null;
		if (cct != null) {
			cct.interrupt(); // Interrupt client connection thread - may not be needed.
		}
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
	 * Returns the current 'keep-alive' state of this Task.
	 * 
	 * @return True if this Task is to be kept alive.
	 */
	public boolean keepAlive() {
		return keepAlive;
	}

	/**
	 * Sets the 'keep-alive' flag for this Task to indicate whether this Task
	 * should still be running.
	 * 
	 * @param keepAlive
	 *            True if this Task is to carry on running.
	 */
	public void keepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * Returns the timeout (time-to-live) for this Task.
	 * 
	 * @return Time to live for this Task.
	 */
	public long getTimeToComplete() {
		return timeToComplete;
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

	public void setManager(TaskManager mgr) {
		manager = mgr;
	}

	/**
	 * Returns this Task's Worker Thread. The Worker thread is generally
	 * allocated when the Task's exec() method is called.
	 */
	public Thread getWorker() {
		return worker;
	}

	// ---------------------
	// JMSMA_Client methods.
	// ---------------------

	/**
	 * Handles the Acknowledgement from the server. Resets the timeout to allow
	 * the ProtocolClientImpl to timeout its connection - this uses a fudge
	 * factor <a href = "#FUDGE">FUDGE</a> to ensure the connection does not
	 * timeout before the sender 'thinks' it will have sent the next ACK or DONE
	 * message. The same factor is used to multiply the timeout to give the <a
	 * href = "#timeToComplete">timeToComplete</a> which is used to put the
	 * invokation thread back to waiting by informing the manager. It will pass
	 * the message up the hierarchy until a manager which has access to the
	 * invokation thread can interrupt it - the thread then goes into wait for
	 * the new timeToComplete period. Finally the manager is signalled that the
	 * ack was received - most managers do not carry out processing of this
	 * signal but just pass it upwards. It is absolutely vital that the first
	 * manager to handle this signal does not spend long processing this message
	 * i.e. the method should either do nothing at all or if it does any
	 * operations should immediately check the category and return if it does
	 * not care about ACK_RECEIVED or, if it does any processing on ACK_RECEIVED
	 * which could take more than a few millis, asynchronous handoff should be
	 * used.
	 * 
	 * @param ack
	 *            The ACK received from the (remote) server.
	 */
	public void handleAck(ACK ack) {
		synchronized (this) {
			timeout = STANDARD_TIMEOUT + (long) (FUDGE * ack.getTimeToComplete());
			timeToComplete = STANDARD_TIMEOUT + (long) (FUDGE * timeout);
			keepAlive(true);
			manager.sigMessage(this, ACK_RECEIVED, ack);
		}
	}

	/**
	 * Handles the results (Done) from the server. Prior to setting done flag or
	 * error flag and informing the manager, subclasses may override the
	 * onCompletion() to carry out any specific processing - if this is likely
	 * to be time-consuming the method should first set keepAlive and update the
	 * TTC for the time required. Note that TaskImpl-type managers call
	 * onSubTaskCompleted() after the subtask calls onCompletion() to carry out
	 * managerial work. If the DONE message indicates an error condition
	 * occurred (upstream) this is passed on via the Task's ErrorIndicator -
	 * i.e. copied from the COMMAND_DONE.
	 * 
	 * @param done
	 *            The COMMAND_DONE received from the server.
	 */
	public void handleDone(COMMAND_DONE response) {
		synchronized (this) {
			if (response.getSuccessful()) {
				done = true;
				onCompletion(response);
				// manager.sigTaskDone(this);
				worker.interrupt();
			} else {
				if (!aborted) {
					synchronized (failLock) {
						failed = true;
						errorIndicator.setErrorCode(response.getErrorNum());
						errorIndicator.setErrorString(response.getErrorString());
						// manager.sigTaskFailed(this);
						worker.interrupt();
					}
				}
			}
		}
	}

	// These method signatures are used to allow the ProtocolImpl
	// to pass back information as to where and how any I/O error
	// occurred. If a concrete class does not care about the details
	// it can just have these delegate to exceptionOccurred or similar.

	/** Handles a failed attempt at connection. */
	public void failedConnect(Exception e) {
		synchronized (failLock) {
			if (!aborted) {
				failed = true;
				errorIndicator.setException(e);
				errorIndicator.setErrorCode(CONNECT_ERROR);
				errorIndicator.setErrorString("Connection failed: ");
				// manager.sigTaskFailed(this);
				worker.interrupt();
			}
		}
	}

	/** Handles a failed COMMAND despatch. */
	public void failedDespatch(Exception e) {
		synchronized (failLock) {
			if (!aborted) {
				failed = true;
				errorIndicator.setException(e);
				errorIndicator.setErrorCode(DESPATCH_ERROR);
				errorIndicator.setErrorString("Send command failed: ");
				// manager.sigTaskFailed(this);
				worker.interrupt();
			}
		}
	}

	/** Handles a failed-to-receive ACK or DONE message. */
	public void failedResponse(Exception e) {
		synchronized (failLock) {
			if (!aborted) {
				failed = true;
				errorIndicator.setException(e);
				errorIndicator.setErrorCode(RESPONSE_ERROR);
				errorIndicator.setErrorString("Response to command failed: ");
				// manager.sigTaskFailed(this);
				worker.interrupt();
			}
		}
	}

	/**
	 * Handles a general exception in the CCT's execution of the client protocol
	 * implementation. This could be due to a non-existant command or anything
	 * else which might conceivably happen e.g NullPointer etc.
	 */
	public void exceptionOccurred(Object source, Exception e) {
		synchronized (failLock) {
			if (!aborted) {
				failed = true;
				errorIndicator.setException(e);
				errorIndicator.setErrorCode(GENERAL_ERROR);
				errorIndicator.setErrorString("General protocol exception from: " + source);
				// manager.sigTaskFailed(this);
				worker.interrupt();
			}
		}
	}

	// ------------
	// Own methods.
	// ------------

	/**
	 * Reset the task parameters to allow this task to be reinserted into the
	 * taskList.
	 */
	public void reset() {
		// delay = 0L;
		aborted = false;
		done = false;
		failed = false;
		suspended = false;
		started = false;
		initialized = false;
		keepAlive = true;
		try {
			createConnection(connectionId);
			logger.log(3, CLASS, name, "reset", "Reconnected to: " + connectionId);
		} catch (UnknownResourceException urx) {
			logger.log(1, CLASS, name, "reset", "Failed to reconnect: " + urx);
			failed = true;
			errorIndicator.setErrorCode(CONNECTION_RESOURCE_ERROR);
			errorIndicator.setErrorString("No such connection resource: " + connectionId);
			return;
		}
		System.err.println("JMSMA-TI::Done Resetting " + getName());
	}

	/**
	 * Must be overridden to compute the estimated completion time.
	 * 
	 * @return The initial estimated completion time in millis.
	 */
	protected abstract long calculateTimeToComplete();

	/** Override to carry out subclass specific initialization. */
	protected abstract void onInit();

	/**
	 * Override to carry out subclass specific completion work This might
	 * include updating a global resource with the results of this task - i.e.
	 * the data returned by the COMMAND_DONE. The Task's manager may want this
	 * data to carry out processing in the onSubTaskDone() method called after
	 * exec() returns.
	 * 
	 * @param response
	 *            The data returned by the connection.
	 */
	protected abstract void onCompletion(COMMAND_DONE response);

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

/**
 * $Log: JMSMA_TaskImpl.java,v $ /** Revision 1.2 2007/09/06 11:28:14 snf /**
 * added test for worker null... /** /** Revision 1.1 2006/12/12 08:28:27 snf
 * /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:16 snf /** Initial
 * revision /** /** Revision 1.4 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.3 2001/06/08 16:27:27 snf /** Modified for use
 * with parallel tasks. /** /** Revision 1.2 2001/04/27 17:14:32 snf /** backup
 * /** /** Revision 1.1 2001/02/16 17:44:27 snf /** Initial revision /**
 */

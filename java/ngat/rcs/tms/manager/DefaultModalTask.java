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
import ngat.astrometry.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Default implementation of ModalTask.
 */
public class DefaultModalTask extends ModalTask implements TaskModeController,
		TaskModeControllerManagement {

	// /** Default time to wait before yielding to control election (millis). */
	//public static final long DEFAULT_POLLING_DELAY = 1000L;

	///** Time to wait before yielding to control election (millis). */
	//protected final long pollingDelay;

	/**
	 * Agent-specific properties. The agent is responsible for updating these
	 * when neccessary.
	 */
	protected Properties properties;

	/** Reason for not accepting control. */
	protected String notAcceptableReason;

	/** Mode controller interface hook. */
	// protected BasicModeController bmc;

	/** Flag to indicate that controller is promoted. */
	protected boolean promoted;

	/** Flag to indicate that the controller is enabled. */
	protected boolean enabled;

	protected List listeners;

	protected TaskSequence controlSequence;

	/**
	 * Create a DefaultModalTask.
	 * 
	 * @param name
	 *            The name of this Task.
	 * @param manager
	 *            This Task's manager.
	 */
	public DefaultModalTask(String name, TaskManager manager) {
		super(name, manager);
		//pollingDelay = DEFAULT_POLLING_DELAY;
		properties = new Properties();
		listeners = new Vector();
		enabled = true;
	}

	/** Creates the specific subclass as a static instance. */
	protected static ModalTask createModalTask(String name, TaskManager manager)
			throws Exception {
		return new DefaultModalTask(name, manager);
	}

	/**
	 * Override to configure from File. This implementation does nothing.
	 * 
	 * @param file
	 *            Configuration file.
	 * @exception IOException
	 *                If any problem occurs reading the file or does not exist.
	 * @exception IllegalArgumentException
	 *                If any config information is dodgy.
	 */
	@Override
	public void configure(File file) throws IOException,
			IllegalArgumentException {
	}

	/**
	 * Override to extract relevant history from the history map.
	 * 
	 * @param history
	 *            The history.
	 * @exception IOException
	 *                If any problem occurs reading files.
	 * @exception IllegalArgumentException
	 *                If any history information is dodgy.
	 */
	@Override
	public void loadHistory(Map history) throws IOException,
			IllegalArgumentException {
	}

	/** Returns agent-specific properties. */
	@Override
	public Properties getAgentProperties() {
		return properties;
	}

	/**
	 * Overriden to return the time at which this ModalControlAgent will next
	 * request control. ##### CURRENTLY FAKED TO RETURN NOW ########
	 * 
	 * @return Time when this MCA will next want/be able to take control (millis
	 *         1970).
	 */
	@Override
	public long demandControlAt() {
		ObsDate obsDate = RCS_Controller.getObsDate();
		long now = System.currentTimeMillis();
		return now;
	}

	/** Return true if the agent will want control at time- default false. */
	public boolean willWantControlAt(long time) {
		return false;
	}

	/**
	 * Override to return <i>true</i> if this ModalTask is willing to accept
	 * control at this instant.
	 */
	@Override
	public boolean acceptControl() {
		return promoted;
	}

	/** Returns the reason for non-acceptance of control. */
	@Override
	public String getNonAcceptanceReason() {
		return notAcceptableReason;
	}

	/**
	 * TaskSequencer method - Attempts to find a subtask to control - if this
	 * fails, waits for a polling delay then sends YIELD to surrender control.
	 * The method beforeStartNextJob() is called before starting the new subtask
	 * to allow subclasses (actual MCAs) to perform nay pre-job initialization.
	 * Note: If there are no jobs to do, this method will be called every poll
	 * interval so watch what it does.
	 */
	@Override
	public void nextJob() {

		beforeStartJob();

		Task tt = getNextJob();
		taskLog.log(3, CLASS, name, "nextJob", "GetNextJob: Returned: "
				+ (tt == null ? "NULL" : tt.getClass().getName()));
		if (tt != null) {
			// Do it !
			if (manage(tt)) {
				taskLog.log(3, CLASS, name, "nextJob", "Successfully managed: "
						+ tt.getName());
				TaskOperations.getInstance().getOperationsManager()
						.notifyOpsListenersOperationStarting(tt.getName());
			} else {
				taskLog.log(3, CLASS, name, "nextJob", "Failed to manage: "
						+ tt.getName());
				yieldControl();
			}
		} else {
			yieldControl();
		}

	}

	/**
	 * Send a YIELD request after a short delay in order to present for
	 * re-election.
	 */
/*	protected void yieldControl() {
		taskLog.log(3, CLASS, name, "yield", "About to yield control.");
		Runnable r = new Runnable() {
			public void run() {
				try {
					Thread.sleep(pollingDelay);
				} catch (InterruptedException e) {
				}
				EventQueue.postEvent(RCS_ControlTask.TASK_YIELD_MESSAGE);
			}
		};
		(new Thread(r)).start();
	}*/

	/** Override to return a new job or null (Default). */
	public Task getNextJob() {
		return null;
	}

	/**
	 * Called by nextJob() before the new job (if any) is started, i.e. this is
	 * called every time nextJob() is called whether a subtask is actually
	 * managed or not. This implementation does nothing - subclasses may
	 * override (with care !).
	 */
	protected void beforeStartJob() {
	}

	// public void createModeController() throws Exception {
	// bmc = new BasicModeController(this);
	// }

	// public TaskModeController getModeController() {
	// return bmc;
	// }

	/** Return true if wants control at time. */
	public boolean wantsControl(long time) throws RemoteException {
		return false;
	}

	public long nextWantsControl(long time) throws RemoteException {
		return time + 24 * 3600 * 1000L;
	}

	/** Returns the mode's priority. */
	public int getPriority() throws RemoteException {
		return getAgentPriority();
	}

	/** Returns the name of the mode. */
	public String getModeName() throws RemoteException {
		return getAgentId();
	}

	public String getModeDescription() throws RemoteException {
		return this.toString();
	}

	/** Promote this controller. */
	public void promote() throws RemoteException {
		promoted = true;
	}

	/** Demote this controller. */
	public void demote() throws RemoteException {
		promoted = false;
	}

	/**
	 * Request the controller to execute the supplied sequence, reporting back
	 * to a listener.
	 */
	public void executeControlSequence(TaskSequence ts, TaskSequenceListener tl)
			throws RemoteException {
		if (!listeners.contains(tl))
			listeners.add(tl);
		controlSequence = ts;
		// make up some sort of TaskList here or a single entry point task with
		// that
		// sequence under it.
	}

	public void disable() throws RemoteException {
		enabled = false;
		taskLog.log(2, getModeName() + " requested to Disable");
	}

	public void enable() throws RemoteException {
		enabled = true;
		taskLog.log(2, getModeName() + " requested to Enable");
	}

	public boolean isEnabled() throws RemoteException {
		return enabled;
	}

}

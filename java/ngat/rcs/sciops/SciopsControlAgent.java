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

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.iss.*;
import ngat.phase2.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import java.util.*;
import java.rmi.RemoteException;
import java.text.*;
import java.io.*;

/**
 * This controller is used for experimental work.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SciopsControlAgent.java,v 1.1 2006/12/12 08:26:53 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/experimental/RCS/SciopsControlAgent.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class SciopsControlAgent extends DefaultModalTask implements Logging {

	protected static SciopsControlAgent instance;

	/** Standard date format. */
	static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	/** Default delay of start of Science Operations after sunset (mins). */
	public static final int DEFAULT_START_DELAY = 60;

	/**
	 * Default period to sleep if no scheduled groups available or scheduler
	 * offline (mins).
	 */
	public static final int DEFAULT_SLEEP_PERIOD = 15;

	/** Implements the lockfile associated with the group instantiation counter. */
	protected PersistentUniqueInteger puid;

	/**
	 * Counts the number of groups so far since start of observing. This number
	 * is maintained over reboots during the night.
	 */
	protected int groupCounter;

	/**
	 * End of a period until which we will refuse control - e.g. no groups
	 * visible at present.
	 */
	protected long sleepPeriodEnd;

	/** How long we sleep if no targets visible (millis). */
	protected long sleepPeriod;

	/** Counts the number of Group sequences so far (this session). */
	protected int groupSequenceCount;

	/**
	 * Create an SciopsControlAgent using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public SciopsControlAgent(String name, TaskManager manager) {
		super(name, manager);

	}

	/**
	 * Creates the initial instance of the BackgroundControlAgent
	 * 
	 */
	@Override
	public void initialize(ModalTask tm) {
		instance = (SciopsControlAgent) tm;
	}

	/** Returns a reference to the singleton instance. */
	public static ModalTask getInstance() {
		return instance;
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
	@Override
	public void configure(File file) throws IOException, IllegalArgumentException {
		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));

		// how long to sleep if the scheduler is offline or no groups available
		sleepPeriod = 60 * 1000 * config.getLongValue("sleep.period", DEFAULT_SLEEP_PERIOD);

		taskLog.log(1, CLASS, name, "Config", "SOCA was configured ok");
	}

	/**
	 * Overridden to carry out specific work after the init() method is called.
	 * Sets a number of FITS headers and subscribes to any required events.
	 */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(1, CLASS, name, "onInit", "\n**********************************************************"
				+ "\n**    Science Operations Control Agent is initialized   **"
				+ "\n**********************************************************\n");
		opsLog.log(1, "Starting Science Operations Mode.");
		FITS_HeaderInfo.current_TELMODE.setValue("ROBOTIC");
		//FITS_HeaderInfo.current_COMPRESS.setValue("NONE");
		// Setup group instantiation counter.
		puid = new PersistentUniqueInteger("%%group");

		try {
			groupCounter = puid.get();
		} catch (Exception e) {
			System.err.println("** WARNING - Unable to read initial group counter: " + e);
			taskLog.log(1, CLASS, name, "onInit", "Error reading initial group counter: " + e);
			taskLog.dumpStack(1, e);
			groupCounter = 0;
		}
		// Always override until we decide not to.
		//ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);
		//ngat.rcs.iss.ISS_AG_STOP_CommandImpl.setOverrideForwarding(false);
		
		// Dont forward MOVE_FOLDs sent by instruments, we do this ourselves in SOCA
		//ngat.rcs.iss.ISS_MOVE_FOLD_CommandImpl.setOverrideForwarding(true);
	}

	/** Deal with failed subtask. */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);

		// just in case a stop-axes failed, we don't really care
		if (task instanceof StopTask) {
			taskLog.log(1, CLASS, name, "onSubTaskFailed", "WARNING - Stop axes failed: " + task.getName());
			return;
		}

		// this can only be due to the scheduler failing to find a job or being
		// offline
		// either way we back off a while and let BGCA take over.
		sleepPeriodEnd = System.currentTimeMillis() + sleepPeriod;
		taskLog.log(1, CLASS, name, "onSubTaskFailed", "No science observations available so sleeping until: "
				+ sdf.format(new Date(sleepPeriodEnd)));

	}

	/**
	 * Overridden to carry out specific work when a subtask fails.
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
	}

	@Override
	public void onAborting() {
		super.onAborting();
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		opsLog.log(1, "Completed Science Operations Mode.");
	}

	/** Return true if wants control at time. */
	@Override
	public boolean wantsControl(long time) throws RemoteException {
		
		if (!enabled) {
			notAcceptableReason = "DISABLED";
			return false;
		}
		
		if (time < sleepPeriodEnd) {
			notAcceptableReason = "SLEEPING";
			System.err.println("SOCA: Sleeping until: " + sdf.format(new Date(sleepPeriodEnd)));
			return false;
		}

		Site site = RCS_Controller.controller.getObservatorySite();

		Position sun = Astrometry.getSolarPosition(time);

		double sunElev = sun.getAltitude(time, site);
		boolean sunup = (sunElev > 0.0);

		return (!sunup);
	}

	@Override
	public long nextWantsControl(long time) throws RemoteException {

		ObsDate obsDate = RCS_Controller.getObsDate();

		if (obsDate.isPreNight(time))
			return obsDate.getSunset();
		else if (obsDate.isPostNight(time))
			return obsDate.getSunset() + ObsDate.ONE_DAY;
		else {
			// we are in night. If now is less than SPE we are sleeping
			// probably
			if (time < sleepPeriodEnd)
				return sleepPeriodEnd;
			else
				return time + 24 * 3600 * 1000L;
		}

	}

	/**
	 * Returns the next available job:-
	 */
	@Override
	public Task getNextJob() {
		
		// Check how long till our own timLimit expires, use as disruptor limit.
		((DefaultMutableAdvancedBookingModel)RCS_Controller.controller.getBookingModel())
			.addBooking(new XTimePeriod(runStartTime  + timeLimit, runStartTime + timeLimit+3600*1000L));

		SciopsSequenceTask sst = new SciopsSequenceTask(name + "/SS(" + (groupSequenceCount++) + ")", this);

		try {
			groupCounter = puid.increment();
		} catch (Exception e) {
			taskLog.log(1, CLASS, name, "getNextJob", "Building symlink: Failed to increment group counter: " + e);
		}

		sst.setGroupCounter(groupCounter);
		return sst;
	}

	/** Dummy impl - does nothing special. */
	@Override
	protected TaskList createTaskList() {
		return taskList;
	}

}

/**
 * $Log: SciopsControlAgent.java,v $ /** Revision 1.1 2006/12/12 08:26:53 snf
 * /** Initial revision /**
 */

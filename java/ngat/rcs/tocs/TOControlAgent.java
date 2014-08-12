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
package ngat.rcs.tocs;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.iss.*;
import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.rmi.*;

/**
 * This Task creates a series of subTasks to carry out the Target-Of-Opportunity
 * Operations plan .
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOControlAgent.java,v 1.3 2007/08/03 08:48:38 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOControlAgent.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.3 $
 */
public class TOControlAgent extends DefaultModalTask implements EventSubscriber, Logging {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_CTRL= 800
	
	/** Abort due to override by higher service. */
	public static final int OVERRIDDEN = 650801;

	/** Default Service priority (= lowest priority). */
	public static final int DEFAULT_PRIORITY = 10;

	/** Default task queue size. */
	public static final int DEFAULT_QUEUE_SIZE = 10;

	/**
	 * Default instrument for service - ##### THIS ITSELF NEEDS TO BE
	 * CONFIGURABLE ####.
	 */
	public static final String DEFAULT_INSTRUMENT = "RATCAM";

	/** Singleton instance. */
	protected static TOControlAgent instance;

	/**
	 * Time after start of TO mode when the RCS should revert to previous
	 * operating mode.
	 */
	public static final long DEFAULT_OVERRIDE_TIME = 30 * 60 * 1000L;

	public static final int DEFAULT_TOFS_PORT = 7166;
	public static final File DEFAULT_TOFS_KEY_FILE = new File("toop/certs/itr.private");
	public static final String DEFAULT_TOFS_PASS = "geronimo";
	public static final File DEFAULT_TOFS_TRUST_FILE = new File("toop/certs/server.public");
	public static final int DEFAULT_BUFFER_LENGTH = 2048;
	public static final File DEFAULT_TOFS_BASE_DIR = new File("/occ/tmp");

	/** Standard date format. */
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	/** Session log format. */
	static SimpleDateFormat qdf = new SimpleDateFormat("yyyy-MMM-dd-HHmmss");

	/** SessionID (short) formatter. */
	static SimpleDateFormat pdf = new SimpleDateFormat("HHmmss");

	static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/** True if an ALERT has been initiated. Set False when alert clears. */
	protected boolean alertInProgress;

	/** List of jobs to start. */
	protected List jobs;

	/** Actual job-queue size. */
	protected int jobQueueSize;

	/** Current Service descriptor. */
	protected ServiceDescriptor currentService;

	/** Nominated new Service descriptor. */
	protected ServiceDescriptor newService;

	/** A command implementor to reply to the SA HELO message. */
	protected TOC_GenericCommandImpl newServiceImpl;

	/** Set of services available. */
	protected SortedMap services;

	/** The TO File Transfer Server. */
	protected SSLFileTransfer.Server tofServer;

	/** Flag to permit daytime operation. */
	boolean overridden;

	/** Flag to indicate that daytime operation is required. */
	boolean enabled;

	private Logger tofsLog;

	/**
	 * Create a TOControlAgent using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOControlAgent(String name, TaskManager manager) {
		super(name, manager);
		services = new TreeMap();
		jobs = new Vector();
		sdf.setTimeZone(UTC);
		qdf.setTimeZone(UTC);

		tofServer = new SSLFileTransfer.Server("TOFS");

	}

	/**
	 * Creates the initial instance of the TOOP_Ops_Task
	 */
	@Override
	public void initialize(ModalTask tm) {
		instance = (TOControlAgent) tm;
		EventRegistry.subscribe("TO_ALERT", instance);
		EventRegistry.subscribe("TO_CLEAR", instance);
		EventRegistry.subscribe("TO_MESG", instance);

		TOC_Server.setAccept(true);
	}

	/** Returns a reference to the singleton instance. */
	public static ModalTask getInstance() {
		return instance;
	}

	/**
	 * Configure from File. Sets timelimits, Configures the SA scripts and relay
	 * host/ports etc.
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

		String name = null;
		String property = null;
		String id = null;
		// String controlHost = null;
		long salloc = 0L;
		long palloc = 0L;
		int priority = DEFAULT_PRIORITY;
		String defInst = null;

		jobQueueSize = config.getIntValue("job.queue.size", DEFAULT_QUEUE_SIZE);

		Enumeration e = config.propertyNames();
		while (e.hasMoreElements()) {
			property = (String) e.nextElement();
			if (property.indexOf(".ID") != -1) {
				// Actual Service name.
				name = property.substring(0, property.indexOf(".ID"));
				System.err.println("Found TO Service: " + property);
				// Get other params.
				id = config.getProperty(property); // Service ID.
				// controlHost = config.getProperty (name+".control.host");
				salloc = config.getLongValue(name + ".session.allocation", 0L);
				palloc = config.getLongValue(name + ".period.allocation", 0L);
				priority = config.getIntValue(name + ".priority", DEFAULT_PRIORITY);
				defInst = config.getProperty(name + ".default.instrument", DEFAULT_INSTRUMENT);

				ServiceDescriptor sd = new ServiceDescriptor(id, salloc, priority);
				sd.defaultInstrument = defInst;
				sd.periodAllocation = palloc;
				sd.timeRemaining = palloc;

				sd.tagId = config.getProperty(name + ".tag", "UNKNOWN");
				sd.userId = config.getProperty(name + ".user", "UNKNOWN");
				sd.proposalId = config.getProperty(name + ".proposal", "UNKNOWN");
				sd.groupId = config.getProperty(name + ".group", "UNKNOWN");
				sd.progId = config.getProperty(name + ".program", "UNKNOWN");
				services.put(id, sd);

				taskLog.log(1, CLASS, "Loaded TO Service Info for: " + sd);

			}
		}

		// TOF Server options.

		int tofsPort = config.getIntValue("tofs.port", DEFAULT_TOFS_PORT);

		File tofsKeyFile = config.getFile("tofs.key.file", DEFAULT_TOFS_KEY_FILE);

		String tofsPassword = config.getProperty("tofs.key.password", DEFAULT_TOFS_PASS);

		File tofsTrustFile = config.getFile("tofs.trust.file", DEFAULT_TOFS_TRUST_FILE);

		// defult to authentication.
		boolean auth = (config.getProperty("tofs.noauth") == null);

		int bufferLength = config.getIntValue("tofs.buffer.length", DEFAULT_BUFFER_LENGTH);

		File tofsBaseDir = config.getFile("tofs.base.dir", DEFAULT_TOFS_BASE_DIR);

		SSLFileTransfer.setLogger("TOCS");

		tofServer.setKeyFile(tofsKeyFile);
		tofServer.setKeyPass(tofsPassword);
		tofServer.setTrustFile(tofsTrustFile);

		tofServer.setBaseDirectory(tofsBaseDir);

		tofServer.setSingleThreaded(config.getProperty("tofs.single.threaded") != null); // defaults
																							// to
																							// multi-threaded
		tofServer.setSecureIncoming(config.getProperty("tofs.secure") != null); // defaults
																				// to
																				// non-secure
		tofServer.setOldProtocol(config.getProperty("tofs.use.old.protocol") != null); // defaults
																						// to
																						// new
																						// protocol

		// Setup logging.
		tofsLog = LogManager.getLogger("TOCS");

		try {
			tofServer.bind(tofsPort, auth, false);
			tofServer.start();
		} catch (Exception se) {
			tofsLog.log(1, "Failed to bind TOF Server: " + se);
		}

	}

	/**
	 * Creates the TaskList for this TaskManager. Starts off with NO tasks as we
	 * havn't received any requests yet from a TOOP_ServiceHandler.
	 */
	@Override
	protected TaskList createTaskList() {
		return taskList;
	}

	/**
	 * Overridden to carry out specific work after the init() method is called.
	 * Sets a number of FITS headers and subscribes to any required events.
	 */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(1, CLASS, name, "onInit", "\n********************************************************"
				+ "\n** Target-Of-Opportunity Control Agent is initialized **"
				+ "\n********************************************************\n");
		opsLog.log(1, "Starting Target-Of-Opportunity Operations [TOCS] Mode.");
		FITS_HeaderInfo.current_TELMODE.setValue("REACTIVE");
		//FITS_HeaderInfo.current_COMPRESS.setValue("PROFESSIONAL");

		FITS_HeaderInfo.current_TAGID.setValue("TOOP");
		FITS_HeaderInfo.current_USERID.setValue("UNKNOWN");
		FITS_HeaderInfo.current_PROGID.setValue("UNKNOWN");
		FITS_HeaderInfo.current_PROPID.setValue("UNKNOWN");
		FITS_HeaderInfo.current_GROUPID.setValue("UNKNOWN");
		FITS_HeaderInfo.current_OBSID.setValue("UNKNOWN");

		FITS_HeaderInfo.clearAcquisitionHeaders();

		// Always override until we decide not to.
		ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);

	}

	/**
	 * Override to return <i>true</i> if it is 'nighttime' i.e. The sun is set
	 * as calculated by Astrometry, or a SUNSET trigger has occurred.
	 */
	@Override
	public boolean acceptControl() {
		ObsDate obsDate = RCS_Controller.getObsDate();
		long now = System.currentTimeMillis();

		// These let TO run during the day.
		if (overridden) {
			if (enabled)
				notAcceptableReason = null;
			else
				notAcceptableReason = "MANUAL_OVERRIDE";

			return enabled;
		}

		// Normally TO does not run in daytime.
		if (obsDate.isPreNight(now) || obsDate.isPostNight(now)) {
			// System.err.println("**"+obsDate.getTimePeriod(now)+
			// "- TO AGENT Not Feasible:");
			notAcceptableReason = "NOT_NIGHT";
			return false;
		} else {
			// Needs setting by any new SAs when they are generated and
			// unset when they are killed off for some reason.
			if (alertInProgress) {
				notAcceptableReason = null;
				return true;
				// System.err.println("**"+obsDate.getTimePeriod(now)+
				// "- TO AGENT Feasible:");
			} else
				notAcceptableReason = "NO_ALERTS_IN_FORCE";
		}

		return false;
	}

	/**
	 * Overriden to return the time at which this ModalControlAgent will next
	 * request control - there is really no way to predict this as its
	 * Opportunistic !. ##### CURRENTLY RETURNS NOW + 24 hours ########
	 * 
	 * @return Time when this MCA will next want/be able to take control (millis
	 *         1970).
	 */
	@Override
	public long demandControlAt() {
		ObsDate obsDate = RCS_Controller.getObsDate(); // not used.
		long nowplus24 = System.currentTimeMillis() + 24 * 3600 * 1000L;
		return nowplus24;
	}

	/** Return true if wants control at time. */
	@Override
	public boolean wantsControl(long time) throws RemoteException {
		// System.err.println("TOCA: test WantsControl");
		Site site = RCS_Controller.controller.getObservatorySite();

		Position sun = Astrometry.getSolarPosition(time);

		double sunElev = sun.getAltitude(time, site);
		boolean sunup = (sunElev > 0.0);

		// System.err.println("TOCA: test WantsControl: sun is: "+(sunup ? "UP"
		// : "DOWN"));
		if (sunup)
			return false;

		return alertInProgress;

	}

	/** How long till this controller will definitely want control from time. */
	@Override
	public long nextWantsControl(long time) throws RemoteException {
		long tplus24 = time + 24 * 3600 * 1000L;
		return tplus24;
	}

	/**
	 * Returns the next available job. When the current SA is ready to start a
	 * new operation, it will send an appropriate command to the TOC_Server, the
	 * ProtocolImplementor will generate a RequestHandler which then creates a
	 * new Task to queue up for execution. For now we just allow a single job to
	 * be set via the TOC_ControlTask.setNextJob(Task) method to wrap a lower
	 * level generic task.
	 **/
	@Override
	public Task getNextJob() {
		if (jobs.isEmpty())
			return null;
		Task task = (Task) jobs.remove(0);
		return task;
	}

	/**
	 * Overridden to carry out subclass-specific abort processing prior to
	 * aborting subtasks. Calls clearCurrentService() to clear up after any
	 * service which is enabled.
	 */
	@Override
	public void onAborting() {
		super.onAborting();
		clearCurrentService();	
		// moved here 24feb2011 to avoid race condition
		alertInProgress = false;
	}

	/**
	 * Overridden to carry out subclass-specific processing on disposal.
	 * Unsubscribe all events.
	 */
	@Override
	public void onDisposal() {
		super.onDisposal();

	}

	/**
	 * Causes any executing subtasks (i.e anything currently being managed) to
	 * be sent Task.abort() - This method is intended to be called by the
	 * RequestHandler for the TOOP_RCS.ABORT command when ABORT ALL is received.
	 * Each task has its abortCode and Message set appropriately before abort()
	 * is called. ## METHOD NOT IMPLEMENTED YET ####
	 */
	public void abandonAll() {

	}

	/**
	 * Add a job to the queue. Jobs are taken from this in order - they do not
	 * run in parallel.
	 */
	public boolean addNextJob(Task task) {
		if (jobs.size() >= jobQueueSize)
			return false;

		if (task != null)
			jobs.add(task);

		return true;
	}

	/**
	 * Called before a new job is started (and every polling interval if not). A
	 * check for any newService is made, if there is one, currentService is
	 * re-assigned and the newService indicator is nulled (to prevent repeated
	 * enablement).
	 * 
	 */
	@Override
	protected void beforeStartJob() {
		// Always enable the TOC_Server even if no SA is active.
		TOC_Server.setAccept(true);

		if (newService == null)
			return;

		// setup the cSA.
		long now = System.currentTimeMillis();
		currentService = newService;
		currentService.initialize(pdf.format(new Date()));

		// We need to set a timer here to kill the service after
		// a period of .. currentService.sessionAllocation

		// Stopper stopper = new Stopper(newService, newServiceImpl);
		// stopper.start();

		// Respond to the waiting CommandImplementor.
		if (newServiceImpl != null) {
			taskLog.log(1, CLASS, name, "beforeStartJob", "New Session initialized: " + currentService.toString());

			newServiceImpl.processReply("OK sessionID=" + currentService.sessionId + ", sessionLimit="
					+ (currentService.sessionAllocation / 1000L) + ", timeRemaining="
					+ (currentService.timeRemaining / 1000L) + ", priority=" + currentService.priority);
		}

		// Loose these now or we get called every poll.
		newService = null;
		newServiceImpl = null;

	}

	/**
	 * EventSubscriber method. <br>
	 */
	@Override
	public void notifyEvent(String eventId, Object data) {

		super.notifyEvent(eventId, data);

		taskLog.log(1, CLASS, name, "notifyEvent", "** Event_Notification [" + eventId + "] + [" + data + "] **");

	}

	/**
	 * Returns true if the named service is authorized. i.e. it has an entry in
	 * the services table.
	 */
	public boolean isAuthorized(String serviceId) {
		return services.containsKey(serviceId);
	}

	/**
	 * Returns the ServiceDescriptor for the named service if available or null.
	 */
	public ServiceDescriptor getService(String serviceId) {
		if (services.containsKey(serviceId))
			return (ServiceDescriptor) services.get(serviceId);
		return null;
	}

	/** Returns the ServiceDescriptor for the currently in control SA or null. */
	public ServiceDescriptor getCurrentService() {
		return currentService;
	}

	/**
	 * Kills the current service (if any). Accounting for current service is
	 * performed. This method can be called when either a new service (of higher
	 * priority) wants control, the TOCA is aborted for any reason or the
	 * current service runs out of time (it will self fail in that circumstance
	 * via its timeConstraint field).
	 */
	protected void clearCurrentService() {
		
		// Clear the job queue - these have not yet been started so we can't use
		// abort
		// TOCControlTask provides the kill() method for this purpose.
		Iterator it = jobs.iterator();
		while (it.hasNext()) {
			TOOP_ControlTask task = (TOOP_ControlTask) it.next();
			task.kill();
		}

		// Do accounting for cSA.
		if (currentService != null) {
			long now = System.currentTimeMillis();
			long timeUsed = now - currentService.sessionStartTime;
			opsLog.log(1, CLASS, name + "." + currentService.id, "clearUp", "Control Session id:["
					+ currentService.sessionId + "]" + " for TOCS Service: " + currentService.id + ", Started: "
					+ sdf.format(new Date(currentService.sessionStartTime)) + ", Used time: " + timeUsed + " ms");

			// Write this info to a session log..
			try {
				File sessionLog = new File("toop/accounting/session-"
						+ qdf.format(new Date(currentService.sessionStartTime)));
				PrintStream pout = new PrintStream(new FileOutputStream(sessionLog));

				pout.println("Session       : " + currentService.sessionId);
				pout.println("Service       : " + currentService.id);
				pout.println("Start         : " + sdf.format(new Date(currentService.sessionStartTime)));
				pout.println("End           : " + sdf.format(new Date(now)));
				pout.println("Time Used (s) : " + (timeUsed / 1000));

				// write the files out - where do we get these from ?

				pout.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			currentService.clearUp();
			// Get rid of this or next HELO will fail.
			currentService = null;
		}

	}

	/** Clear up the current service and kill the currentTask if any. */
	public void killCurrentService() {
		clearCurrentService();
		// Abort the cSA's current (only) Task.
		if (currentTask != null) {
			((ParallelTaskImpl) currentTask).setAbortCode(OVERRIDDEN, "Overridden by higher priority service");
			currentTask.abort();
		}
		// Clear the alert flag..moved here 24feb2011 to avoid a race condition
		alertInProgress = false;
	}

	/**
	 * Nominates the named service if it has a valid services entry. When this
	 * method is called the current service (if any) will be killed off.
	 */
	public boolean replaceService(String serviceId, TOC_GenericCommandImpl impl) {
		ServiceDescriptor sd = getService(serviceId);
		if (sd == null)
			return false;
		// These are effectively flags to indicate that a new service wants
		// control.
		// Only when the current service dies off will we get control.

		// First, Stop anyone trying to connect - dont forget to re-enable as
		// soon as the TOCA yields again. It will do this soon if it still has
		// the alert flag set.
		TOC_Server.setAccept(false);

		newService = sd;
		newServiceImpl = impl;

		// Clearup current SA and do accounting.
		clearCurrentService();

		// Reset the alert flag to indicate ( to acceptControl() ) that an SA
		// wants control
		// since clearCurrentService() will just have cleared it.
		alertInProgress = true;

		// Abort the cSA's current (only) Task.
		if (currentTask != null) {
			taskLog.log(1, CLASS, name, "replaceService", "Aborting the CSA's currentTask: " + currentTask.getName());

			((ParallelTaskImpl) currentTask).setAbortCode(OVERRIDDEN, "Overridden by higher priority service");
			currentTask.abort();
		}

		// We now need to alert the TMM that the TOCA wants control immediately
		// and does not want to wait till the next MCA yield event.
		// Temporary fix - only call this if TOCA is not actually in control
		// otherwise
		// we end up potentially aborting it which is not neccessary and may
		// even
		// cause unexpected stuff to happen.
		//EventQueue.postEvent(RCS_ControlTask.TASK_YIELD_MESSAGE, "Yield-to: TOCS/" + serviceId);
		
		TaskOperations.getInstance()
			.getOperationsManager()
				.operationsModeYieldControl("TOCS/" + serviceId);
		
		return true;

	}

	/**
	 * EventSubscriber method.
	 */
	@Override
	public String getSubscriberId() {
		return name;
	}

	/**
	 * Holds description of a TO Service and its short-term accounting
	 * information.
	 */
	public static class ServiceDescriptor {

		/** Class id. */
		public final String CLASS = "TOControlAgent.ServiceDescriptor";

		/** Service ID. */
		public String id;

		/** Host where SA is running. */
		// public String controlHost;

		/** Execution priority. */
		public int priority;

		/**
		 * Time allocation (per 'accounting-period') in millis. This quantity is
		 * the total amount of time (spread over sessions) for the accounting
		 * period (e.g. per day). This needs definition - we use a VERY crude
		 * RCX invokation as the period for now.
		 */
		public long periodAllocation;

		/** Time allocation (per session) in millis. */
		public long sessionAllocation;

		/**
		 * Time remaining in current accounting-period. This quantity is
		 * deducted by the time actually used when a session ends (i.e. now -
		 * sessionStartTime ) .
		 */
		public long timeRemaining;

		/** Time the current control session started. */
		public long sessionStartTime;

		/** An ID for the current session (null when not in a control session). */
		public String sessionId;

		/** The ID of the default instrument - used for INIT. */
		public String defaultInstrument;

		/** TagID for first hour response accounting. */
		public String tagId;

		/** UserID for first hour response accounting. */
		public String userId;

		/** ProgramID for first hour response accounting. */
		public String progId;

		/** ProposalID for first hour response accounting. */
		public String proposalId;

		/** GroupID for first hour response accounting. */
		public String groupId;

		/** A thread to kill this SA if it overruns. */
		protected Stopper stopper;

		ServiceDescriptor(String id, long allocation, int priority) {
			this.id = id;
			this.sessionAllocation = allocation;
			// this.controlHost = controlHost;
			this.priority = priority;

			// Accounting.
			periodAllocation = 0L;
			timeRemaining = 0L;
			sessionStartTime = 0L;
			sessionId = null;
		}

		/** Initialize a session with the supplied sessionId. */
		public void initialize(String sessionId) {
			sessionStartTime = System.currentTimeMillis();
			this.sessionId = sessionId;
			// stopper = new Stopper(sessionAllocation);
			// stopper.start();
		}

		/** Clearup and do accounting for this session. */
		public void clearUp() {
			// Kill the Stopper thread before it does any harm.
			// if (stopper != null) stopper.terminate();
			long now = System.currentTimeMillis();
			long timeUsed = now - sessionStartTime;
			timeRemaining = Math.max(0L, timeRemaining - timeUsed);
			sessionStartTime = 0L;
			sessionId = null;
		}

		/** Returns a readable version of this ServiceDescriptor. */
		@Override
		public String toString() {
			return "[ServiceDescriptor:" + (sessionId != null ? "Session=" + sessionId : "") + ", Service ID=" + id
					+ ", Priority=" + priority + ", Session-alloc=" + (sessionAllocation / 60000L) + " mins."
					+ ", Period-alloc=" + (periodAllocation / 60000L) + " mins." + ", Time remaining="
					+ (timeRemaining / 1000L) + " secs." + "]";
		}

	}

	/** Thread to timeout after time and kill this SA. */
	class Stopper extends ControlThread {

		/** Timeout interval millis. */
		long time;

		ServiceDescriptor service;

		/** Create a Thread to timeout after time. */
		Stopper(ServiceDescriptor service, TOC_GenericCommandImpl serviceImpl) {
			super("STOPPER", false);
			this.service = service;
		}

		/** Initialization - does nothing. */
		@Override
		protected void initialise() {

		}

		/** Kills off the current service. */
		@Override
		public void mainTask() {
			try {
				sleep(service.sessionAllocation);
			} catch (InterruptedException ix) {
			}
			if (canRun()) {

				// Check this is the SA which created this Stopper.

				if (service == currentService && service != null)

					TOControlAgent.this.clearCurrentService();

			}
		}

		/** Clearup - does nothing. */
		@Override
		protected void shutdown() {
		};

	}

}

/**
 * $Log: TOControlAgent.java,v $ /** Revision 1.3 2007/08/03 08:48:38 snf /**
 * added extra TOFS options /** /** Revision 1.2 2007/08/03 08:18:40 snf /**
 * added TOFS server setup. /** /** Revision 1.1 2006/12/12 08:32:07 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:35:48 snf /** Initial
 * revision /** /** Revision 1.1 2002/09/16 09:38:28 snf /** Initial revision
 * /**
 */

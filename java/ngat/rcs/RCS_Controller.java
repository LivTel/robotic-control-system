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
package ngat.rcs;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.rmi.*;

import ngat.astrometry.*;
import ngat.icm.*;
import ngat.message.RCS_TCS.*;
import ngat.net.cil.CilService;
import ngat.net.cil.tcs.TcsStatusPacket;
import ngat.net.cil.test.CilServer;
import ngat.net.cil.test.DespatcherThread;
import ngat.net.cil.test.ReaderThread;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.emm.*;
import ngat.rcs.ers.test.BasicReactiveSystem;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.iss.ISS;
import ngat.rcs.iss.ISS_Server;
import ngat.rcs.newstatemodel.*;
import ngat.rcs.newstatemodel.test.BasicStateModelReactiveInput;
import ngat.rcs.ops.OperationsManager;
import ngat.rcs.sciops.ConfigTranslator;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.collation.InstrumentStatusProvider;
import ngat.rcs.scm.detection.*;
import ngat.rcs.telemetry.DefaultGroupOperationsMonitor;
import ngat.rcs.telemetry.InstrumentArchiveGateway;
import ngat.rcs.telemetry.OperationsArchiveGateway;
import ngat.rcs.telemetry.StateModelArchiveGateway;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.tocs.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.ems.*;
import ngat.ems.test.*;
import ngat.ims.*;
import ngat.tcm.*;
import ngat.tcm.test.*;

/**
 * @author dev
 */
public class RCS_Controller implements Logging {

	/** Major Version number - will be filled by ANT at build time. */
	public static final String MAJOR_VERSION = "@MAJOR-VERSION@";

	/** Minor Version number - will be filled by ANT at build time. */
	public static final String MINOR_VERSION = "@MINOR-VERSION@";

	/** Patch Version number - will be filled by ANT at build time. */
	public static final String PATCH_VERSION = "@PATCH-VERSION@";

	/** Build number - will be filled by ANT at build time. */
	public static final String BUILD_NUMBER = "@BUILD-NUMBER@";

	/** Release Name - will be filled by ANT at build time. */
	public static final String RELEASE_NAME = "@RELEASE-NAME@";

	/** Build Date - will be filled by ANT at build time. */
	public static final String BUILD_DATE = "@BUILD-DATE@";

	public static final String CLASS = "RCS_Controller";

	/** Polling interval for Parallel-Task queue polling. */
	public static final long DEFAULT_TASK_QUEUE_POLLING_INTERVAL = 2000L;

	/** Default interval at which the net state is tested for firing enablement. */
	public static final long DEFAULT_STATE_MODEL_UPDATE_INTERVAL = 1500L;

	/**
	 * Time to wait for weather clear confirmation whatever the weather is
	 * actually doing on startup (ms).
	 */
	public static final long DEFAULT_WEATHER_HOLD_TIME = 1200 * 1000L;

	/** Command to initiate Day-time operations mode. */
	public static final String START_DAY_OPS_COMMAND = "INTENTION_DAY_OPS";

	/** Command to initiate Night-time operations mode. */
	public static final String START_NIGHT_OPS_COMMAND = "INTENTION_NIGHT_OPS";

	/** Command to initiate Engineering mode. */
	public static final String INIT_MODE_ENG_MESSAGE = "INIT_MODE_ENG";

	/** Command to initiate Automatic mode. */
	public static final String INIT_MODE_AUTO_MESSAGE = "INIT_MODE_AUTO";

	/** Command to initiate weather hold. */
	public static final String WEATHER_HOLD_MESSAGE = "WEATHER_HOLD";

	/** Command to initiate weather release. */
	public static final String WEATHER_RELEASE_MESSAGE = "WEATHER_RELEASE";

	/** ERROR_BASE for this Task type. */
	public static final int ERROR_BASE = 5000;

	public static final double SITE_LATITUDE = 0;

	public static final double SITE_LONGITUDE = 0;

	// These exit codes are caught by the RCS startup script.
	// They are used to determine what to do when the process exits.
	//
	//

	/** Start index for failure exit codes. */
	public static final int EXIT_START = 605000;

	/** Failure exit code: Indicates . */
	public static final int LOAD_STANDARDS = 605001;

	/** Failure exit code: Indicates . */
	public static final int JCIL_INIT = 605002;

	/** Failure exit code: Indicates . */
	public static final int SERVER_BIND = 605003;

	/**
	 * Failure exit code: Indicates failure to configure calibration
	 * requirments.
	 */
	public static final int CALIB_INIT = 605004;

	/** Failure exit code: Indicates . */
	public static final int INSTRUMENTS_INIT = 605007;

	/** Failure exit code: Indicates . */
	public static final int STANDARDS_INIT = 605008;

	/** Failure exit code: Indicates . */
	public static final int PLANETARIUM_INIT = 605009;

	/** Failure exit code: Indicates . */
	public static final int BAD_TIMEZONE = 605010;

	/** Failure exit code: Indicates . */
	public static final int BAD_START_HOUR = 605011;

	/** Failure exit code: Indicates . */
	public static final int NO_CALLOUT = 605012;

	/** Failure exit code: Indicates . */
	public static final int CIL_HANDLERS_INIT = 605013;

	/** Failure exit code: Indicates . */
	public static final int SMM_INIT = 605014;

	/** Failure exit code: Indicates . */
	public static final int NO_LOGDIR = 605015;

	/** Failure exit code: Indicates . */
	public static final int NO_SITE = 605016;

	/** Failure exit code: Indicates . */
	public static final int STATE_MODEL_INIT = 605017;

	/** Failure exit code: Indicates . */
	public static final int TMM_CONFIG = 605018;

	/** Failure exit code: Indicates . */
	public static final int AGENTS_INIT = 605019;

	/** Failure exit code: Indicates . */
	public static final int TASK_INIT = 605019;

	/** Failure exit code: Indicates EMS startup problem . */
	public static final int EMS_CONFIG = 605020;

	/** 
	 * Failure exit code: Indicates IMS startup problem . 
	 */
	public static final int IMS_CONFIG = 605020;
	
	/** Failure exit code: Indicates GOM startup problem . */
	public static final int GOM_CONFIG = 605020;

	/** Signal entry code. Indicates - Normal operations. */
	// public static final int NOOP = 605020;

	/** Signal exit code. Indicates - Restart RCS (JVM) in Engineering mode. */
	public static final int RESTART_ENGINEERING = 605021;

	/** Signal exit code. Indicates - Restart RCS (JVM) in Robotic mode. */
	public static final int RESTART_ROBOTIC = 605022;

	/** Signal exit code. Indicates - REBOOT OCC system. */
	public static final int REBOOT = 605023;

	/** Signal exit code. Indicates - HALT RCS watchdog. */
	public static final int HALT = 605024;

	/** Signal exit code. Indicates - SHUTDOWN OCC system immediately. */
	public static final int SHUTDOWN = 605025;

	/** ERS Logging. */
	private Logger ersLog;

	/** ASTRO logging. */
	private Logger astroLog;

	/** BOOT Logging. */
	private Logger bootLog;

	/** TASK Logging. */
	private Logger taskLog;

	/** EMS Logging. */
	private Logger emsLog;

	/** OPS Logging. */
	private Logger opsLog;

	// protected LogServer logServer;

	/** Directory for logging files. */
	protected static File logDir;

	// ## We also have TASK, ERROR and OPERATIONS loggers available thro
	// ParallelTaskImpl ##
	// ## BUT NOT FOR RCS_Booter tho. ##.

	/** Generic DateFormat for messages. */
	public static SimpleDateFormat gdf = new SimpleDateFormat("yyyy/MM/dd HH:mm z");

	/** Standard ISO8601 DateFormat. */
	public static SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/** Day-in-year DateFormat. */
	public static SimpleDateFormat adf = new SimpleDateFormat("DDD");

	/** Day of week formatter. */
	public static SimpleDateFormat wdf = new SimpleDateFormat("EEE dd MMM 'at' HH:mm:ss z");

	/** Observing log formatter. */
	public static SimpleDateFormat odf = new SimpleDateFormat("yyyy_MM_dd");

	/** UTC TimeZone. */
	public static SimpleTimeZone UTC;

	/** Reference to Controller. */
	public static RCS_Controller controller;

	/** Reference to ControlAgent (CA). */
	// public static RCS_ControlTask controlAgent;

	/** ID of RCS. */
	public String rcsId;

	/** Site latitude. */
	public static double latitude;

	/** Site longitude. */
	public static double longitude;

	/** The site. */
	protected Site obsSite;

	protected ISite site;

	protected AstrometrySiteCalculator astro;

	protected BasicInstrumentRegistry ireg;

	protected BasicTelescope telescope;

	/** Dome low limit. */
	public static double domeLimit;

	/** Time this RCS instantiation was booted up. */
	public static long startupTime;

	/** Counts Groups executed - TEMP. */
	int groupCount = 0; // ???

	/** Current Observation Date. */
	protected static ObsDate obsDate;

	/** PUID for run count. */
	protected PersistentUniqueInteger run_puid;

	protected long runStartTime;

	/**
	 * Run-counter, counts number of times RCS has been started since time
	 * began.
	 */
	protected int runCounter;

	/** Used to store names of Threads running within the RCS. */
	public static Map threadRegistry;
	// AAAA

	/** RCI Command server - now rarely used in anger. */
	public static RCS_CommandServer cmdServer;

	/** TCS Status Monitor Controller. */
	public static SMM_Controller smmController;

	/** Instrument Status Monitors. */
	public static Map icsStatusMonitors;

	/** Tracking monitor. */
	public static DefaultTrackingMonitor defaultTrackingMonitor;

	/** Autoguider monitor. */
	public static DefaultAutoguiderMonitor defaultAutoguiderMonitor;

	/** Instrument monitor. */
	public static DefaultInstrumentMonitor defaultInstrumentMonitor;

	/** PMC Monitor. */
	public static ngat.rcs.scm.detection.PmcMonitor pmcMonitor;

	/** Booking model. */
	public static DefaultMutableAdvancedBookingModel bookingModel;

	/** Calibration requirements. */
	public static TelescopeCalibration tcal;

	/** Calibration history. */
	public static TelescopeCalibrationHistory tch;

	/** The DEFUNCT RCS State Model representation. */
	// protected ngat.rcs.statemodel.StateModel stateModel;

	protected static ngat.rcs.newstatemodel.StandardStateModel tsm;

	/** Interval for statemodel to update (millis). */
	protected long stateModelUpdateInterval;

	/** TMM Configuration file. */
	private static File tmmAgentConfigFile;

	/** TMM Configuration file. */
	private static File tmmScheduleConfigFile;

	/** CA History file. */
	private static File historyFile;

	/** Status Multicaster. */
	// public static MulticastObjectRelay statusRelay;

	/** Application Configuration settings. */
	protected static ConfigurationProperties rcs_config;

	/** Site Configuration settings. */
	protected static ConfigurationProperties site_config;

	/** ID of the telescope in use. */
	protected String telescopeId;

	/** Description of the telescope in use. */
	protected String telescopeDesc;

	/** Description of the telescope location. */
	protected String telescopeLocation;

	/** A skymodel for updating. */
	protected SkyModel skyModel;

	protected BasicMeteorologyProvider meteo;

	/**
	 * Instance that reads disk status (percentage used/free space)
	 * from a file written by a cronjob.
	 */
	protected BasicDiskStatusProvider diskStatusProvider;
	
	/** A calibration monitor. */
	protected BasicCalibrationMonitor bcalMonitor;

	/** Calibration logger. */
	protected BasicCalibrationUpdateListener bcalLog;

	/** Group operations monitor. */
	protected DefaultGroupOperationsMonitor gom;

	/** Operations manager. */
	private OperationsManager opsManager;

	/** Task monitor. */
	protected BasicTaskMonitor taskMonitor;

	/**
	 * Used to indicate that the RCS is running in a day or night operational
	 * mode with no inhibitors - i.e. that it can observe.
	 */
	protected volatile boolean operational;

	protected boolean automatic;

	/**
	 * Starts up the RCS Controller. The command line argument -help can be used
	 * to just print out the list of arguments available. The RCS_Controller is
	 * started up using the supplied arguments and loads the specified config
	 * files. If an error occurs during startup an exception is thrown and Main
	 * exits with an error dependant exit status. This can be used by a startup
	 * script or an AutoBooter to determine what to do next.
	 */
	public static void main(String args[]) {

		// Setup global time formatting.

		UTC = new SimpleTimeZone(0, "UTC");

		TimeZone.setDefault(UTC);

		gdf.setTimeZone(UTC);
		adf.setTimeZone(UTC);
		wdf.setTimeZone(UTC);
		odf.setTimeZone(UTC);
		iso8601.setTimeZone(UTC);

		startupTime = System.currentTimeMillis();

		CommandParser parser = new CommandParser();

		try {
			parser.parse(args);
		} catch (ParseException px) {
			usage();
			System.exit(1);
			return;
		}

		ConfigurationProperties map = parser.getMap();

		// Help only.
		String help = map.getProperty("help", "false");
		if (help.equals("true")) {
			usage();
			System.exit(0);
			return;
		}

		System.err.println("Starting RCS " + MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION + " (" + RELEASE_NAME
				+ ") Build: " + BUILD_NUMBER + " " + BUILD_DATE);

		// Load extra system properties:
		// These are used as temporary fill-ins or for configs which don't fit
		// in
		// anywhere very obvious at the moment.

		// Any integrated modules should have their own configuration files.

		try {
			Properties sysextra = new Properties();
			sysextra.load(new FileInputStream("config/system.properties"));
			Enumeration e = sysextra.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String val = sysextra.getProperty(key);
				System.setProperty(key, val);
			}
			// Simpler ? alternative
			// Properties p = System.getProperties();
			// p.putAll(sysextra);
		} catch (Exception ex) {
			System.err.println("WARNING - RCS_Startup: unable to load extra system properties: " + ex);
		}

		// Config files.
		File configFile = new File("rcs.properties");

		// Task Startup.
		boolean engineering = false;
		String eng = map.getProperty("engineering", "false");
		if (eng.equals("true"))
			engineering = true;

		// RCS-ID.
		String rcsId = map.getProperty("id", "RCS");

		// ------------------------------
		// Setup Legacy Comms parameters.
		// ------------------------------

		JMSMA_TaskImpl.setConnectionFactory(RCS_SubsystemConnectionFactory.getInstance());

		ParallelTaskImpl.setConnectionFactory(RCS_SubsystemConnectionFactory.getInstance());

		TCSStatusClient.setConnectionFactory(RCS_SubsystemConnectionFactory.getInstance());
		// InstrumentStatusClient.setConnectionFactory(RCS_SubsystemConnectionFactory.getInstance());

		// Create the Registry first the CA will need to register with it.
		EventRegistry.getInstance();

		// Try to create the Controller.
		try {
			controller = new RCS_Controller(configFile, rcsId, !engineering);
		} catch (RCSStartupException rsx) {
			rsx.printStackTrace();
			System.err.println("RCS_Startup - Aborted startup on Creation of RCS Controller: " + rsx);
			System.exit(rsx.getExitCode() - EXIT_START);
		}

		// Create the CA - It must be up and ready to accept alerts before the
		// monitors are generating them.
		// RCS_ControlTask.initialize("CONTROL_AGENT", null);
		// controlAgent = RCS_ControlTask.getInstance();

		// Configure TMM: Setup Mode Control Agents.
		try {
			TaskOperations.getInstance().configureAgents(tmmAgentConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
			// We should send a FATAL error message to the OPERATORs here.
			System.err.println("RCS_Startup - Aborted startup on Setup Mode Control Agents: " + e);
			System.exit(TMM_CONFIG - EXIT_START);
		}

		// Insert opsmgr binding
		try {
			OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();
			tsm.addControlActionImplementor(opsMgr);
			// MAYBE EventRegistry.subscribe(RCS_ControlTask.TASK_YIELD_MESSAGE,
			// opsMgr);

			// TODO #TNG# OperationsArchiveGateway oag = new
			// OperationsArchiveGateway(opsMgr);
			// Naming.rebind("OperationsGateway", oag);
			// oag.startProcessor();
		} catch (Exception e) {
			e.printStackTrace();
			// We should send a FATAL error message to the OPERATORs here.
			System.err.println("RCS_Startup - Aborted startup on binding OpsMgr: " + e);
			System.exit(TMM_CONFIG - EXIT_START);
		}

		// Link GOM to OpsMgr ...
		/*
		 * try { OperationsManager opsMgr =
		 * TaskOperations.getInstance().getOperationsManager();
		 * opsMgr.addOperationsEventListener
		 * (controller.getGroupOperationsMonitor()); } catch (Exception e) {
		 * e.printStackTrace(); // We should send a FATAL error message to the
		 * OPERATORs here. System.err.println(
		 * "RCS_Startup - Aborted startup on binding OpsMgr with GOM: " + e);
		 * System.exit(TMM_CONFIG - EXIT_START); }
		 */

		// THIS IS UNUSUAL - ITS the only gateway thats initialized outside the
		// contructor -

		try {
			OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();
			DefaultGroupOperationsMonitor gom = controller.getGroupOperationsMonitor();
			OperationsArchiveGateway oag = new OperationsArchiveGateway(opsMgr, gom);

			Naming.rebind("OperationsGateway", oag);
			oag.setProcessInterval(10000L);
			oag.setBackingStoreAgeLimit(10 * 60 * 1000L);
			oag.startProcessor();
		} catch (Exception iax) {
			iax.printStackTrace();
		}

		// register TSM to receive instability triggers
		// defaultTrackingMonitor.addTrackingStatusListener(tsm);
		// defaultAutoguiderMonitor.addGuideStatusListener(tsm);
		pmcMonitor.addPmcStatusListener(tsm);

		// and let the tsm know about these so it can switch em off and on
		// tsm.setTrackingMonitor(defaultTrackingMonitor);
		// tsm.setAutoguiderMonitor(defaultAutoguiderMonitor);
		tsm.setPmcMonitor(pmcMonitor);

		// ---------------------------
		// Setup FITS header settings.
		// ---------------------------
		try {
			File file = rcs_config.getFile("FITS.header.config.file", "FITS.config");
			FITS_HeaderInfo.configure(file);
			System.out.println("Configured FITS_Headers from file: " + file.getPath());
			// bootLog.log(1, CLASS, rcsId, "init",
			// "Configured FITS_Headers from file: "+file.getPath());
		} catch (FileNotFoundException fnfx) {
			FITS_HeaderInfo.defaultSetup();
			// bootLog.log(1, CLASS, rcsId, "init",
			// "Unable to load FITS_Headers config - used defaults.");
			System.out.println("Configuring FITS_Headers FAILED: " + fnfx);
		}

		// controlAgent.init();

		// Start the Status Monitors now.
		// SMM_Controller.setLogger("STATUS");
		// smmController.startAll();

		// ##################
		// invokeTestStatus();
		// ##################

		// Wait for the Status Monitors to generate some info.
		long initDelay = rcs_config.getLongValue("init.delay", 15000L);
		try {
			Thread.sleep(initDelay);
		} catch (InterruptedException e) {
		}

		// Start the Monitor thread.
		// Monitors.startMonitoring();

		// #########
		// MonitorsXX.startMonitoring();
		// #########

		// Wait till now to Start Servers.
		// POS_Server.launch();
		// bootLog.log(1, CLASS, rcsId, "init", "POS_Server started:");

		Ctrl_Server.launch();
		// bootLog.log(1, CLASS, rcsId, "init",
		// "Control (CAMP) server started:");

		ISS.launch();
		// bootLog.log(1, CLASS, rcsId, "init", "ISS and server started:");

		// Setup the various MCAs here via TMM_TaskSequencer.

		// Launch the ControlAgent.
		// new TaskWorker(((Task) controlAgent).getName(),
		// controlAgent).beginJob();

		// Send this as a default weather hold in the absence of other weather
		// triggers..
		EventQueue.postEvent(WEATHER_HOLD_MESSAGE);

		long now = System.currentTimeMillis();

		long weatherHoldTime = rcs_config.getLongValue("weather.hold.time", DEFAULT_WEATHER_HOLD_TIME);

		// Send this after a while.
		EventQueue.postTimedEvent(WEATHER_RELEASE_MESSAGE, null, now + weatherHoldTime, 0L, EventQueue.DEFAULT_LEVEL);

		// NOTE These are to fix an antibug introduced by fixing the AXES_ALERT
		// problem
		EventQueue.postEvent("AZIMUTH_ERROR");
		EventQueue.postEvent("ALTITUDE_ERROR");
		EventQueue.postEvent("ROTATOR_ERROR");

		// Wait for it to settle down then send the correct signal.
		long runupDelay = rcs_config.getLongValue("runup.delay", 150000L);
		try {
			Thread.sleep(runupDelay);
		} catch (InterruptedException e) {
		}

		// Start the state model loop. (this is a bit naff but has to be thus).
		// If only we had - SM.setUpdateInterval(long) - perhaps one day !
		// / TEMP 6-oct-08 snf
		// controller.getStateModel().start(controller.getStateModelUpdateInterval());
		// TAKE THIS OUT ASAP UNLESS SWITCHING TO NSM

		now = System.currentTimeMillis();

		// Work out desired state.
		// In ENG mode we remain in ENG until mode switch to AUTO
		if (engineering) {
			EventQueue.postEvent(INIT_MODE_ENG_MESSAGE);
			try {
				tsm.environmentChanged(new EnvironmentChangeEvent(EnvironmentChangeEvent.INTENT_ENGINEERING));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			EventQueue.postEvent(INIT_MODE_AUTO_MESSAGE);
			try {
				tsm.environmentChanged(new EnvironmentChangeEvent(EnvironmentChangeEvent.INTENT_OPERATIONAL));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Time offsets for pre-night startup and pre-day stopping.
		long opStartTimeOffset = rcs_config.getLongValue("operational.start.time.offset", 30 * 60 * 1000L);
		long opStopTimeOffset = rcs_config.getLongValue("operational.stop.time.offset", 7 * 60 * 1000L);

		// Work out current sunstate, next rise/set and time until.
		Position sun = Astrometry.getSolarPosition(now);

		// TODO: How the bejabbers do we signal day and night in the future
		// .....

		long sunset = RCS_Controller.obsDate.getSunset();
		if (sunset < now)
			sunset += 24 * 3600 * 1000L;
		long ttsunset = sunset - now;
		long sunrise = RCS_Controller.obsDate.getSunrise();
		if (sunrise < now)
			sunrise += 24 * 3600 * 1000L;
		long ttsunrise = sunrise - now;

		System.err.println("Event calculations:" + "      Next Sunset:  " + wdf.format(new Date(sunset)) + " ("
				+ (ttsunset / 1000) + " secs)" + "      Next Sunrise: " + wdf.format(new Date(sunrise)) + " ("
				+ (ttsunrise / 1000) + " secs)");

		if (sun.isRisen()) {
			System.err.println("Current Period: Daytime");
			// It is DAY
			EventQueue.postEvent("SUNRISE");

			if (ttsunset < opStartTimeOffset) {
				System.err.println("    Current Intent: NightOps");
				EventQueue.postEvent(START_NIGHT_OPS_COMMAND);
			} else {
				System.err.println("    Current Intent: DayOps");
				EventQueue.postEvent(START_DAY_OPS_COMMAND);
				System.err.println(" Starting NightOps: " + wdf.format(new Date(sunset - opStartTimeOffset)) + " ("
						+ ((sunset - opStartTimeOffset) / 1000) + " secs)");
				EventQueue
						.postTimedEvent(START_NIGHT_OPS_COMMAND, null, sunset - opStartTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			}

			// Push the time of NEXT sunset, next sunrise, which may be next OD
			// and next SODOPS.

			EventQueue.postTimedEvent("SUNSET", null, sunset - opStartTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			EventQueue.postTimedEvent(START_DAY_OPS_COMMAND, null, sunrise - opStopTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			EventQueue.postTimedEvent("SUNRISE", null, sunrise - opStopTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);

		} else {
			System.err.println("Current Period: Nighttime");
			// It is NIGHT
			EventQueue.postEvent("SUNSET");

			if (ttsunrise < opStopTimeOffset) {
				System.err.println("    Current Intent: DayOps");
				EventQueue.postEvent(START_DAY_OPS_COMMAND);

			} else {
				System.err.println("    Current Intent: NightOps");
				EventQueue.postEvent(START_NIGHT_OPS_COMMAND);
				System.err.println("   Starting DayOps: " + wdf.format(new Date(sunrise - opStopTimeOffset)) + " ("
						+ ((sunrise - opStopTimeOffset) / 1000) + " secs)");
				EventQueue.postTimedEvent(START_DAY_OPS_COMMAND, null, sunrise - opStopTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			}

			// Push the time of NEXT sunrise, next sunset, which may be next OD
			// and next SONOPS.
			EventQueue.postTimedEvent("SUNRISE", null, sunrise - opStopTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			EventQueue.postTimedEvent(START_NIGHT_OPS_COMMAND, null, sunset - opStartTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);
			EventQueue.postTimedEvent("SUNSET", null, sunset - opStartTimeOffset, 0L, EventQueue.DEFAULT_LEVEL);

		}

		// Either way we need to force a restart either at next available
		// startOfDay + rebootMins.
		int rebootMins = rcs_config.getIntValue("reboot.minutes", 30);
		long sod = RCS_Controller.obsDate.getStartOfDay();
		long rebootTime = sod + (rebootMins * 60 * 1000L);
		if (now > rebootTime)
			rebootTime += ObsDate.ONE_DAY;
		// Give it 60 Mins slop

		String rebootCommand = rcs_config.getProperty("reboot.command", "REBOOT_REBOOT");

		EventQueue.postTimedEvent(rebootCommand, "", rebootTime, rebootTime + 60 * 60 * 1000L, EventQueue.DEFAULT_LEVEL);

		// ##### OP AUTO_REBOOT gives the RCS and ICS options.

		// Enable telemetry.
		Telemetry telemetry = Telemetry.getInstance();
		// telemetry.startDespatcher();

		// ### BasicWeatherMonitor - a real architectural gem ! (fudge)
		try {
			BasicWeatherMonitoring bwm = new BasicWeatherMonitoring();
			bwm.setGoodState("CLEAR");
			bwm.setBadState("ALERT");
			bwm.setKey("C:THREAT");
			Naming.rebind("rmi://localhost:1099/WeatherMonitoring", bwm);
			System.err.println("Bound BasicWeatherMonitoring to registry");

			(new Thread(bwm)).start();

			controller.registerControlThread("BW_MONITOR", bwm);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error binding: BasicWeatherMonitoring to registry");
		}

		// keep this bally thread alive ...
		while (true) {
			try {
				Thread.sleep(10000L);
				System.err.print("*");
			} catch (InterruptedException ix) {
			}
		}

	}

	public static void usage() {
		System.err.println("java ngat.rcs.RCS_Controller [options]" + "\n Where options includes:-"
				+ "\n   -config:      The name of the configuration file."
				+ "\n   -id:          A name/id for the RCS for logging."
				+ "\n   -engineering: Startup and remain in INIT state.");
	}

	public RCS_Controller(File configFile, String rcsId, boolean automatic) throws RCSStartupException {

		this.rcsId = rcsId;
		this.automatic = automatic;

		// Generate the Run-counter.
		run_puid = new PersistentUniqueInteger("%%run");

		runStartTime = System.currentTimeMillis();

		try {
			runCounter = run_puid.get();
		} catch (Exception e) {
			System.err.println("Error reading Run-counter: " + e);
			runCounter = 0;
		}
		try {
			runCounter = run_puid.increment();
		} catch (Exception e) {
			runCounter++;
		}

		// Setup thread register.
		threadRegistry = Collections.synchronizedMap(new HashMap());

		// --------------
		// Basic logging.
		// --------------

		LogFormatter slf = new BasicLogFormatter(150);

		// Primary console handler.
		LogHandler console = new ConsoleLogHandler(slf);
		console.setLogLevel(5);
		console.setName("SYS_CONSOLE:1");
		LogManager.registerHandler(console);

		// Secondary console handler.
		LogHandler console2 = new ConsoleLogHandler(slf);
		console2.setLogLevel(5);
		console2.setName("SYS_CONSOLE:2");
		LogManager.registerHandler(console2);

		// Boot log.
		bootLog = LogManager.getLogger("BOOT");
		bootLog.addHandler(console);
		bootLog.setLogLevel(ALL);

		// Log Directory.
		//
		// -First look for a System property: [log]
		// If exists -> use it.
		// Else try to create it.
		// If Created -> use it.
		// Else [user.home]/logs.
		// -No [log] property.
		// Try [user.home]/logs.
		// If exists use it.
		// Else try to create it.
		// If created -> use it.
		// Else STUFFED.
		//

		boolean builtLogDir = false;
		logDir = null;
		String logDirName = System.getProperty("log");
		if (logDirName == null) {
			bootLog.log(1, CLASS, rcsId, "init", "No logging directory specified: - trying default.");
		} else {
			logDir = new File(logDirName);
			if (logDir.exists()) {
				if (logDir.isDirectory() && logDir.canWrite()) {
					builtLogDir = true;
				} else {
					bootLog.log(1, CLASS, rcsId, "init", "Unable to use logging directory: " + logDir.getPath()
							+ " - trying default.");
				}
			} else {
				bootLog.log(1, CLASS, rcsId, "init", "Log directory not found trying to create:");
				if (logDir.mkdirs()) {
					bootLog.log(1, CLASS, rcsId, "init", "Created logging directory: " + logDir.getPath());
					builtLogDir = true;
				} else {
					bootLog.log(1, CLASS, rcsId, "init", "Unable to create logging directory: " + logDir.getPath()
							+ " - trying default.");
				}
			}
		}
		// We either have a useable logdir here or we are going for the default.
		if (!builtLogDir) {
			logDir = new File(System.getProperty("user.home"), "logs");
			if (logDir.exists()) {
				if (logDir.isDirectory() && logDir.canWrite()) {
					bootLog.log(1, CLASS, rcsId, "init", "Created logging directory: " + logDir.getPath());
					builtLogDir = true;
				} else {
					throw new RCSStartupException("Unable to write to logging directory: ", NO_LOGDIR);
				}
			} else {
				bootLog.log(1, CLASS, rcsId, "init", "Log directory not found trying to create:");
				if (logDir.mkdirs()) {
					bootLog.log(1, CLASS, rcsId, "init", "Created logging directory: " + logDir.getPath());
					builtLogDir = true;
				} else {
					throw new RCSStartupException("Unable to create logging directory: ", NO_LOGDIR);
				}
			}
		}

		// We should have a useable logdir now.
		// ----------------------------
		// Load configuration settings.
		// ----------------------------

		rcs_config = new ConfigurationProperties();
		try {
			rcs_config.load(new FileInputStream(configFile));
			bootLog.log(1, CLASS, rcsId, "init", "Configured RCS_Controller (Application) from file: " + configFile.getPath());
		} catch (IOException e) {
			defaultSetup();
			bootLog.log(1, CLASS, rcsId, "init", "Failed to load rcs_config settings - using defaults.  Tried file: "
					+ configFile.getPath());
		}

		File siteConfigFile = new File("config/site.properties");
		site_config = new ConfigurationProperties();
		try {
			site_config.load(new FileInputStream(siteConfigFile));
			bootLog.log(1, CLASS, rcsId, "init", "Configured RCS_Controller (Site) from file: " + siteConfigFile.getPath());
		} catch (IOException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Failed to load site_config settings from: " + siteConfigFile.getPath());
			throw new RCSStartupException("Unable to load Site config: ", NO_SITE);
		}

		// BOOT LOG
		try {
			File file = new File(logDir, "rcs_boot_" + runCounter);
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), 1000, 1, 20);
			fh.setLogLevel(ALL);
			fh.setName("BOOT_TEXT");
			LogManager.registerHandler(fh);
			bootLog.addHandler(fh);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating Boot_log text filehandler: " + e);
		}

		// EVENT LOG
		Logger eventLog = LogManager.getLogger("EVENT");
		eventLog.addHandler(console);

		eventLog.setLogLevel(ALL);
		try {
			File file = new File(logDir, "rcs_event");
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.DAILY_ROTATION);
			fh.setLogLevel(ALL);
			fh.setName("EVENT_TEXT");
			LogManager.registerHandler(fh);
			eventLog.addHandler(fh);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating Event_log text filehandler: " + e);
		}

		// ISS LOG
		Logger issLog = LogManager.getLogger("ISS");
		issLog.setLogLevel(ALL);
		try {
			File file = new File(logDir, "rcs_iss");
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.DAILY_ROTATION);
			fh.setLogLevel(ALL);
			fh.setName("ISS_TEXT");
			LogManager.registerHandler(fh);
			issLog.addHandler(fh);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating ISS_log text filehandler: " + e);
		}

		// TCM LOG
		Logger tcmLog = LogManager.getLogger("TCM");
		tcmLog.setLogLevel(ALL);
		try {
			File file = new File(logDir, "rcs_tcm");
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.HOURLY_ROTATION);
			fh.setLogLevel(ALL);
			fh.setName("TCM_TEXT");
			LogManager.registerHandler(fh);
			tcmLog.addHandler(fh);
			tcmLog.addExtendedHandler((ExtendedLogHandler) console2);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating TCM_log text filehandler: " + e);
		}

		// ICM LOG
		Logger icmLog = LogManager.getLogger("ICM");
		icmLog.setLogLevel(ALL);
		try {
			File file = new File(logDir, "rcs_icm");
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.HOURLY_ROTATION);
			fh.setLogLevel(ALL);
			fh.setName("ICM_TEXT");
			LogManager.registerHandler(fh);
			icmLog.addHandler(fh);
			icmLog.addExtendedHandler((ExtendedLogHandler) console2);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating ICM_log text filehandler: " + e);
		}

		// CIL LOG
		Logger cilLog = LogManager.getLogger("CIL");
		cilLog.setLogLevel(ALL);
		try {
			File file = new File(logDir, "rcs_cil");
			FileLogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.DAILY_ROTATION);

			// NOTE extended file handler will NOT log using ALL must be a
			// NUMBER
			fh.setLogLevel(5);
			fh.setName("CIL_TEXT");
			LogManager.registerHandler(fh);
			cilLog.addExtendedHandler(fh);
			// cilLog.addExtendedHandler((ExtendedLogHandler) console3);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating Cil_log filehandler: " + e);
		}

		// -------------
		// Startup mode.
		// -------------
		bootLog.log(1, CLASS, rcsId, "init", "Starting bootup in " + (automatic ? "AUTOMATIC" : "ENGINEERING") + " Mode");

		// -----------------------
		// Set up routine logging.
		// -----------------------
		taskLog = LogManager.getLogger("TASK");
		taskLog.setLogLevel(ALL);
		taskLog.addHandler(console);
		taskLog.addExtendedHandler((ExtendedLogHandler) console2);
		taskLog.setChannelID("RCS_TASK");
		taskLog.log(1, "LAUNCHER", "RCSController", "Initialized TASK log");

		astroLog = LogManager.getLogger("ASTRO");
		astroLog.setLogLevel(5);
		astroLog.addHandler(console);
		astroLog.addExtendedHandler((ExtendedLogHandler) console);
		astroLog.log(1, "LAUNCHER", "RCSController", "Initialized ASTRO log");

		// EMS LOG
		try {
			File file = new File(logDir, "rcs_ems");
			FileLogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.HOURLY_ROTATION);
			fh.setLogLevel(3);
			fh.setName("EMS_TXT");
			LogManager.registerHandler(fh);
			emsLog = LogManager.getLogger("EMS");
			emsLog.setLogLevel(3);
			emsLog.addExtendedHandler(fh);
			emsLog.log(1, "LAUNCHER", "RCSController", "Initialized EMS log");
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating filehandler [EMS_TXT] : " + e);
		}

		// ERS LOG
		try {
			File file = new File(logDir, "rcs_ers");
			FileLogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.HOURLY_ROTATION);
			fh.setLogLevel(3);
			fh.setName("ERS_TXT");
			LogManager.registerHandler(fh);
			ersLog = LogManager.getLogger("ERS");
			ersLog.setLogLevel(3);
			ersLog.addExtendedHandler(fh);
			ersLog.log(1, "LAUNCHER", "RCSController", "Initialized ERS log");
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating filehandler [ERS_TXT] : " + e);
		}

		// OPS LOG
		try {
			File file = new File(logDir, "rcs_ops");
			FileLogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(150), FileLogHandler.HOURLY_ROTATION);
			fh.setLogLevel(3);
			fh.setName("OPS_TXT");
			LogManager.registerHandler(fh);
			opsLog = LogManager.getLogger("OPS");
			opsLog.setLogLevel(3);
			opsLog.addExtendedHandler(fh);
			opsLog.log(1, "LAUNCHER", "RCSController", "Initialized OPS log");
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating filehandler [OPS_TXT] : " + e);
		}

		// JMS LOG
		Logger jms = LogManager.getLogger("JMS");
		jms.setLogLevel(0);
		jms.addHandler(console);
		jms.log(1, "LAUNCHER", "RCSController", "Initialized JMS log");

		// TASK LOG
		try {
			File file = new File(logDir, "rcs_task");
			LogHandler fh = new FileLogHandler(file.getPath(), new BasicLogFormatter(250), FileLogHandler.DAILY_ROTATION);
			fh.setLogLevel(ALL);
			fh.setName("TASK_TXT");
			LogManager.registerHandler(fh);
			taskLog.addHandler(fh);
		} catch (FileNotFoundException e) {
			bootLog.log(1, CLASS, rcsId, "init", "Error creating filehandler [TASK_TXT] : " + e);
		}

		// Register a Shutdown hook for unexpected termination.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				emergencyShutdown();
			}
		});

		// ----------
		// Telescope.
		// ----------
		telescopeId = site_config.getProperty("id", "???");
		telescopeDesc = site_config.getProperty("description", "???");
		telescopeLocation = site_config.getProperty("site", "???");

		// -------------------------
		// Initialize EventRegistry.
		// -------------------------
		EventRegistry.getInstance();
		bootLog.log(1, CLASS, rcsId, "init", "EventRegistry initialized:");

		// ----------------------
		// Initialize EventQueue.
		// ----------------------
		EventQueue.initialize();
		bootLog.log(1, CLASS, rcsId, "init", "EventQueue initialized:");

		// --------------------------
		// Global Position viewpoint.
		// --------------------------
		latitude = Math.toRadians(site_config.getDoubleValue("latitude", SITE_LATITUDE));
		longitude = Math.toRadians(site_config.getDoubleValue("longitude", SITE_LONGITUDE));
		obsSite = new Site(telescopeLocation, latitude, longitude);

		Position.setViewpoint(latitude, longitude);
		bootLog.log(1, CLASS, rcsId, "init",
				"Set Site location: Long: " + Position.toDMSString(longitude) + " Lat: " + Position.toDMSString(latitude));

		// These 2 properties can be got from (.site) as well.

		int tzoffset = 0;
		// Read the TIMEZONE Offset fed in as System property.
		try {
			// tzoffset = Integer.parseInt(System.getProperty("TZ_OFFSET"));

			tzoffset = site_config.getIntValue("tzoffset");

		} catch (Exception nx) {
			throw new RCSStartupException("TimeZone not defined or badly formatted: " + System.getProperty("TZ_OFFSET"),
					BAD_TIMEZONE);
		}

		int sodh = 0;
		// Read the Start-of-day-hours fed in as System property.
		try {
			// sodh = Integer.parseInt(System.getProperty("OD_START_HOUR"));

			sodh = site_config.getIntValue("sodh");

		} catch (Exception nx) {
			throw new RCSStartupException("Start-of-day-hour not defined or badly formatted: "
					+ System.getProperty("OD_START_HOUR"), BAD_START_HOUR);
		}

		obsDate = new ObsDate(tzoffset, sodh);

		bootLog.log(1, CLASS, rcsId, "init", "RCS Bootup:" + obsDate.toString());

		// new astro
		site = new BasicSite(telescopeLocation, latitude, longitude);
		astro = new BasicAstrometrySiteCalculator(site);

		// ----------------------------
		// Initialize Status mechanism.
		// ----------------------------
		int tcspoolSize = rcs_config.getIntValue("tcs.status.pool.size", 20);
		StatusPool.initialize(tcspoolSize);
		TCS_Status.mapCodes();
		TcsStatusPacket.mapCodes();

		// block###
		bootLog.log(1, CLASS, rcsId, "init", "TCS_Status_Pool was initialized at size: " + StatusPool.getSize());

		// SMM_Controller.setLogger("BOOT");
		// smmController = SMM_Controller.getController();
		// EventRegistry.subscribe("NETWORK_COMMS_ALERT", smmController);
		// EventRegistry.subscribe("NETWORK_COMMS_CLEAR", smmController);

		// ### REMOVE THIS BLOCK WHEN SCM-X IS SWITCHED IN ######
		/*
		 * try { File file = rcs_config.getFile("smm.config.file",
		 * "config/status_monitor.properties"); smmController.configure(file);
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Configured SMM_Controller from file: " + file.getPath()); } catch
		 * (IOException iox) { bootLog.log(1, CLASS, rcsId, "init",
		 * "Unable to setup SMM_Controller: " + iox); throw new
		 * RCSStartupException("Unable to setup SMM_Controller: " + iox,
		 * SMM_INIT); } catch (IllegalArgumentException iax) { bootLog.log(1,
		 * CLASS, rcsId, "init", "Unable to setup SMM_Controller: " + iax);
		 * throw new RCSStartupException("Unable to setup SMM_Controller: " +
		 * iax, SMM_INIT); }
		 */

		// Task Manager Configuration
		long pollingInterval = rcs_config.getLongValue("task.queue.polling.interval", DEFAULT_TASK_QUEUE_POLLING_INTERVAL);
		ParallelTaskImpl.setPollingInterval(pollingInterval);

		// Recovery. THIS IS STILL USED BUT NOT A LOT
		try {
			File file = rcs_config.getFile("task.recovery.config.file", "config/recovery.properties");
			TaskRecoveryRegistry registry = new TaskRecoveryRegistry();
			registry.configure(file);
			ParallelTaskImpl.setRecoveryRegistry(registry);
			bootLog.log(1, CLASS, rcsId, "init", "Configured Task Recovery Registry from file: " + file.getPath());
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error configuring Task Recovery Registry: ", null, e);
			throw new RCSStartupException("Error initializing Task Recovery Registry: " + e, TASK_INIT);
		}

		// Task Config. THIS IS STILL USED OFTEN
		try {
			File file = rcs_config.getFile("task.central.config.file", "config/task.properties");
			TaskConfigRegistry registry = new TaskConfigRegistry();
			registry.configure(file);
			ParallelTaskImpl.setTaskConfigRegistry(registry);
			JMSMA_TaskImpl.setTaskConfigRegistry(registry);
			bootLog.log(1, CLASS, rcsId, "init", "Configured Task Config Registry from file: " + file.getPath());
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error configuring Task Recovery Registry: ", null, e);
			throw new RCSStartupException("Error initializing Task Config Registry: " + e, TASK_INIT);
		}

		// --------------------
		// Setup the CIL layer.
		// --------------------
		String cilServerId = rcs_config.getProperty("cil.server", "CIL_PROXY");
		String cilHost = rcs_config.getProperty("cil.host", "ltccd1");
		int cilDestPort = rcs_config.getIntValue("cil.dest.port", 13021);
		int cilSendPort = rcs_config.getIntValue("cil.send.port", 13022);
		int cilServicePort = rcs_config.getIntValue("cil.service.port", 5599);
		int cilServiceClass = rcs_config.getIntValue("cil.service.class", 589824);
		int cilTxId = 18;
		int cilRxId = 17;
		String cilServiceName = rcs_config.getProperty("cil.service.name", "TCSCilService");
		/*
		 * try { JCIL.setup(cilHost, cilSendPort, cilDestPort); bootLog.log(1,
		 * CLASS, rcsId, "init", "JCIL (UDP) is set up: ServerID: " +
		 * cilServerId + " LocalPort: " + cilSendPort + " RemoteHost: " +
		 * cilHost + " RemotePort: " + cilDestPort); } catch (IOException e) {
		 * bootLog.log(1, CLASS, rcsId, "init", "Unable to setup JCIL: " + e);
		 * throw new RCSStartupException("Unable to setup JCIL: " + e,
		 * JCIL_INIT); }
		 */
		/*
		 * int sl = rcs_config.getIntValue("cil.send.log.level", 1); int rl =
		 * rcs_config.getIntValue("cil.recv.log.level", 2);
		 * 
		 * JCIL.setLogging("COMMAND", sl, rl);
		 */
		// --------------------
		// Initialize CIL Proxy. KEEP
		// --------------------
		CIL_Proxy_Server.getInstance();

		int cilStart = rcs_config.getIntValue("cil.start.sequence", 1);
		CIL_ProxyRegistry.init(cilStart);
		bootLog.log(1, CLASS, rcsId, "init", "CIL_Proxy_Registry initialized:");

		// ----------------------------
		// Configure CIL_ProxyHandlers. KEEP
		// ----------------------------
		try {
			File file = rcs_config.getFile("tcs.command.handler.config.file", "config/tcs_cil_handler.properties");
			CIL_ProxyHandler.configure(file);
			bootLog.log(1, CLASS, rcsId, "init", "Configured CIL_ProxyHanders from file: " + file.getPath());
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error configuring CIL_Handlers: ", null, e);
			throw new RCSStartupException("Error initializing instruments: " + e, CIL_HANDLERS_INIT);
		}

		// ----------------------------------
		// Start CIL Proxy server and reader.
		// ----------------------------------
		/*
		 * CIL_ProxyReader cilReader = new CIL_ProxyReader();
		 * 
		 * cilReader.getDespatchThread().setDesc("CIL Proxy: Despatcher");
		 * threadRegistry.put("PROXY_DESPATCHER",cilReader.getDespatchThread());
		 * 
		 * cilReader.getReaderThread().setDesc("CIL Proxy: Reader");
		 * threadRegistry.put("PROXY_READER", cilReader.getReaderThread());
		 * cilReader.start(); bootLog.log(1, CLASS, rcsId, "init",
		 * "CIL_Proxy_Reader started:");
		 */
		// TODO INSERT POINT
		// insert new cil stuff using cilservice here, do not start other cil
		// stuff
		// on previous lines other than the proxyserver which JMS needs.
		CilService cil = null;
		try {
			DatagramSocket dsocket = new DatagramSocket(cilSendPort);
			bootLog.log(1, CLASS, rcsId, "init", "CIL Socket bound to: " + cilSendPort);

			Map registry = Collections.synchronizedMap(new HashMap());

			CilServer server = new CilServer(cilServicePort, dsocket, registry);

			bootLog.log(1, CLASS, rcsId, "init", "Creating cil server: " + server);

			bootLog.log(1, CLASS, rcsId, "init", "Cil Service linked to UDP socket: " + dsocket);
			InetAddress cilAddress = InetAddress.getByName(cilHost);
			server.setCilHostAddress(cilAddress);

			server.setCilPort(cilDestPort);

			server.setServiceClass(cilServiceClass);
			server.setTxId(cilTxId);
			server.setRxId(cilRxId);

			bootLog.log(1, CLASS, rcsId, "init", "Binding cil server to local rmi registry: " + server);
			Naming.rebind(cilServiceName, server);
			cil = server;
			bootLog.log(1, CLASS, rcsId, "init", "Bound Cil service: [" + cilServiceName + "] on rmi port " + cilServicePort
					+ "\n outgoing udp port: " + cilSendPort + "\n destination:       " + cilAddress + ":" + cilDestPort
					+ "\n ids: tx/rx:        " + cilTxId + "/" + cilRxId + "\n svc-class::    " + cilServiceClass);

			List messages = new Vector();

			ReaderThread reader = new ReaderThread(dsocket, messages);
			reader.start();

			DespatcherThread despatcher = new DespatcherThread(messages, registry);
			despatcher.start();
		} catch (Exception e) {
			bootLog.log(1, CLASS, rcsId, "init", "Unable to setup CIL services: " + e);
			throw new RCSStartupException("Unable to setup JCIL: " + e, JCIL_INIT);
		}

		// -----------------
		// Start ISS server.
		// -----------------

		ISS iss = ISS.getInstance();

		try {
			File file = rcs_config.getFile("iss.config.file", "config/iss.properties");
			iss.configure(file);
			bootLog.log(1, CLASS, rcsId, "init", "Configured ISS from file: " + file.getPath());
		} catch (IOException e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error binding ISS: ", null, e);
			throw new RCSStartupException("Error binding ISS: " + e, SERVER_BIND);
		}

		ISS_Server.getInstance().setDesc("Instrument Support System Server");
		threadRegistry.put("ISS_SERVER", ISS_Server.getInstance());
		// ISS.launch();
		// bootLog.log(1, CLASS, rcsId, "init", "ISS and server started:");

		// ----------------------
		// Start the TOSH server.
		// ----------------------
		int toshPort = 8410;
		try {
			toshPort = rcs_config.getIntValue("rcs.tosh.port", 8410);
			TOSH_Server.bindInstance(toshPort);
			bootLog.log(1, CLASS, rcsId, "init", "TOSH Server bound to port: " + toshPort);
		} catch (IOException e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error binding TOSH server to port: " + toshPort, null, e);
			throw new RCSStartupException("Error binding TOSH server to port: " + toshPort + " : " + e, SERVER_BIND);
		}
		TOSH_Server.getInstance().setDesc("Target of Opportunity Service Handler");
		threadRegistry.put("TOSH_SERVER", TOSH_Server.getInstance());
		TOSH_Server.launch();
		bootLog.log(1, CLASS, rcsId, "init", "TOSH_Server started:");

		// ----------------------
		// Start the TOCS server.
		// ----------------------
		int tocsPort = 8510;
		try {
			tocsPort = rcs_config.getIntValue("rcs.tocs.port", 8510);
			TOC_Server.bindInstance(tocsPort);
			bootLog.log(1, CLASS, rcsId, "init", "TOC Server temporarily bound to port: " + tocsPort);
		} catch (IOException e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error binding TOC server to port: " + tocsPort, null, e);
			throw new RCSStartupException("Error binding TOC server to port: " + tocsPort + " : " + e, SERVER_BIND); // #######
			// NOTE
			// THE
			// ERROR
			// CODE
		}
		TOC_Server.getInstance().setDesc("Target of Opportunity Server");
		threadRegistry.put("TOC_SERVER", TOC_Server.getInstance());
		TOC_Server.launch();
		bootLog.log(1, CLASS, rcsId, "init", "TOC_Server started:");

		// --------------------------
		// Start the Control servers.
		// --------------------------
		// CAMP server
		int ctrlPort = 9120;
		try {
			ctrlPort = rcs_config.getIntValue("rcs.control.port", 9120);
			Ctrl_Server.bindInstance("CTRL_SERVER", ctrlPort);
			bootLog.log(1, CLASS, rcsId, "init", "Control (CAMP) Server bound to port: " + ctrlPort);
		} catch (IOException e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error binding Control (CAMP) server to port: " + ctrlPort, null, e);
			throw new RCSStartupException("Error binding Control (CAMP) server to port: " + ctrlPort + " : " + e, SERVER_BIND);
		}
		Ctrl_Server.getInstance().setDesc("RCS Control (CAMP) Server");
		threadRegistry.put("CTRL_SERVER", Ctrl_Server.getInstance());

		// Engineering control server
		cmdServer = null;

		int cmdPort = 8221;
		try {
			cmdServer = new RCS_CommandServer("TEXT_SERVER", cmdPort);
			bootLog.log(1, CLASS, rcsId, "init", "Control (TEXT) Server bound to port: " + cmdPort);
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error binding Control (TEXT) server to port: " + cmdPort, null, e);
			throw new RCSStartupException("Error binding Control (TEXT) server to port: " + cmdPort + " : " + e, SERVER_BIND);
		}

		cmdServer.setDesc("RCS Command Interface (TEXT) Server");
		threadRegistry.put("TEXT_SERVER", cmdServer);
		cmdServer.start();
		bootLog.log(1, CLASS, rcsId, "init", "Control (TEXT) server started:");

		// -------------------------
		// Initialize Telemetry.
		// -------------------------
		Telemetry telemetry = Telemetry.getInstance();

		// -----------
		// Subsystems.
		// -----------
		RCS_SubsystemConnectionFactory.getInstance();

		// ------------------------------------
		// Observer Support System (Scheduler).
		// ------------------------------------
		bootLog.log(1, CLASS, rcsId, "init", "Setting up OSS link:");

		// String ossId = rcs_config.getProperty("oss.id", "OSS_COMMAND");
		// String ossHost = rcs_config.getProperty("oss.host", "ltccd1");
		// int ossPort = rcs_config.getIntValue("oss.port", 7910);

		String smsId = rcs_config.getProperty("sms.id", "SMS_COMMAND");
		String smsHost = rcs_config.getProperty("sms.host", "localhost");
		int smsPort = rcs_config.getIntValue("sms.port", 7939);

		// TCS comms - this is a hybrid system since /ngat.net.cil/ came into
		// existence.
		RCS_SubsystemConnectionFactory.addSlotResource(cilServerId, CIL_Proxy_Server.getInstance());

		// OSS (old scheduler)
		// RCS_SubsystemConnectionFactory.addSocketResource(ossId, ossHost,
		// ossPort);
		// bootLog.log(1, CLASS, rcsId, "init", "Configured OSS: CId: " + ossId
		// + " Host: " + ossHost + " Port: "
		// + ossPort);

		// SMS (new scheduler)
		RCS_SubsystemConnectionFactory.addSocketResource(smsId, smsHost, smsPort);
		bootLog.log(1, CLASS, rcsId, "init", "Configured SMS: CId: " + smsId + " Host: " + smsHost + " Port: " + smsPort);

		// ------------------------------------------------
		// Setup the ConnectionFactory for status handling.
		// ------------------------------------------------
		// CCS_StatusMonitorThread.setConnectionFactory(RCS_SubsystemConnectionFactory.getInstance());

		// --------------------------------------------------
		// Calibration - Photometric/Spectrometric Standards.
		// --------------------------------------------------

		domeLimit = Math.toRadians(rcs_config.getDoubleValue("telescope.dome.low.limit", 20.0));
		long duration = rcs_config.getLongValue("standards.duration", 15 * 60 * 1000L);
		try {
			telescope = new BasicTelescope();
			XmlConfigurator.use(new File("/occ/rcs/telescope.xml")).configure(telescope);
			bootLog.log(1, CLASS, rcsId, "init", "Telescope initialized.");

			telescope.addTelescopeStatusUpdateListener(StatusPool.getInstance());
			bootLog.log(1, CLASS, rcsId, "init", "Bound legacy StatusPool to telescope updates");

			BasicTelescopeSystem bts = (BasicTelescopeSystem) telescope.getTelescopeSystem();
			BasicTelescopeAlignmentAdjuster bta = bts.getAdjuster();
			bta.setCilService(cil);
			// TODO adjuster is not automatically enabled yet
			// bta.enableAdjustmentsSingle();

			// Link the adjuster to the telemetry feed
			// telescope.addTelescopeStatusUpdateListener(bta);
			bootLog.log(1, CLASS, rcsId, "init", "Bound Alignment Adjuster to telescope updates");

			telescope.startMonitoring(cil);
			bootLog.log(1, CLASS, rcsId, "init", "Telescope system monitoring IS started");

			GuidanceSystem guidance = telescope.getTelescopeSystem().getGuidanceSystem();
			List<Autoguider> aglist = guidance.listAutoguiders();
			for (int i = 0; i < aglist.size(); i++) {
				BasicAutoguider ag = (BasicAutoguider) aglist.get(i);
				telescope.startMonitoringAutoguiderState(ag);
			}

			Naming.rebind("Telescope", telescope);

			Naming.rebind("TelescopeAlignmentAdjuster", bts.getAdjuster());

			// TODO #TNG# TelescopeArchiveGateway tag = new
			// TelescopeArchiveGateway(telescope);
			// bootLog.log(1, CLASS, rcsId, "init",
			// "Telescope Archive Gateway initialised");
			// Naming.rebind("TelescopeGateway", tag);
			// tag.startProcessor();

		} catch (Exception e) {
			e.printStackTrace();
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error setting up Telescope system : " + e);
			throw new RCSStartupException("Error setting up telescope system: " + e, CALIB_INIT);
		}

		// legacy live-status service for AG temperature
		try {
			AgActiveProvider amp = new AgActiveProvider();
			telescope.addTelescopeStatusUpdateListener(amp);
			LegacyStatusProviderRegistry.getInstance().addStatusCategory("AGACTIVE", amp);

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			File file = rcs_config.getFile("calibration.config.file", "config/telescope_calib.properties");
			tcal = new TestTelescopeCalibration();
			PropertiesConfigurator.use(file).configure((PropertiesConfigurable) tcal);
			bootLog.log(1, CLASS, rcsId, "init", "Telescope calibration requirements loaded: " + tcal);
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error configuring Telescope calibration requirements: " + e);
			throw new RCSStartupException("Error configuring Telescope calibration requirements: " + e, CALIB_INIT);
		}

		try {
			File file = rcs_config.getFile("calibration.history.file", "config/telescope_calib_history.dat");
			tch = TelescopeCalibrationHistory.load(file);
		} catch (Exception e) {
			bootLog.log(WARNING, 1, CLASS, rcsId, "init", "Error loading Telescope calibration history: " + e);
		}
		// check it wasnt saved as a null object on first load/save cycle.
		if (tch == null) {
			tch = new TelescopeCalibrationHistory();
		}
		bootLog.log(1, CLASS, rcsId, "init", "Telescope calibration history loaded: " + tch);

		// -------------------------
		// Intrumentation Subsytems.
		// -------------------------
		bootLog.log(1, CLASS, rcsId, "init", "Setting up Instrumentation");

		// -----------------------------------------
		// Configure the current set of Instruments.
		// -----------------------------------------
		/*
		 * try { File file = rcs_config.getFile("instruments.config.file",
		 * "config/instrument.properties"); Instruments.initialize(this, file,
		 * "BOOT"); bootLog.log(1, CLASS, rcsId, "init",
		 * "Instruments initialized."); } catch (IOException e) {
		 * bootLog.log(FATAL, 1, CLASS, rcsId, "init",
		 * "Error initializing instruments: ", null, e); throw new
		 * RCSStartupException("Error initializing instruments: " + e,
		 * INSTRUMENTS_INIT); }
		 */

		// ------------------------------------
		// Setup the Instrument Status Updater.
		// ------------------------------------
		/*
		 * InstrumentStatusUpdater.setConnectionFactory(
		 * RCS_SubsystemConnectionFactory .getInstance()); String
		 * instrumentStatusUpdaterId =
		 * rcs_config.getProperty("instrument.status.updater.id",
		 * "INST_UPDATER"); InstrumentStatusUpdater instrumentStatusUpdater =
		 * new InstrumentStatusUpdater(instrumentStatusUpdaterId);
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Initialized Instrument status updater: " +
		 * instrumentStatusUpdaterId);
		 */
		// ---------------------------------
		// Setup Instrument Status Monitors.
		// ---------------------------------
		/*
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Setting up Instrument Status monitors."); icsStatusMonitors = new
		 * HashMap();
		 * 
		 * ConfigurationProperties instNetworkConfig; ConfigurationProperties
		 * instMonitorConfig; // ConfigurationProperties instStandardConfig;
		 * 
		 * // Load the instruments' network properties. try { File file = new
		 * File("instruments", "network.properties"); instNetworkConfig = new
		 * ConfigurationProperties(); InputStream in = new
		 * FileInputStream(file); instNetworkConfig.load(in); } catch
		 * (IOException e) { bootLog.log(FATAL, 1, CLASS, rcsId, "init",
		 * "Error initializing instruments: ", null, e); throw new
		 * RCSStartupException
		 * ("Error initializing instrument network properties: " + e,
		 * INSTRUMENTS_INIT); }
		 * 
		 * // Load the instruments' monitor properties. try { File file = new
		 * File("instruments", "monitor.properties"); instMonitorConfig = new
		 * ConfigurationProperties(); InputStream in = new
		 * FileInputStream(file); instMonitorConfig.load(in); bootLog .log(1,
		 * CLASS, rcsId, "init",
		 * "Loaded instrument monitoring properties from file: " +
		 * file.getPath()); bootLog.log(1, CLASS, rcsId, "init",
		 * "Loaded instrument monitoring properties: " + instMonitorConfig);
		 * 
		 * } catch (IOException e) { bootLog.log(FATAL, 1, CLASS, rcsId, "init",
		 * "Error initializing instruments: ", null, e); throw new
		 * RCSStartupException
		 * ("Error initializing instrument monitor properties: " + e,
		 * INSTRUMENTS_INIT); }
		 */

		// STARTUP SCM_X - cant we get rid of this ??????
		SCM_ControllerX.getController();

		/*
		 * int icsCount = 0; Iterator it = Instruments.findInstrumentSet();
		 * while (it.hasNext()) { String instId = (String) it.next();
		 * 
		 * String instRef = instId.toLowerCase();
		 * 
		 * String icsHost = instNetworkConfig.getProperty(instRef + ".host");
		 * int icsPort = instNetworkConfig.getIntValue(instRef + ".port", 6783);
		 * 
		 * // Setup Network Connection Entries based on: 'ICS_ID' and //
		 * 'ICS_Config_Class_Name'.
		 * RCS_SubsystemConnectionFactory.addSocketResource(instId, icsHost,
		 * icsPort);
		 * 
		 * // Create a Status Pool for the Instrument. int icspoolSize =
		 * instMonitorConfig.getIntValue(instRef + ".status.pool.size", 20);
		 * 
		 * CCSPool.getInstance(instId, icspoolSize).latest(); bootLog.log(1,
		 * CLASS, rcsId, "init", "Status_Pool CCS_[" + instId +
		 * "] was initialized at size: " +
		 * CCSPool.getInstance(instId).getSize()); boolean icsLogEnable =
		 * instMonitorConfig.getBooleanValue(instRef + ".log.enable"); // Pool
		 * notification. // if (icsLogEnable) { //
		 * CCSPool.getInstance(instId).register(statusLogger); // }
		 * 
		 * // EMM Registration. //
		 * EMM_Registry.getInstance().addStatusategory(instId, //
		 * CCSPool.getInstance(instId).latest());
		 * 
		 * boolean icsEnable = instMonitorConfig.getBooleanValue(instRef +
		 * ".status.monitor.enable");
		 * 
		 * long icsSmmUpdate = instMonitorConfig.getLongValue(instRef +
		 * ".status.monitor.update", 10000L); bootLog .log(1, CLASS, rcsId,
		 * "init", "KEY: [" + instRef + ".status.monitor.update] Value = " +
		 * icsSmmUpdate); int icsSmmLevel =
		 * instMonitorConfig.getIntValue(instRef + ".status.monitor.level", 1);
		 * 
		 * // Data logging. String instLogFileName =
		 * instMonitorConfig.getProperty(instRef + ".status.log.file",
		 * "logs/rcs_" + instId + ".log"); File instLogFile = new
		 * File(instLogFileName); int instLogLevel =
		 * instMonitorConfig.getIntValue(instRef + ".status.log.level", 1);
		 * 
		 * // ------------------------------------------------------- // START
		 * NEW IMC CONFIG HERE //
		 * -------------------------------------------------------
		 * 
		 * bootLog.log(INFO, 1, CLASS, rcsId, "init",
		 * "Starting Instrument NEW setup for monitoring on: " + instId);
		 * 
		 * String instSMCFileName = instMonitorConfig.getProperty(instRef +
		 * ".monitor.config.file"); if (instSMCFileName != null) {
		 * 
		 * File smcFile = new File("instruments", instSMCFileName); if
		 * (smcFile.exists()) {
		 * 
		 * // Create a network resource ID. String ismNetResName = instId +
		 * "NET";
		 * 
		 * NetworkStatusProvider ismNsp =
		 * SCM_ControllerX.getController().addNetworkResource(ismNetResName);
		 * NetworkStatus ismNetStatus = (NetworkStatus) ismNsp.getStatus();
		 * InstrumentStatusClient ismClient = new
		 * InstrumentStatusClient(instId);
		 * 
		 * DefaultStatusLogger statusLogger = new DefaultStatusLogger(new
		 * File("data/" + instId)); statusLogger.setFormatter(new
		 * InstrumentStatusLogFormatter());
		 * 
		 * StatusMonitorThread ismThread = null;
		 * 
		 * try { ismClient.configure(smcFile);
		 * 
		 * // Add the created client to SCM_X. // This is also a StatusProvider
		 * and hence the thing // that provides the // StatusCategory returned
		 * from the EMM when we ask for // a named category.
		 * 
		 * ismThread = SCM_ControllerX.getController().addStatusMonitor(instId,
		 * "X_" + instId + "_IMT", ismClient, ismNetStatus,
		 * "Xperimental monitor for: " + instId, icsEnable, icsSmmUpdate,
		 * statusLogger);
		 * 
		 * threadRegistry.put(instId, ismThread);
		 * 
		 * bootLog.log(INFO, 1, CLASS, rcsId, "init",
		 * "Finished Instrument NEW setup for monitoring on: " + instId);
		 * 
		 * } catch (Exception ex) { bootLog.log(INFO, 1, CLASS, rcsId, "init",
		 * "Failed Instrument NEW setup for monitoring on: " + instId +
		 * " Due to: " + ex); }
		 * 
		 * } }
		 */
		// -------------------------------------------------------
		// END NEW IMC CONFIG HERE
		// -------------------------------------------------------

		// } // next instrument

		// ============================================================
		// HERE WE START THE VERY VERY NEW ICM BASED INSTRUMENT STUFF
		// ============================================================
		try {
			ireg = new BasicInstrumentRegistry();
			XmlConfigurator.use(new File("/occ/rcs/ireg.xml")).configure(ireg);
			Naming.rebind("InstrumentRegistry", ireg);
			List ilist = ireg.listInstruments();
			System.err.println("Created and bound instrument registry with " + (ilist == null ? "NO" : ilist.size())
					+ " instruments");

			// obtain proxy controllers
			try {
				// File networkConfigFile = new
				// File("config/network.properties");
				// ConfigurationProperties networkCfg = new
				// ConfigurationProperties();
				// networkCfg.load(new FileInputStream(networkConfigFile));

				Iterator iin = ilist.iterator();
				while (iin.hasNext()) {
					InstrumentDescriptor instId = (InstrumentDescriptor) iin.next();

					ControllerProxy proxy = ireg.getController(instId).getControllerProxy();
					String icsHost = proxy.getHost();
					int icsPort = proxy.getPort();

					String iname = instId.getInstrumentName().toLowerCase();
					// String icsHost = networkCfg.getProperty(iname + ".host");
					// int icsPort = networkCfg.getIntValue(iname + ".port");
					RCS_SubsystemConnectionFactory.addSocketResource(instId.getInstrumentName(), icsHost, icsPort);
					System.err.println("Add instrument resource: " + instId.getInstrumentName() + "@" + icsHost + ":" + icsPort);

					// add sub inst connection resources...

					List<InstrumentDescriptor> sublist = instId.listSubcomponents();
					for (int is = 0; is < sublist.size(); is++) {
						InstrumentDescriptor subid = sublist.get(is);
						String subname = instId.getInstrumentName() + "_" + subid.getInstrumentName();
						RCS_SubsystemConnectionFactory.addSocketResource(subname, icsHost, icsPort);
						System.err.println("Add instrument resource: " + subname + "@" + icsHost + ":" + icsPort);
					}

					// Make some sort of thing which is able to provide
					// polled instrument status for these feckers as live-status
					// needs this for now.
					InstrumentStatusProvider oisp = new InstrumentStatusProvider(instId);
					ireg.getStatusProvider(instId).addInstrumentStatusUpdateListener(oisp);
					LegacyStatusProviderRegistry.getInstance().addStatusCategory(instId.getInstrumentName(), oisp);

				}
			} catch (Exception e) {
				e.printStackTrace();
				bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error reading inst network info : ", null, e);
				throw new RCSStartupException("Error reading inst network info: " + e, EMS_CONFIG);
			}

		} catch (Exception icx) {
			icx.printStackTrace();
		}

		try {
			InstrumentArchiveGateway iag = new InstrumentArchiveGateway(ireg);
			bootLog.log(1, CLASS, rcsId, "init", "Instrument Archive Gateway initialised");
			Naming.rebind("InstrumentGateway", iag);
			iag.setProcessInterval(10000L);
			iag.setBackingStoreAgeLimit(10 * 60 * 1000L);

			iag.startProcessor();
		} catch (Exception iax) {
			iax.printStackTrace();
		}

		// now lets see if we set these feckers up right, fail if any
		// inconsistency...
		SciencePayload sci = null;
		try {
			List instList = ireg.listInstruments();
			sci = telescope.getTelescopeSystem().getSciencePayload();
			Iterator in = instList.iterator();
			while (in.hasNext()) {
				InstrumentDescriptor iid = (InstrumentDescriptor) in.next();
				String alias = sci.getAliasForInstrument(iid);
				int aperture = sci.getApertureNumberForInstrument(iid);
				File mount = sci.getMountPointForInstrument(iid);
				int reboot = sci.getRebootLevelForInstrument(iid);
				int port = sci.getPortForInstrument(iid);
				System.err.println("SciPayload: " + iid.getInstrumentName() + " at: " + port + ", alias: " + alias + ", mount: "
						+ mount + ", apno: " + aperture + ", reboot: " + reboot);
			}
		} catch (Exception icx) {
			icx.printStackTrace();
			throw new RCSStartupException("Startup Error: conflicting ireg/sci-playload setup: " + icx, INSTRUMENTS_INIT);

		}

		// TODO must be a better way to hook these up ??
		ParallelTaskImpl.setInstrumentRegistry(ireg);
		ParallelTaskImpl.setSciencePayload(sci);
		ConfigTranslator.ireg = ireg;

		// Internal status provider
		/*
		 * NetworkStatusProvider intNsp =
		 * SCM_ControllerX.getController().addNetworkResource("INT_NET");
		 * NetworkStatus intNetStatus = (NetworkStatus) intNsp.getStatus();
		 * RCSInternalStatusClient intClient = new RCSInternalStatusClient();
		 * 
		 * DefaultStatusLogger intStatusLogger = new DefaultStatusLogger(new
		 * File("data/internal")); intStatusLogger.setFormatter(new
		 * DefaultStatusLogFormatter());
		 * 
		 * long internalUpdatePeriod =
		 * rcs_config.getLongValue("internal.status.monitor.period", 30000L);
		 * 
		 * StatusMonitorThread intThread = null; intThread =
		 * SCM_ControllerX.getController().addStatusMonitor("INTERNAL",
		 * "X_INTERNAL_IMT", intClient, intNetStatus,
		 * "Xperimental internal state monitor", true, internalUpdatePeriod,
		 * intStatusLogger);
		 * 
		 * threadRegistry.put("INTERNAL", intThread);
		 */

		// LEGACY Class which can provide INTERNAL status for the live-status
		// feed.
		InternalStatusProvider intsp = new InternalStatusProvider();
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("INTERNAL", intsp);
		bootLog.log(INFO, 1, CLASS, rcsId, "init", "Added INTERNAL status provider");

		// OSS monitor status provider
		/*
		 * NetworkStatusProvider ossNsp =
		 * SCM_ControllerX.getController().addNetworkResource("OSS_NET");
		 * NetworkStatus ossNetStatus = (NetworkStatus) ossNsp.getStatus();
		 * OssStatusClient ossClient = new OssStatusClient();
		 * 
		 * try { File ossFile = new File("config/oss_monitor.properties");
		 * ossClient.configure(ossFile);
		 * 
		 * DefaultStatusLogger ossStatusLogger = new DefaultStatusLogger(new
		 * File("data/oss")); ossStatusLogger.setFormatter(new
		 * DefaultStatusLogFormatter());
		 * 
		 * long ossUpdatePeriod =
		 * rcs_config.getLongValue("oss.status.monitor.period", 120000L);
		 * 
		 * StatusMonitorThread ossThread = null; ossThread =
		 * SCM_ControllerX.getController().addStatusMonitor("X_OSS_MONITOR",
		 * "X_OSS_MONITOR_IMT", ossClient, ossNetStatus,
		 * "Xperimental OSS state monitor", true, ossUpdatePeriod,
		 * ossStatusLogger);
		 * 
		 * threadRegistry.put("OSS_MONITOR", ossThread); bootLog.log(INFO, 1,
		 * CLASS, rcsId, "init", "Added OSS status monitor");
		 * 
		 * } catch (Exception osx) { bootLog.log(INFO, 1, CLASS, rcsId, "init",
		 * "Failed setup for OSS monitoring due to: " + osx); }
		 */

		// ############# TEMP CONFIGURE STATUS - using SPP as wrapper round
		// TCS_Status cats.
		StatusPool.latest();

		StatusPoolProvider sppMeteo = new StatusPoolProvider(StatusPool.latest().meteorology);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("METEO", sppMeteo);

		StatusPoolProvider sppMech = new StatusPoolProvider(StatusPool.latest().mechanisms);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("MECHANISM", sppMech);

		StatusPoolProvider sppLim = new StatusPoolProvider(StatusPool.latest().limits);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("LIMITS", sppLim);

		StatusPoolProvider sppState = new StatusPoolProvider(StatusPool.latest().state);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("STATE", sppState);

		StatusPoolProvider sppAstro = new StatusPoolProvider(StatusPool.latest().astrometry);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("ASTRO", sppAstro);

		StatusPoolProvider sppTime = new StatusPoolProvider(StatusPool.latest().time);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("TIME", sppTime);

		StatusPoolProvider sppAuto = new StatusPoolProvider(StatusPool.latest().autoguider);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("AUTOGUIDER", sppAuto);

		StatusPoolProvider sppNet = new StatusPoolProvider(StatusPool.latest().network);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("NETWORK", sppNet);

		StatusPoolProvider sppSrc = new StatusPoolProvider(StatusPool.latest().source);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("SOURCE", sppSrc);

		StatusPoolProvider sppCal = new StatusPoolProvider(StatusPool.latest().calibrate);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("CALIB", sppCal);

		// SkyModel
		try {
			skyModel = new DefaultMutableSkyModel(8, 120000.0);
			Naming.rebind("rmi://localhost:1099/SkyModel", skyModel);
			bootLog.log(1, CLASS, rcsId, "init", "Bound SkyModel to local registry");

			// TODO #TNG# SkyModelArchiveGateway sag = new
			// SkyModelArchiveGateway((SkyModelMonitor) skyModel);
			// bootLog.log(1, CLASS, rcsId, "init",
			// "SkyModel Archive Gateway initialised");
			// Naming.rebind("SkyModelGateway", sag);
			// sag.startProcessor();

			// Make some sort of thing which is able to provide
			// sky status.

			// TODO This code is temp moved into .. .. while old rcs is still
			// running...
			// TODO SkyModelProvider smp = new SkyModelProvider();
			// TODO ((DefaultMutableSkyModel)
			// skyModel).addSkyModelUpdateListener(smp);
			// TODO EMM_Registry.getInstance().addStatusCategory("SKY", smp);

			// TODO TEMP FIX: Add a file handler - this to allow photom state to
			// persist between restarts
			// An external cronjob looks in the file every minute and updates
			// the SkyModel photom state.
			FileLogSkyListener flist = new FileLogSkyListener("/occ/data/photom.dat");
			((SkyModelMonitor) skyModel).addSkyModelUpdateListener(flist);

		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing SkyModel : ", null, ex);
			throw new RCSStartupException("Error initializing EMS SkyModel: " + ex, EMS_CONFIG);
		}

		try {
			// LEGACY Class which can provide SKY status for the live-status
			// feed.
			SkyModelProvider smp = new SkyModelProvider(skyModel);
			((DefaultMutableSkyModel) skyModel).addSkyModelUpdateListener(smp);
			// Note the different call to register as SeeingStatus is NOT a
			// StatusCategory like the other cats
			LegacyStatusProviderRegistry.getInstance().setSkyModelProvider(smp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// EMS
		try {

			meteo = new BasicMeteorologyProvider();
			Naming.rebind("rmi://localhost:1099/Meteorology", meteo);
			bootLog.log(1, CLASS, rcsId, "init", "Bound MeteoProvider to local registry");

			// link meteo provider to feed into SP
			meteo.addMeteorologyStatusUpdateListener(StatusPool.getInstance());
			bootLog.log(1, CLASS, rcsId, "init", "Bound legacy StatusPool to meteo updates");

			meteo.startMonitoring(cil);
			bootLog.log(1, CLASS, rcsId, "init", "METEO monitoring of WMS is started using CIL");

			// monitor BCS
			URL bcsUrl = new URL("file:///occ/data/cloud.dat");
			meteo.startMonitoringBcs(bcsUrl);
			bootLog.log(1, CLASS, rcsId, "init", "METEO monitoring of external CLOUD sensor is started using: " + bcsUrl);

			// LEGACY Class which can provide BCS status for the live-status
			// feed.
			BcsCloudStatusProvider bcsStatusProvider = new BcsCloudStatusProvider();
			meteo.addMeteorologyStatusUpdateListener(bcsStatusProvider);
			LegacyStatusProviderRegistry.getInstance().addStatusCategory("CLOUD", bcsStatusProvider);

			// monitor TNG Dust
			URL dustUrl = new URL("file:///occ/data/tng_dust.dat");
			meteo.startMonitoringDust(dustUrl);
			bootLog.log(1, CLASS, rcsId, "init", "METEO monitoring of external DUST sensor is started using: " + dustUrl);
			// LEGACY Class which can provide TNG Dust status for the
			// live-status feed.
			TNGDustStatusProvider tngDustStatusProvider = new TNGDustStatusProvider();
			meteo.addMeteorologyStatusUpdateListener(tngDustStatusProvider);
			LegacyStatusProviderRegistry.getInstance().addStatusCategory("TNGDUST", tngDustStatusProvider);

			// TODO #TNG# MeteorologyArchiveGateway mag = new
			// MeteorologyArchiveGateway(meteo);
			// bootLog.log(1, CLASS, rcsId, "init",
			// "Meteorology Archive Gateway initialised");
			// Naming.rebind("MeteorologyGateway", mag);
			// mag.startProcessor();

		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing MeteorologyProvider : ", null, ex);
			throw new RCSStartupException("Error initializing EMS MeteorologyProvider: " + ex, EMS_CONFIG);
		}

		// ---------------------------------------
		// IMS (infrastructure monitoring System)
		// ---------------------------------------
		try
		{
			// create object for monitoring disk status
			diskStatusProvider = new BasicDiskStatusProvider();
			// add to RMI registry
			Naming.rebind("rmi://localhost:1099/DiskStatus",diskStatusProvider);
			bootLog.log(1, CLASS, rcsId, "init", "Bound DiskStatusProvider to local registry");
			// create legacy disk status provider, this is an interface between
			// the new RMI-based system an older legacy systems
			LegacyDiskStatusProvider legacyDiskStatusProvider = new LegacyDiskStatusProvider();
			// ensure the legacy data gets updated by the disk status provider.
			diskStatusProvider.addDiskStatusUpdateListener(legacyDiskStatusProvider);
			// link meteo provider to feed into SP
			LegacyStatusProviderRegistry.getInstance().addStatusCategory("DISKS",legacyDiskStatusProvider);
			bootLog.log(1, CLASS, rcsId, "init", "Bound legacy StatusPool to disk status updates");
			// starts disk status monitoring thread
			diskStatusProvider.startMonitoringThread(new URL("config/disk_status.properties"));
			bootLog.log(1, CLASS, rcsId, "init", "Disk monitoring thread started.");
		}
		catch (Exception ex) 
		{
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing DiskStatusProvider : ", null, ex);
			throw new RCSStartupException("Error initializing IMS DiskStatusProvider: " + ex, IMS_CONFIG);
		}

		
		
		// ---------------------
		// New Reactive System
		// ---------------------

		BasicReactiveSystem trs = null;
		try {
			trs = new BasicReactiveSystem(site);

			File file = rcs_config.getFile("reactive.system.rulebase", "rules.xml");
			XmlConfigurator.use(file).configure(trs);

			// Reactive system needs input from METEO and TCM
			telescope.addTelescopeStatusUpdateListener(trs);
			meteo.addMeteorologyStatusUpdateListener(trs);

			trs.startCacheReader();

			Naming.rebind("rmi://localhost:1099/ReactiveSystem", trs);
			bootLog.log(1, CLASS, rcsId, "init", "Bound ReactiveSystem to local registry");
		} catch (Exception e) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error loading Reactive system: " + e);
			e.printStackTrace();
		}
		bootLog.log(1, CLASS, rcsId, "init", "Reactive system running: " + trs);

		// switch off temporarily while investigating open socket problems
		/*try {
			ReactiveSystemArchiveGateway rag = new ReactiveSystemArchiveGateway(trs);
			bootLog.log(1, CLASS, rcsId, "init", "Reactive System Archive Gateway initialised");
			Naming.rebind("ReactiveSystemGateway", rag);
			rag.setProcessInterval(10000L);
			rag.setBackingStoreAgeLimit(10 * 60 * 1000L);
			rag.startProcessor();
		} catch (Exception iax) {
			iax.printStackTrace();
		}*/

		// ----------------------------
		// (Group) Operations Monitor
		// ----------------------------

		try {
			gom = new DefaultGroupOperationsMonitor();
			Naming.rebind("rmi://localhost:1099/GroupOperationsMonitor", gom);
			bootLog.log(1, CLASS, rcsId, "init", "Bound GroupOperationsMonitor to local registry");
		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing Group Operations Monitor : ", null, ex);
			throw new RCSStartupException("Error initializing Group Operations Monitor: " + ex, GOM_CONFIG);
		}

		// ---------------------
		// Calibration Monitor
		// ---------------------

		try {
			bcalMonitor = new BasicCalibrationMonitor();
			Naming.rebind("rmi://localhost:1099/CalibrationMonitor", bcalMonitor);
			bootLog.log(1, CLASS, rcsId, "init", "Bound CalibrationMonitor to local registry");
		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing CalibrationMonitor: ", null, ex);
			throw new RCSStartupException("Error initializing CalibrationMonitor: " + ex, TMM_CONFIG);
		}

		// setup something to log calibration data
		try {
			bcalLog = new BasicCalibrationUpdateListener(new File("data/calib_log.dat"));
			bcalMonitor.addCalibrationUpdateListener(bcalLog);
			bootLog.log(1, CLASS, rcsId, "init", "Registered calibration logger with CalibrationMonitor");
		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing CalibrationMonitor: ", null, ex);
			throw new RCSStartupException("Error initializing CalibrationMonitor: " + ex, TMM_CONFIG);
		}

		try {
			bookingModel = new DefaultMutableAdvancedBookingModel();
			Naming.rebind("rmi://localhost:1099/AdvancedBookingModel", bookingModel);
			bootLog.log(1, CLASS, rcsId, "init", "Bound AdvancedBookingModel to local registry");
		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error starting up advanced booking model: ", null, ex);
			throw new RCSStartupException("Error starting advanced booking model: " + ex, TMM_CONFIG);
		}
		try {
			taskMonitor = new BasicTaskMonitor();
			Naming.rebind("rmi://localhost:1099/TaskMonitor", taskMonitor);
			bootLog.log(1, CLASS, rcsId, "init", "Bound TaskMonitor to local registry");
			taskMonitor.startEventDespatcher();

			/*
			 * try { TaskArchiveGateway tag = new
			 * TaskArchiveGateway(taskMonitor); bootLog.log(1, CLASS, rcsId,
			 * "init", "Task Archive Gateway initialised");
			 * Naming.rebind("TaskGateway", tag);
			 * tag.setProcessInterval(10000L); tag.setBackingStoreAgeLimit(10 *
			 * 60 * 1000L); tag.startProcessor(); } catch (Exception iax) {
			 * iax.printStackTrace(); }
			 */

		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error starting up task monitor: ", null, ex);
			throw new RCSStartupException("Error starting task monitor: " + ex, TMM_CONFIG);
		}

		// #####################
		// invokeTestStatus();
		// #####################

		// ----------------------------------------------
		// Setup OLD STYLE Sensors and Filters and attach Monitors.
		// ----------------------------------------------
		/*
		 * try { File file = rcs_config.getFile("rcs.sensor.config.file",
		 * "sensors.config"); Sensors.configure(file); bootLog.log(1, CLASS,
		 * rcsId, "init", "Configured Sensor swarm from file: " +
		 * file.getPath()); } catch (FileNotFoundException fnfx) {
		 * Sensors.defaultSetup(); bootLog.log(1, CLASS, rcsId, "init",
		 * "Unable to load Sensor config - used defaults."); }
		 * 
		 * try { File file = rcs_config.getFile("rcs.filter.config.file",
		 * "filters.config"); Filters.configure(file); bootLog.log(1, CLASS,
		 * rcsId, "init", "Configured Filter rabble from file: " +
		 * file.getPath()); } catch (FileNotFoundException fnfx) {
		 * Filters.defaultSetup(); bootLog.log(1, CLASS, rcsId, "init",
		 * "Unable to load Filter config - used defaults."); }
		 * 
		 * try { File file = rcs_config.getFile("rcs.monitor.config.file",
		 * "monitors.config"); Monitors.configure(file); bootLog.log(1, CLASS,
		 * rcsId, "init", "Configured Monitor throng from file: " +
		 * file.getPath()); } catch (FileNotFoundException fnfx) {
		 * Monitors.defaultSetup(); bootLog.log(1, CLASS, rcsId, "init",
		 * "Unable to load Monitor config - used defaults."); }
		 * 
		 * // ---------------------------------------------- // Setup NEW STYLE
		 * Sensors and Filters and attach Monitors. //
		 * ----------------------------------------------
		 * 
		 * try { File file = rcs_config.getFile("rcs.sensor.config.file",
		 * "config/sensors.properties"); SensorsXX.configure(file);
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Configured SensorXX swarm from file: " + file.getPath()); } catch
		 * (IOException fnfx) { // SensorsXX.defaultSetup(); bootLog.log(1,
		 * CLASS, rcsId, "init",
		 * "Unable to load SensorsXX config - used XX defaults."); } try { File
		 * file = rcs_config.getFile("rcs.filter.config.file",
		 * "config/filters.properties"); FiltersXX.configure(file);
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Configured FiltersXX rabble from file: " + file.getPath()); } catch
		 * (IOException fnfx) { // FiltersXX.defaultSetup(); bootLog.log(1,
		 * CLASS, rcsId, "init",
		 * "Unable to load FiltersXX config - used XX defaults."); } try { File
		 * file = rcs_config.getFile("rcs.monitor.config.file",
		 * "config/monitors.properties"); MonitorsXX.configure(file);
		 * bootLog.log(1, CLASS, rcsId, "init",
		 * "Configured MonitorXX throng from file: " + file.getPath()); } catch
		 * (IOException fnfx) { // MonitorsXX.defaultSetup(); bootLog.log(1,
		 * CLASS, rcsId, "init",
		 * "Unable to load MonitorXX config - used defaults."); }
		 */
		// ############# TEMP CONFIGURE STATUS

		// --------------------------
		// Setup FITS header storage.
		// --------------------------
		bootLog.log(1, CLASS, rcsId, "init", "Setting up FITS_Header event subscriptions.");

		// TODO This update mechanism is out of date, FITS_HeaderInfo should
		// subscribe
		// directly to telescope and Meteo feeds, then the following section can
		// be zapped

		// TODO
		// telescope.addTelescopeStatusUpdateListener(FITS_HeaderInfo.getInstance());
		// TODO
		// meteo.addMeteorologyStatusUpdateListener(FITS_HeaderInfo.getInstance());

		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.SOURCE_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.ASTROMETRY_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.METEOROLOGY_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.AUTOGUIDER_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.MECHANISMS_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.TIME_UPDATE_EVENT);
		StatusPool.register(FITS_HeaderInfo.getInstance(), StatusPool.STATE_UPDATE_EVENT);

		// ### TRACKING MONITOR
		try {
			defaultTrackingMonitor = new DefaultTrackingMonitor();
			// defaultTrackingMonitor.setTrackAz(true);
			defaultTrackingMonitor.setTrackRot(true);
			defaultTrackingMonitor.setMaxTrackingLostTime(rcs_config.getLongValue("max.tracking.lost.time", 10000L));
			// defaultTrackingMonitor.setMaxRotTrackError(Math.toRadians(rcs_config.getDoubleValue(
			// "max.rotator.tracking.error", 60.0) / 3600.0));
			// arcsec-rads

			defaultTrackingMonitor.setTrackAz(true); 
			// DISABLE

			// StatusPool.register(defaultTrackingMonitor,
			// StatusPool.MECHANISMS_UPDATE_EVENT);

			bootLog.log(1, CLASS, rcsId, "init", "Initialized axis tracking monitor");
			Naming.rebind("rmi://localhost:1099/TrackingMonitor", defaultTrackingMonitor);
			System.err.println("Bound axis tracking monitor to registry as TrackingMonitor");

			telescope.addTelescopeStatusUpdateListener(defaultTrackingMonitor);
			bootLog.log(1, CLASS, rcsId, "init", "Linked axis tracking monitor to telescope");

		} catch (Exception e) {
			e.printStackTrace();
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing TrackingMonitor: ", null, e);
			throw new RCSStartupException("Error initializing TrackingMonitor: " + e, TMM_CONFIG);
		}
		// ### AUTOGUIDER MONITOR

		// TODO these need disabling before we go onto OCC at the moment
		// 10-may-2011
		try {
			defaultAutoguiderMonitor = new DefaultAutoguiderMonitor("CASS");
			defaultAutoguiderMonitor.setMaxGuidingLostTime(rcs_config.getLongValue("max.autoguider.lost.time", 10000L));

			// StatusPool.register(defaultAutoguiderMonitor,
			// StatusPool.MECHANISMS_UPDATE_EVENT);

			bootLog.log(1, CLASS, rcsId, "init", "Initialized Autoguider monitor");
			Naming.rebind("rmi://localhost:1099/AutoguiderMonitor", defaultAutoguiderMonitor);
			System.err.println("Bound AutoguiderMonitor to registry");

			telescope.addTelescopeStatusUpdateListener(defaultAutoguiderMonitor);
			bootLog.log(1, CLASS, rcsId, "init", "Linked autoguider lock monitor to telescope");

		} catch (Exception e) {
			e.printStackTrace();
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing AutoguiderMonitor: ", null, e);
			throw new RCSStartupException("Error initializing AutoguiderMonitor: " + e, TMM_CONFIG);
		}

		// INSTRUMENT MONITOR
		try {
			defaultInstrumentMonitor = new DefaultInstrumentMonitor();
			// register with all instruments...
			List instList = ireg.listInstruments();
			Iterator in = instList.iterator();
			while (in.hasNext()) {
				InstrumentDescriptor iid = (InstrumentDescriptor) in.next();
				ngat.icm.InstrumentStatusProvider isp = ireg.getStatusProvider(iid);
				isp.addInstrumentStatusUpdateListener(defaultInstrumentMonitor);
				bootLog.log(1, CLASS, rcsId, "init",
						"Registering InstMonitor with status provider for: " + iid.getInstrumentName());
			}

			bootLog.log(1, CLASS, rcsId, "init", "Initialized Instrument monitor");
			Naming.rebind("rmi://localhost:1099/InstrumentMonitor", defaultInstrumentMonitor);
			System.err.println("Bound InstrumentMonitor to registry");

		} catch (Exception e) {
			e.printStackTrace();
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing InstrumentMonitor: ", null, e);
			throw new RCSStartupException("Error initializing InstrumentMonitor: " + e, TMM_CONFIG);
		}

		// ### PMC MONITOR
		pmcMonitor = new ngat.rcs.scm.detection.PmcMonitor();

		StatusPool.register(pmcMonitor, StatusPool.MECHANISMS_UPDATE_EVENT);
		bootLog.log(1, CLASS, rcsId, "init", "Initialized PMC monitor");

		// --------------------------------------
		// Initialize Event Despatcher thread(s).
		// --------------------------------------
		bootLog.log(1, CLASS, rcsId, "init", "Setting up Event_Despatcher.");
		int edtCount = rcs_config.getIntValue("event.despatcher.count", 1);
		long edtCycleTime = rcs_config.getLongValue("event.despatcher.cycle.time", 2000L);
		for (int i = 0; i < edtCount; i++) {
			EventDespatcher eventDespatcher = new EventDespatcher(edtCycleTime);
			eventDespatcher.setDesc("Event Despatcher");
			threadRegistry.put("EVENT_DESPATCHER", eventDespatcher);
			// SWITCHED OFF FOR EVER ???
			// eventDespatcher.start();
			bootLog.log(1, CLASS, rcsId, "init", "\nStarted Event_Despatcher #" + i + "\n\tCycle time:      "
					+ (edtCycleTime / 1000) + " secs." + "\n\tThread-Priority: " + eventDespatcher.getPriority());
		}

		// -----------------------------------################################################
		// Load the Engineering Mode Settings.
		// -----------------------------------################################################
		try {
			File engfile = rcs_config.getFile("engineering.mode.config.file", "engineering.config");
		} catch (IOException iox) {
			bootLog.log(1, CLASS, rcsId, "init", "Unable to load Engineering mode settings - IGNORING.");
		} catch (IllegalArgumentException iax) {
			bootLog.log(1, CLASS, rcsId, "init", "Unable to load Engineering mode settings - IGNORING.");
		}

		// Initialize the TMM.
		try {
			TaskOperations.initialize();
			bootLog.log(1, CLASS, "init", "Initialized TaskOperations.");
		} catch (Exception ex) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error initializing TaskOperations: ", null, ex);
			throw new RCSStartupException("Error initializing TMM: " + ex, TMM_CONFIG);
		}

		try {
			tmmAgentConfigFile = rcs_config.getFile("agent.config.file", "config/agent.properties");
			bootLog.log(1, CLASS, rcsId, "init", "Located TMM agent configuration file");

		} catch (IOException iox) {
			bootLog.log(FATAL, 1, CLASS, rcsId, "init", "Error configuring TaskOperations/agent config: ", null, iox);
			throw new RCSStartupException("Error locating TMM agent config file: " + iox, TMM_CONFIG);
		}
		/*
		 * try { tmmScheduleConfigFile =
		 * rcs_config.getFile("schedule.config.file",
		 * "config/schedule.properties"); bootLog.log(1, CLASS, rcsId, "init",
		 * "Located TMM schedule config file");
		 * 
		 * } catch (IOException iox) { bootLog.log(FATAL, 1, CLASS, rcsId,
		 * "init", "Error configuring TMM Task_Sequencer/schedule config: ",
		 * null, iox); throw new
		 * RCSStartupException("Error locating TMM schedule config file: " +
		 * iox, TMM_CONFIG); }
		 */
		// --------------------------------------------------
		// Initialize the OLD StateModel.
		// --------------------------------------------------

		/*
		 * stateModel = new ngat.rcs.statemodel.StateModel("RCS_STATE_MODEL");
		 * stateModelUpdateInterval =
		 * rcs_config.getLongValue("state.model.update.interval",
		 * DEFAULT_STATE_MODEL_UPDATE_INTERVAL); try { File smConfigFile =
		 * rcs_config.getFile("state.model.config.file",
		 * "config/state_model.properties"); bootLog.log(1, CLASS, rcsId,
		 * "init", "Located StateModel configuration file"); // TEMP
		 * stateModel.configure(smConfigFile); } catch (Exception e) { throw new
		 * RCSStartupException("Error locating StateModel config file: " + e,
		 * STATE_MODEL_INIT); }
		 */

		// --------------------------------------------------
		// Initialize the NEW StateModel.
		// --------------------------------------------------

		try {

			// Create state-model, with 30 sec cycle time.
			tsm = new StandardStateModel(30000L);

			// Create thing to recieve reactive triggers and pass these onto
			// state-model
			// / as environment change events.
			BasicStateModelReactiveInput tsmri = new BasicStateModelReactiveInput(tsm);

			// TESTSNIP
			// File tsmConfigFile = new
			// File("config/test_state_model.properties");
			// bootLog.log(1, CLASS, rcsId, "init",
			// "Located NEW StateModel configuration file");
			// TESTSNIP
			// tsm.configure(tsmConfigFile);

			// New reactive input mapping from: (top level rule triggers) ->
			// (env change events)
			File tsmriConfigFile = new File("config/tsm_reactive.properties");
			tsmri.configure(tsmriConfigFile);

			bootLog.log(1, CLASS, rcsId, "init", "Binding NEW statemodel...");
			Naming.rebind("rmi://localhost/StateModel", tsm);
			bootLog.log(1, CLASS, rcsId, "init", "NEW StateModel bound");

			// Link reactive trigger handler to the reactive system output
			trs.addReactiveSystemUpdateListener(tsmri);
			bootLog.log(1, CLASS, rcsId, "init", "Linked reactive trigger handler to reactive system output");

			// Link up gateway
			StateModelArchiveGateway smag = new StateModelArchiveGateway(tsm);
			bootLog.log(1, CLASS, rcsId, "init", "Created StateModelArchive gateway and bound to stateModel");

			Naming.rebind("StateModelGateway", smag);
			smag.setProcessInterval(10000L);
			smag.setBackingStoreAgeLimit(10 * 60 * 1000L);
			smag.startProcessor();

			(new Thread(tsm)).start();

		} catch (Exception e) {
			e.printStackTrace();

			throw new RCSStartupException("Error initializing StateModel: " + e, STATE_MODEL_INIT);
		}

		bootLog.log(1, CLASS, rcsId, "init", "Boot sequence completed");

		// ------------------------------------------------------------
		// Left until now so all Loggers have had a chance to be built.
		// ------------------------------------------------------------
		LogManager.configureLoggers(rcs_config);

	} // [Constructor].

	public void emergencyShutdown() {
		System.err.println("...Controller interrupted !");
		bootLog.log(1, CLASS, rcsId, "emergencyShutdown", ".....Controller unexpectedly going down now....");
		terminate(1); // need different code here soas to stop the watchdog...
	}

	/** Carry out default configuration due to missing config info. */
	private void defaultSetup() {

	}

	/**
	 * Access the ThreadRegistry.
	 * 
	 * @uml.property name="threadRegistry"
	 */
	public Map getThreadRegistry() {
		return threadRegistry;
	}

	/** Register a Controllable Thread. */
	public void registerControlThread(String id, ControlableThread thread) {
		threadRegistry.put(id, thread);
	}

	/**
	 * This method is used to put an OPERATOR_NORMAL command on the EventQueue.
	 * When the notification is received, the RCS will leave the FAULT state and
	 * enter either OPERATIONAL or SUSPENDED state.
	 */
	public void invokeOperatorNormal() {
		// EventQueue.postEvent(RCS_ControlTask.OPERATOR_NORMAL_COMMAND);
	}

	/**
	 * This method is used to put an OPERATOR_STANDBY command on the EventQueue.
	 * When the notification is received the RSC will leave the FAULT state and
	 * enter the STANDBY state.
	 */
	public void invokeOperatorStandby() {
		// EventQueue.postEvent(RCS_ControlTask.OPERATOR_STANDBY_COMMAND);
	}

	/**
	 * This method is used to put an OPERATOR_OPERATIONAL command on the
	 * EventQueue. When the notification is received the RCS will leave the
	 * FAULT state and enter the OPERATIONAL state.
	 */
	public void invokeOperatorOperational() {
		// EventQueue.postEvent(RCS_ControlTask.OPERATOR_OPERATIONAL_COMMAND);
	}

	/**
	 * Puts an appropriate command event code onto the EventQueue. When the
	 * notification is received the RCS will make a state transition which will
	 * enable various operations to proceed if not inhibited.
	 * 
	 * @param stateCode
	 *            The code for the state change event trigger.
	 */
	public void invokeStateChangeEvent(String stateCode) {
		EventQueue.postEvent(stateCode);
	}

	/** Close logging, terminate running threads. */
	public void terminate(int code) {
		// beep to terminal
		// Toolkit.getDefaultToolkit().beep();

		bootLog.log(1, CLASS, rcsId, "terminate", "About to terminate threads...");

		Iterator it = threadRegistry.keySet().iterator();
		while (it.hasNext()) {
			String id = (String) it.next();
			ControlableThread thread = (ControlableThread) threadRegistry.get(id);
			bootLog.log(1, CLASS, rcsId, "terminate", "Stopping thread: [" + id + "] ...");

			thread.terminate();
			if (thread instanceof ControlThread)
				((ControlThread) thread).interrupt();
		}

		// smmController.stopAll();

		bootLog.log(1, CLASS, rcsId, "terminate", "Saving instrument calibration history...");
		// Iterator iin = Instruments.findInstrumentSet();
		try {
			List ilist = ireg.listInstruments();
			Iterator iin = ilist.iterator();
			while (iin.hasNext()) {
				InstrumentDescriptor instId = (InstrumentDescriptor) iin.next();

				try {
					InstrumentCalibrationProvider icalp = ireg.getCalibrationProvider(instId);
					InstrumentCalibrationHistory ich = icalp.getCalibrationHistory();
					File instCalibFile = new File("instruments/" + instId.getInstrumentName().toLowerCase() + ".calib.dat");
					InstrumentCalibrationHistory.save(ich, instCalibFile);
					// Instruments.saveCalib(instId);
					bootLog.log(1, CLASS, rcsId, "terminate", "Saved calibration history for: " + instId);
				} catch (Exception iix) {
					bootLog.log(1, CLASS, rcsId, "terminate", "Error saving calibration history for: " + instId + " : " + iix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		bootLog.log(1, CLASS, rcsId, "terminate", "Saving telescope calibration history...");
		try {
			TelescopeCalibrationHistory.save(tch, new File("config/telescope_calib_history.dat"));
			bootLog.log(1, CLASS, rcsId, "terminate", "Saved telescope calibration history");
		} catch (Exception cx) {
			bootLog.log(1, CLASS, rcsId, "terminate", "Error saving calibration history: " + cx);
		}

		bootLog.log(1, CLASS, rcsId, "terminate", "Closing calibration log...");
		try {
			bcalLog.close();
			bootLog.log(1, CLASS, rcsId, "terminate", "Closed calibration log");
		} catch (Exception cx) {
			bootLog.log(1, CLASS, rcsId, "terminate", "Error closing calibration log: " + cx);
		}

		bootLog.log(1, CLASS, rcsId, "terminate", "Closing logging streams...");
		bootLog.log(1, CLASS, rcsId, "terminate", "Controller Stopping on code: " + code);
		bootLog.close();

	}

	// -----------------
	// Global accessors.
	// -----------------

	/** Returns the configuration settings. */
	public static ConfigurationProperties getConfig() {
		return rcs_config;
	}

	/**
	 * Returns the logging directory.
	 * 
	 * @uml.property name="logDir"
	 */
	public static File getLogDir() {
		return logDir;
	}

	/**
	 * Returns the current ObsDate.
	 * 
	 * @uml.property name="obsDate"
	 */
	public static ObsDate getObsDate() {
		return obsDate;
	}

	/**
	 * Returns the Telescope ID.
	 * 
	 * @uml.property name="telescopeId"
	 */
	public String getTelescopeId() {
		return telescopeId;
	}

	/**
	 * Returns the Telescope description.
	 * 
	 * @uml.property name="telescopeDesc"
	 */
	public String getTelescopeDesc() {
		return telescopeDesc;
	}

	/**
	 * Returns the Telescope location description.
	 * 
	 * @uml.property name="telescopeLocation"
	 */
	public String getTelescopeLocation() {
		return telescopeLocation;
	}

	public ISite getSite() {
		return site;
	}

	public AstrometrySiteCalculator getSiteCalculator() {
		return astro;
	}

	/**
	 * Returns the DefaultTrackingMonitor.
	 */
	public DefaultTrackingMonitor getTrackingMonitor() {
		return defaultTrackingMonitor;
	}

	/**
	 * Returns the booking model.
	 */
	public AdvancedBookingModel getBookingModel() {
		return bookingModel;
	}

	/**
	 * Returns the DefaultAutoguiderMonitor.
	 */
	public DefaultAutoguiderMonitor getAutoguiderMonitor() {
		return defaultAutoguiderMonitor;
	}

	/**
	 * Returns the DefaultInstrumentMonitor.
	 */
	public ngat.rcs.scm.detection.DefaultInstrumentMonitor getInstrumentMonitor() {
		return defaultInstrumentMonitor;
	}

	/**
	 * Returns the PMCMonitor.
	 */
	public ngat.rcs.scm.detection.PmcMonitor getPmcMonitor() {
		return pmcMonitor;
	}

	/**
	 * Returns the StateModel.
	 * 
	 * @uml.property name="stateModel"
	 */
	// public ngat.rcs.statemodel.StateModel getStateModel() {
	// return stateModel;
	// }

	// TEMP
	public ngat.rcs.newstatemodel.StandardStateModel getTestStateModel() {
		return tsm;
	}

	/**
	 * Returns the SkyModel.
	 * 
	 * @uml.property name="skyModel"
	 */
	public SkyModel getSkyModel() {
		return skyModel;
	}

	/** Returns a reference to the GOM. */
	public DefaultGroupOperationsMonitor getGroupOperationsMonitor() {
		return gom;
	}

	/** Returns a ref to the task monitor. */
	public BasicTaskMonitor getTaskMonitor() {
		return taskMonitor;
	}

	/**
	 * Returns the TelescopeCalibration.
	 */
	public TelescopeCalibration getTelescopeCalibration() {
		return tcal;
	}

	/**
	 * Returns the TelescopeCalibrationHistory.
	 */
	public TelescopeCalibrationHistory getTelescopeCalibrationHistory() {
		return tch;
	}

	/**
	 * Returns the CalibrationMonitor.
	 */
	public BasicCalibrationMonitor getCalibrationMonitor() {
		return bcalMonitor;
	}

	/**
	 * @return The InstrumentRegistry.
	 */
	public InstrumentRegistry getInstrumentRegistry() {
		return ireg;
	}

	/**
	 * * @return The telescope.
	 */
	public Telescope getTelescope() {
		return telescope;
	}

	/**
	 * Returns the StateModel update interval.
	 * 
	 * @uml.property name="stateModelUpdateInterval"
	 */
	public long getStateModelUpdateInterval() {
		return stateModelUpdateInterval;
	}

	/**
	 * Returns site latitude.
	 * 
	 * @uml.property name="latitude"
	 */
	public static double getLatitude() {
		return latitude;
	}

	public static void setLatitude(double lat) {
		latitude = lat;
	}

	/**
	 * Returns site longitude.
	 * 
	 * @uml.property name="longitude"
	 */
	public static double getLongitude() {
		return longitude;
	}

	public static void setLongitude(double lon) {
		longitude = lon;
	}

	/** Returns the observatory site. */
	public Site getObservatorySite() {
		return obsSite;
	}

	/** Returns dome low limit (rads). */
	public static double getDomelimit() {
		return domeLimit;
	}

	public static void setDomeLimit(double dl) {
		domeLimit = dl;
	}

	/**
	 * Returns true if RCS is operational - able to observe.
	 * 
	 * @uml.property name="operational"
	 */
	public boolean isOperational() {
		return operational;
	}

	/**
	 * @return the automatic setting
	 */
	public boolean isAutomatic() {
		return automatic;
	}

	/**
	 * Sets the operational state of the RCS.
	 * 
	 * @uml.property name="operational"
	 */
	public void setOperational(boolean operational) {
		this.operational = operational;
		System.err.println("***** GOING " + (operational ? "" : "NON-") + "OPERATIONAL *****");

		if (operational) {
			FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_AUTOMATIC);
			// Re-enable forwarding of AG START command by ISS to TCS. ?
			ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(false);

		} else {
			FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_MANUAL);
			// Override forwarding of AG START command by ISS to TCS.
			ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);

		}

	}

	/**
	 * Returns the RCS ID.
	 * 
	 * @uml.property name="rcsId"
	 */
	public String getRcsId() {
		return rcsId;
	}

	/**
	 * Returns the runCounter.
	 * 
	 * @uml.property name="runCounter"
	 */
	public int getRunCounter() {
		return runCounter;
	}

	public long getRunStartTime() {
		return runStartTime;
	}

	/** Returns the StatusMonitor for the given InstrumentID. */
	// public static CCS_StatusMonitorThread getCCSStatusMonitor(String instId)
	// {
	// if (icsStatusMonitors.containsKey(instId))
	// return (CCS_StatusMonitorThread) icsStatusMonitors.get(instId);
	// return null;
	// }

	// ####### TEST STATUS SETUP USING NEW CLASSES.
	public void invokeTestStatus() {

		System.err.println("RCS-SCM OLD SYSTEM FOR LIVE_STATUS UPDATES Starting");
		bootLog.log(1, CLASS, rcsId, "init", "RCS-SCM OLD SYSTEM FOR LIVE_STATUS UPDATES Starting");

		NetworkStatus netstat = new NetworkStatus("X_TCS_COMMS");
		NetworkStatusProvider nsp = new NetworkStatusProvider(netstat);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("X_TCS_COMMS", nsp);

		TCSStatusClient tcsmet = new TCSStatusClient("X_METEO");

		try {
			tcsmet.configure(new File("config/met_sim.properties"));
		} catch (Exception ex) {
			System.err.println("Failed to configure metsim: " + ex);
			return;
		}

		LegacyStatusProviderRegistry.getInstance().addStatusCategory("X_METEO", tcsmet);

		StatusMonitorThread smt = new StatusMonitorThread("X_SM_METEO");

		smt.setPollingInterval(30000L);
		smt.setNetworkStatus(netstat);
		smt.setClient(tcsmet);
		bootLog.log(1, CLASS, rcsId, "init", "Starting Test: StatusMonitorThread: X_SM_METEO");

		smt.start();

		SCM_ControllerX.getController();

		try {
			SCM_ControllerX.getController().configure(new File("config/scm.properties"));
			SCM_ControllerX.getController().startAll();
			System.err.println("*********Starting SCM-X");
			bootLog.log(1, CLASS, rcsId, "init", "Configured and started SCM_ControllerX");
		} catch (Exception ex) {
			bootLog.log(1, CLASS, rcsId, "init", "** WARNING - Problem configuring SCM_ControllerX: " + ex);
		}

	}

}

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
package ngat.rcs.scm;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

/**
 * Control class for the Status Monitoring Module (SMM). Provides general
 * information point and methods for controlling the various status monitor
 * threads.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SCM_ControllerX.java,v 1.1 2006/12/12 08:30:40 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/RCS/SCM_ControllerX.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SCM_ControllerX {

	/** Default polling rate for notifier thread. */
	public static final long DEFAULT_NOTIFICATION_POLLING_INTERVAL = 20000L;

	protected static final String CLASS = "SCM_ControllerX";

	/** Map of StatusMonitorThreads against name. */
	protected Map monitorThreads;

	/** Map of Network monitors against name. */
	protected Map networkMonitors;

	/**
	 * Thread which checks regularly for notification requests by
	 * StatusObservers which have registered via registerObserver().
	 */
	protected Notifier notifier;

	/**
	 * Map of StatusObservers which wish to be notified when a status is
	 * updated.
	 */
	protected Map observers;

	/** Instance. */
	protected static SCM_ControllerX controller;

	Logger logger;

	/** Private constructor. */
	private SCM_ControllerX() {
		monitorThreads = Collections.synchronizedMap(new HashMap());
		networkMonitors = Collections.synchronizedMap(new HashMap());
		observers = Collections.synchronizedMap(new HashMap());
		logger = LogManager.getLogger("TRACE");

		notifier = new Notifier();
	}

	/** Returns the controller instance and/or builds one. */
	public static SCM_ControllerX getController() {
		if (controller == null)
			controller = new SCM_ControllerX();
		return controller;
	}

	/** Configure from file. */
	public void configure(File file) throws IOException,
			IllegalArgumentException {
		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));
		configure(config);
	}

	/** Configure from properties. */
	public void configure(ConfigurationProperties config) throws IOException,
			IllegalArgumentException {
		String key = "";
		String name = "";

		// First get the network resources.

		// network.resource.XX = NET_NAME

		Enumeration e1 = config.propertyNames();
		while (e1.hasMoreElements()) {
			key = (String) e1.nextElement();

			if (key.indexOf("network.resource") != -1) {

				name = config.getProperty(key);

				addNetworkResource(name);

			}
		}

		String className = null;
		Class clazz = null;
		Constructor con = null;

		StatusMonitorClient client = null;

		String configFileName = null;
		File configFile = null;

		boolean enabled = false;
		long polling = 0L;
		long timeout = 0L;
		String desc = "";

		String netResourceId = null;
		NetworkStatus netStatus = null;
		NetworkStatusProvider netProvider = null;

		String logFmtClassName = null;
		Class logFmtClazz = null;

		StatusLogger statusLogger = null;

		String tname = null;

		// Now read each of the Monitor thread params.

		// status.monitor.XX = STAT_NAME (e.g.MOUNTAIN_WEATHER)
		//
		// MOUNTAIN_WEATHER.client.class = ngat.rcs.SomeNetworkClient
		// MOUNTAIN_WEATHER.config.file = config/mountain_weather.properties
		// MOUNTAIN_WEATHER.network.resource.id = MOUNTAIN_WEB
		// MOUNTAIN_WEATHER.enabled = true
		// MOUNTAIN_WEATHER.polling.interval = 30000 (millis)
		// MOUNTAIN_WEATHER.timeout = 50000 (millis)
		// MOUNTAIN_WEATHER.description = The weather from the mountain met
		// station
		// MOUNTAIN_WEATHER.monitor.name = SM_MNT_WEATHER
		// MOUNTAIN_WEATHER.log.output
		// MOUNTAIN_WEATHER.log.formatter.class = ngat.rcs.SomeDataLogger

		Enumeration e2 = config.propertyNames();
		while (e2.hasMoreElements()) {
			key = (String) e2.nextElement();

			// status.monitor.ZZ.ID = METEO_SM
			if (key.indexOf("status.monitor") != -1) {

				name = config.getProperty(key);

				className = config.getProperty(name + ".client.class");

				if (className == null)
					throw new IllegalArgumentException(
							"No client class specified for monitor: " + name);

				try {
					clazz = Class.forName(className);
					con = clazz.getConstructor(new Class[] {});
					client = (StatusMonitorClient) con
							.newInstance(new Object[] {});
				} catch (Exception ex) {
					throw new IllegalArgumentException(
							"Error creating client for monitor: " + name
									+ " : " + ex);
				}

				client.setName(name);

				configFileName = config.getProperty(name + ".config.file");
				if (configFileName == null)
					throw new IllegalArgumentException(
							"No config file specified for monitor: " + name);

				configFile = new File(configFileName);
				client.configure(configFile);

				tname = config.getProperty(name + ".monitor.name", "SMT_"
						+ name);

				StatusMonitorThread monitor = new StatusMonitorThread(tname);

				netResourceId = config.getProperty(name
						+ ".network.resource.id");
				if (!networkMonitors.containsKey(netResourceId))
					throw new IllegalArgumentException(
							"Illegal network resource for monitor: " + name
									+ " : " + netResourceId);
				netProvider = (NetworkStatusProvider) networkMonitors
						.get(netResourceId);
				netStatus = (NetworkStatus) netProvider.getStatus();

				enabled = config.getBooleanValue(name + ".enabled", false);
				polling = config.getLongValue(name + ".polling.interval",
						StatusMonitorThread.DEFAULT_POLLING_INTERVAL);
				desc = config.getProperty(name + ".description");

				// ######## Start DL stuff

				statusLogger = null;

				if (config.getProperty(name + ".log.output") != null) {

					statusLogger = new DefaultStatusLogger(new File("data/"
							+ name));

					logFmtClassName = config.getProperty(name
							+ ".log.formatter.class");

					if (logFmtClassName == null) {

						statusLogger
								.setFormatter(new DefaultStatusLogFormatter());

					} else {

						try {
							logFmtClazz = Class.forName(logFmtClassName);
							con = logFmtClazz.getConstructor(new Class[] {});
							StatusLogFormatter logFmt = (StatusLogFormatter) con
									.newInstance(new Object[] {});
							// Now configure it.
							statusLogger.setFormatter(logFmt);
						} catch (Exception ex) {
							throw new IllegalArgumentException(
									"Error creating status logger for monitor: "
											+ name + " : " + ex);
						}
					}
					// ######## End DL stuff
				}

				addStatusMonitor(name, tname, client, netStatus, desc, enabled,
						polling, statusLogger);

			}
		}

		// Finally get any Notifier parameters.
		long notifyRate = config.getLongValue("notification.polling.rate",
				DEFAULT_NOTIFICATION_POLLING_INTERVAL);
		notifier.setPollingInterval(notifyRate);

	}

	/**
	 * Adds a new network-resource to the table of network resources and
	 * registers as a status category with EMM. If the resource already exists
	 * then returns silently.
	 */
	public NetworkStatusProvider addNetworkResource(String netResourceId) {

		if (networkMonitors.containsKey(netResourceId))
			return null;

		NetworkStatus netStatus = new NetworkStatus(netResourceId);

		NetworkStatusProvider netProvider = new NetworkStatusProvider(netStatus);

		LegacyStatusProviderRegistry.getInstance()
				.addStatusCategory(netResourceId, netProvider);

		networkMonitors.put(netResourceId, netProvider);

		return netProvider;

	}

	/** Add a status-monitor to the table of monitors. */
	public StatusMonitorThread addStatusMonitor(String name,
			String monitorName, StatusMonitorClient client,
			NetworkStatus netStatus, String description, boolean enabled,
			long pollingInterval, StatusLogger statusLogger) {

		StatusMonitorThread monitor = new StatusMonitorThread(monitorName);
		monitor.setClient(client);
		monitor.setNetworkStatus(netStatus);
		monitor.setDescription(description);
		monitor.setEnabled(enabled);
		monitor.setPollingInterval(pollingInterval);
		monitor.setStatusLogger(statusLogger);

		monitorThreads.put(name, monitor);

		LegacyStatusProviderRegistry.getInstance().addStatusCategory(name, client);

		// logger.log(1, "SCM", "-", "addStatusMonitor",
		System.err.println("SCM:: Adding monitor: " + name + "/" + monitorName
				+ " at " + pollingInterval + "ms");

		return monitor;

	}

	/** Start the named monitor thread. */
	public void start(String id) {
		StatusMonitorThread monitor = (StatusMonitorThread) monitorThreads
				.get(id);
		if (monitor == null)
			return;
		if (monitor.isEnabled())
			monitor.start();
		// logger.log(1, CLASS, "-", "init",
		// "TCS_["+id+"]_MonitorThread started: Updating at: "+
		// (monitor.getInterval()/1000)+" secs.");
		// logger.log(1, "SCM", "-", "start",
		System.err.println("SCM:: Starting monitor: " + monitor);
	}

	/** Start ALL monitor threads. */
	public void startAll() {
		Iterator it = monitorThreads.keySet().iterator();
		while (it.hasNext()) {
			start((String) it.next());
		}
	}

	/** Stop the named monitor thread. */
	public void stop(String id) {
		StatusMonitorThread monitor = (StatusMonitorThread) monitorThreads
				.get(id);
		if (monitor == null)
			return;
		monitor.terminate();
	}

	/** Stop ALL monitor threads. */
	public void stopAll() {
		Iterator it = monitorThreads.keySet().iterator();
		while (it.hasNext()) {
			stop((String) it.next());
		}
	}

	/** Pause the named monitor thread. */
	public void pause(String id) {
		StatusMonitorThread monitor = (StatusMonitorThread) monitorThreads
				.get(id);
		if (monitor == null)
			return;
		monitor.linger();
	}

	/** Pause ALL monitor threads. */
	public void pauseAll() {
		Iterator it = monitorThreads.keySet().iterator();
		while (it.hasNext()) {
			pause((String) it.next());
		}
	}

	/** Resume the named monitor thread. */
	public void resume(String id) {
		StatusMonitorThread monitor = (StatusMonitorThread) monitorThreads
				.get(id);
		if (monitor == null)
			return;
		monitor.awaken();
	}

	/** Resume ALL monitor threads. */
	public void resumeAll() {
		Iterator it = monitorThreads.keySet().iterator();
		while (it.hasNext()) {
			resume((String) it.next());
		}
	}

	/**
	 * Returns the StatusMonitorThread registered against the specified name.
	 * 
	 * @param id
	 *            The monitor name.
	 * @return An existing StatusMonitorThread registered against id.
	 */
	public StatusMonitorThread getMonitor(String id) {
		if (monitorThreads.containsKey(id))
			return (StatusMonitorThread) monitorThreads.get(id);
		return null;
	}

	/**
	 * Returns the StatusMonitorThread registered against the specified (SHOW)
	 * key. If no such monitor is currently registered returns null.
	 * 
	 * @param key
	 *            The (SHOW) key to register against.
	 * @return An existing or new StatusMonitor registered against key.
	 */
	public static StatusMonitorThread findMonitor(String id) {
		return controller.getMonitor(id);
	}

	// public void registerObserver(String cat, StatusObserver observer) {
	// // add this chap to list of observers of cat
	// }

	/** Status update notification thread. */
	class Notifier extends ControlThread {

		/** Polling rate (millis). */
		private long pollingInterval;

		/** Create a Notifier. */
		Notifier() {
			super("NOTIFIER", true);
		}

		@Override
		public void initialise() {
			// log starting SCMX notifier.
		}

		@Override
		public void mainTask() {

			try {
				Thread.sleep(pollingInterval);
			} catch (InterruptedException ix) {
			}

			System.err
					.println("** Warning: SCM-Notifier polling - Not currently operational");
			// Check each notificant/observer

			// Iterator obs = observers.keySet().iterator();
			// while (obs.hasNext()) {

			// String cat = (String)obs.next();

			// StatusMonitorThread monitor =
			// (StatusMonitorThread)monitorThreads.get(cat);

			// if (monitor != null) {

			// if (monitor.updated()) {

			// List obsList = (list)observers.get(cat);

			// Iterator ol = obsList.iterator();
			// while (ol.hasNext()) {

			// StatusObserver observer = (StatusObserver)ol.next();

			// }

		}

		@Override
		public void shutdown() {
		}

		/** Set the polling interval (millis). */
		public void setPollingInterval(long pollingInterval) {
			this.pollingInterval = pollingInterval;
		}

	}

}

/**
 * $Log: SCM_ControllerX.java,v $ /** Revision 1.1 2006/12/12 08:30:40 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:34:47 snf /** Initial
 * revision /**
 */

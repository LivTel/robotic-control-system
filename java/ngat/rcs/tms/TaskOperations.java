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
package ngat.rcs.tms;

import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.text.*;
import java.util.*;
import java.rmi.server.*;

import ngat.rcs.*;
import ngat.rcs.comms.*;
import ngat.rcs.emm.*;
import ngat.rcs.ops.OperationsManager;
import ngat.rcs.tms.manager.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * This class is responsible for selecting the appropriate MCA to take control
 * of the telescope at any time. The Background controller (BCA) is the default.
 * In addition it is responsible for loading the schedule file and the schedule
 * history file and provides methods to update and extract history and schedule:
 * information. schedule-file : Contains a series of MCA-specific time slots for
 * MCAs which use this type of scheduling. history-file : Contains a set of
 * history events relating to the successful or otherwise completion/attempt of
 * the schedule windows by their nominated MCAs. The TMM is initialized using:
 * TMM.initialize() which creates the instance. The history file is next loaded:
 * configureHistory(file). The schedule is loaded: configureSchedule(file).
 * Finally the agents (MCA)s are initialized: configureAgents(file). 
 */
public class TaskOperations implements TaskModeManagement {

	/** Used to parse history file data. */
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	/** Used for presentation purposes. */
	public static SimpleDateFormat odf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static final int MAX_PRIORITY = 20;

	/** Contains a sorted list of MCAs which operate in nighttime. */
	protected Map execPriority;

	/** Contains a list of ALL MCAs. */
	protected Map agents;

	public static final String CLASS = "TMM_TaskSequencer";

	/** TMM Logging. */
	protected Logger tmmLog;

	/** The nominated Default agent to be in control when no-one else is. */
	protected ModalTask defaultAgent;

	/** Singleton instance. */
	private static TaskOperations instance;

	/** Time next MCA will want control. */
	private long nextMacTime;

	/** Operations manager. */
	private OperationsManager operationsManager;

	/** Private instance constructor. */
	private TaskOperations() {

		sdf.setTimeZone(RCS_Controller.UTC);
		odf.setTimeZone(RCS_Controller.UTC);

		execPriority = Collections.synchronizedMap(new TreeMap());
		agents = Collections.synchronizedMap(new TreeMap());

		tmmLog = LogManager.getLogger("TMM");

	}

	/** Create the singleton instance. */
	public static void initialize() throws Exception {

		if (instance == null)
			instance = new TaskOperations();

		Thread current = Thread.currentThread();
		System.err.println("TMM_Init:: Current Thread: " + current + " State: "
				+ (current.isDaemon() ? "DAEMON" : "NON_DAEMON"));

		//BasicExecTimingModel execModel = new BasicExecTimingModel();
		//Naming.rebind("rmi://localhost/ExecTimingModel", execModel);

		//BasicAvailabilityModel availModel = new BasicAvailabilityModel();
		//Naming.rebind("rmi://localhost/AvailabilityModel", availModel);

		// instance.createModeManager();
		// Export server and bind
		UnicastRemoteObject.exportObject(instance);
		// Naming.rebind("rmi://localhost/ModeManager",
		// instance.getModeManager());
		Naming.rebind("rmi://localhost/ModeManager", instance);

		// why are these setup here ? 
		RCS_SubsystemConnectionFactory.addSocketResource("BSS", "localhost", 6683);
		RCS_SubsystemConnectionFactory.addSocketResource("ISS", "localhost", 7383);
		
	}

	/**
	 * Returns the singleton instance.
	 * 
	 * @uml.property name="instance"
	 */
	public static TaskOperations getInstance() {
		return instance;
	}

	/** Return an iterator over the set of Mode controllers. */
	public List listModeControllers() throws RemoteException {
		List list = new Vector();
		Iterator ic = agents.values().iterator();
		while (ic.hasNext()) {
			list.add(ic.next());
		}
		return list;
	}

	/** Returns a Mode controller identified by name or NULL. */
	public TaskModeController getModeController(String name) throws RemoteException {
		return (TaskModeController) getAgent(name);
	}

	/**
	 * Configure from files.
	 * 
	 * @param configFile
	 *            The TMM configuration file.
	 */
	public void configureAgents(File configFile) throws IOException, IllegalArgumentException {
		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(configFile));
		configureAgents(config);
	}

	/** Configure from properties. */
	public void configureAgents(ConfigurationProperties config) throws IOException, IllegalArgumentException {

		String key = null;
		String agentKey = null;

		String agentId = null;
		String agentVersion = null;
		String agentDesc = null;
		String agentClassName = null;

		boolean agentIsDefault = false;

		String agentConfigFileName = null;

		File agentConfigFile = null;

		int agentPriority = 0;

		boolean mandatory = false;

		//RCS_ControlTask controlAgent = RCS_ControlTask.getInstance();

		// Setup OpsMgr..
		operationsManager = new OperationsManager("OCM");
		UnicastRemoteObject.exportObject(operationsManager);

		Naming.rebind("rmi://localhost/OperationsManager", operationsManager);
		tmmLog.log(1, CLASS, "TMM_Sequencer", "configureAgents", "Exported and bound OpsManager: " + operationsManager);

		
		
		Enumeration e = config.propertyNames();
		while (e.hasMoreElements()) {

			key = (String) e.nextElement();

			if (key.endsWith(".ID")) {

				agentKey = key.substring(0, key.lastIndexOf("."));

				agentId = config.getProperty(key);
				agentVersion = config.getProperty(agentKey + ".version", "UNKNOWN");
				agentDesc = config.getProperty(agentKey + ".desc", agentId);

				agentClassName = config.getProperty(agentKey + ".class");

				if (agentClassName == null)
					throw new IllegalArgumentException("Agent: " + agentId + " No class specified");
				agentConfigFileName = config.getProperty(agentKey + ".config.file");

				if (agentConfigFileName == null)
					throw new IllegalArgumentException("Agent: " + agentId + " No config file specified");

				agentConfigFile = new File(agentConfigFileName);
				if (!agentConfigFile.exists())
					throw new IllegalArgumentException("Agent: " + agentId + " Not a valid config file: "
							+ agentConfigFileName);

				agentPriority = config.getIntValue(agentKey + ".night.priority", -1);

				agentIsDefault = (config.getProperty(agentKey + ".default") != null);

				mandatory = (config.getProperty(agentKey + ".mandatory") != null);

				// Try to instantiate the Agent class.
				ModalTask agentImpl = null;

				try {
					Class agentClass = Class.forName(agentClassName);

					// Make a constuctor.
					Constructor agentCon = agentClass.getConstructor(new Class[] { String.class, TaskManager.class });

					// Make an Instance of the Class.
					// agentImpl = (ModalTask)agentCon.newInstance(new
					// Object[]{agentId , controlAgent});
					agentImpl = (ModalTask) agentCon.newInstance(new Object[] { agentId, operationsManager });
					System.err.println("Built Agent: " + agentImpl);
				} catch (Exception ex) {
					System.err.println("Error building agent: " + agentClassName);
					ex.printStackTrace();
					throw new IllegalArgumentException("Unable to construct control agent: " + agentId + " : " + ex);
				}

			
				// Initialize it - Note we are actually setting a static
				// reference..
				// We should never need to call this externally - if we can use
				// the new:
				// ModalTask at = ca.getModalControlAgent(agId) or
				// equivelantly..
				// ModalTask at = TMM.getAgent(agId); - rather than the old:
				// ModalTask at = (SomeAgentClass)SomeAgentClass.getInstance();
				// which is very NAFFF !
				agentImpl.initialize(agentImpl);

				agentImpl.setAgentId(agentId);
				agentImpl.setAgentVersion(agentVersion);
				agentImpl.setAgentDesc(agentDesc);

				// Try to configure it from its config file.
				// If it fails we either re-throw exception or disable the
				// agent.
				try {
					agentImpl.configure(agentConfigFile);
				} catch (IllegalArgumentException iax) {
					iax.printStackTrace();
					if (mandatory) {
						throw iax;
					} else {
						tmmLog.log(1, CLASS, "TMM_Sequencer", "configureAgents", "** WARNING - Agent: " + agentId
								+ " did not configure and is unavailable");
					}
				}

				// Set a reference so we can access the instance globally via CA
				// as well as via TMM.
				// ### Should we do this if it has failed to configure ?
				//RCS_ControlTask.addModalControlAgent(agentId, agentImpl);

				// Set up priorities for this MCA - we hope there arn't 2 the
				// same !
				if (agentPriority >= 0)
					execPriority.put(new Integer(agentPriority), agentImpl);

				agentImpl.setAgentPriority(agentPriority);

				// Record this agent anyway.
				agents.put(agentId, agentImpl);

				if (agentIsDefault)
					defaultAgent = agentImpl;

				// add mode management hooks
				if (agentImpl instanceof DefaultModalTask) {
					try {
						DefaultModalTask dma = (DefaultModalTask) agentImpl;
						// rmi export
						UnicastRemoteObject.exportObject(dma);
						// Bind this controller
						Naming.rebind("rmi://localhost/" + agentId + "ModeController", dma);

					} catch (Exception ce) {
						throw new IllegalArgumentException("Error binding ModeController: " + ce);
					}
				}

			}

		}

		// operationsManager = new OperationsManager();
		// UnicastRemoteObject.exportObject(operationsManager);

		Naming.rebind("rmi://localhost/OperationsManager", operationsManager);
		tmmLog.log(1, CLASS, "TMM_Sequencer", "configureAgents", "Exported and bound OpsManager: " + operationsManager);

		// start the OPS Mgr
		operationsManager.start();

	}

	/**
	 * Returns the next available MCA - i.e. the highest priority MCA which can
	 * be run at this time.
	 */
	public synchronized Task nextModal() {
		ModalTask task = null;
		tmmLog.log(3, CLASS, "TMM_Sequencer", "nextModal", "Requesting new Modal from set of " + execPriority.size()
				+ " available MCAs");

		ModalTask mac = null;
		ModalTask nextMac = null;

		long now = System.currentTimeMillis();
		long macTime = now + 24 * 3600 * 1000L;
		nextMacTime = now + 24 * 3600 * 1000L;
		;

		Integer ii = null;

		for (int i = 0; i < MAX_PRIORITY; i++) {

			tmmLog.log(1, CLASS, "TMM_Sequencer", "nextModal", " Checking for an MCA with priority: " + i);

			ii = new Integer(i);

			if (execPriority.containsKey(ii)) {

				mac = (ModalTask) execPriority.get(ii);
				tmmLog
						.log(1, CLASS, "TMM_Sequencer", "nextModal", "Testing control acceptance by: "
								+ mac.getAgentId());

				if (mac.acceptControl()) {

					tmmLog.log(2, CLASS, "TMM_Sequencer", "nextModal", "Control Accepted by: " + mac.getAgentId()
							+ ", Pre-empt By: "
							+ (nextMac != null ? nextMac.getName() + odf.format(new Date(nextMacTime)) : "NOBODY")
							+ " in " + ((nextMacTime - now) / 1000) + " secs");
					return mac;
				} else {

					macTime = mac.demandControlAt();

					if (macTime < nextMacTime) {
						nextMacTime = macTime;
						nextMac = mac;
					}

					tmmLog.log(2, CLASS, "TMM_Sequencer", "nextModal", "Control Refused by: " + mac.getAgentId()
							+ " Due to: " + mac.getNonAcceptanceReason() + " Next session: "
							+ odf.format(new Date(macTime)) + " in " + ((macTime - now) / 1000) + " secs");

				}

			} // no MCA at this level

		} // next priority level

		if (defaultAgent != null) {
			tmmLog.log(2, CLASS, "TMM_Sequencer", "nextModal", "Using default agent: " + defaultAgent.getName());
			return defaultAgent;
		}

		// no default agent !
		tmmLog.log(2, CLASS, "TMM_Sequencer", "nextModal", "Control was Accepted by Default by: BACKGROUND_OPS");
		return BackgroundControlAgent.getInstance();

	}

	/**
	 * Returns the time at which the next MCA, higher in priority than the
	 * current MCA which will want control. Ths time can be used to set a limit
	 * on the execution of the current MCA when it is started.
	 * 
	 * @param mca
	 *            The MCA for which any higher priority MCAs are to be found.
	 */
	public synchronized long futureModal(ModalTask mca) {

		return nextMacTime;

	}

	/** Returns the agent identified by agentId or null. */
	public ModalTask getAgent(String agentId) {
		if (agents.containsKey(agentId))
			return (ModalTask) agents.get(agentId);
		return null;
	}

	/** Returns an Iterator over the list of current MCAs. */
	public Iterator listAgents() {
		return agents.values().iterator();
	}

	public OperationsManager getOperationsManager() {
		return operationsManager;
	}

	/** Monitors events. */
	static class EventMonitor implements EventSubscriber {

		EventMonitor() {
		}

		/**
		 * EventSubscriber method.
		 */
		public void notifyEvent(String eventCode, Object data) {
		}

		/**
		 * EventSubscriber method.
		 */
		public String getSubscriberId() {
			return "TMM_EventMonitor";
		}

	}

}

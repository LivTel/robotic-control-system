package ngat.rcs.newstatemodel;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

import ngat.util.*;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.SolarCalculator;
import ngat.rcs.RCS_Controller;
import ngat.rcs.newstatemodel.test.FireEnvironmentChangeEvent;
import ngat.rcs.scm.detection.*;
import ngat.rcs.emm.*;
import ngat.rcs.ers.ReactiveSystemMonitor;
import ngat.rcs.ers.ReactiveSystemUpdateListener;

public class StandardStateModel extends UnicastRemoteObject implements
		StateModel, ControlActionResponseHandler, Runnable, EventSubscriber,
		PmcStatusListener {

	public static final int INIT_STATE = 1;

	public static final int STANDBY_STATE = 2;

	public static final int STARTING_STATE = 3;

	public static final int OPENING_STATE = 4;

	public static final int OPERATIONAL_STATE = 5;

	public static final int CLOSING_STATE = 6;

	public static final int STOPPING_STATE = 7;

	public static final int SHUTDOWN_STATE = 8;

	List cals;

	List cais;

	List scls;

	/** The current state of the model. */
	TestState state;

	int intState;

	int system = EnvironmentChangeEvent.SYSTEM_SUSPEND;

	int axes = EnvironmentChangeEvent.AXES_ERROR;

	int weather = EnvironmentChangeEvent.WEATHER_ALERT;

	int enclosure = EnvironmentChangeEvent.ENCLOSURE_ERROR;

	int control = EnvironmentChangeEvent.CONTROL_DISABLED;

	int network = EnvironmentChangeEvent.NETWORK_ALERT;

	// Will be filled by RCS COntrol on startup.
	int intent;

	// Will be filled by RCS COntrol on startup.
	int tod;

	int mirrcover = EnvironmentChangeEvent.MIRR_COVER_ERROR;

	int run = EnvironmentChangeEvent.OP_RUN;

	long poll;

	BooleanLock signal;

	Map events;

	/** List of Variables indexed by Name. */
	protected Map variables;

	Timer timer = new Timer("TRANSIENT_TIMER");

	// TODO A bit strange, the way this is done........

	/** int to determine the stability state. */
	private volatile int stability = 0; // 0=OK, 1=BAD

	/**
	 * Description of stability criterion - used as abort action reason.
	 * Defaults to STABLE (0) but can be changed to PMC_CLOSED (1) if the pmc is
	 * seen to close.
	 */
	private String stabilityCriterion = "STABLE";

	// protected DefaultTrackingMonitor tm;

	// protected DefaultAutoguiderMonitor am;

	protected PmcMonitor pm;

	AstrometrySiteCalculator astro;

	/** Count cycles. */
	private int cycleNumber = 0;

	/** Logger. */
	private LogGenerator logger;

	/**
     * 
     */
	public StandardStateModel(long poll) throws RemoteException {

		super();

		this.poll = poll;
		cals = new Vector();
		cais = new Vector();
		scls = new Vector();
		events = new HashMap();
		variables = new HashMap();
		state = new TestState(INIT_STATE, "INIT");
		intState = INIT_STATE;
		signal = new BooleanLock(true);

		Logger alogger = LogManager.getLogger("OPS");
		logger = alogger.generate().system("RCS").subSystem("OPS")
				.srcCompClass(this.getClass().getSimpleName())
				.srcCompId("StateModel");

	}

	public void configure(File file) throws Exception {

		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));

		File topicFile = new File(config.getProperty("state.model.topic.file"));
		configureTopics(topicFile);

		File variableFile = new File(
				config.getProperty("state.model.variable.file"));
		configureVariable(variableFile);

	}

	public void configureTopics(File file) throws Exception {

		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));

		Enumeration e = config.propertyNames();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();
			int value = config.getIntValue(name);
			System.err.println("NSM::NSM:: Add env signal: " + name
					+ " as code " + value);
			events.put(name, new Integer(value));
			EventRegistry.subscribe(name, this);
		}
	}

	public void configureVariable(File file) throws Exception {

		BufferedReader in = new BufferedReader(new FileReader(file));

		Vector stateVarList = new Vector();
		Vector boolVarList = new Vector();
		Vector recordVarList = new Vector();

		// Scanning phase.
		String line = null;
		Vector lineList = new Vector();
		while ((line = in.readLine()) != null) {
			// System.err.println("Read:"+line);
			line = line.trim();
			// Skip blank or comment lines.
			if ((line != "") && !(line.startsWith("#")))
				lineList.add(line);
		}

		// Variable collection phase.
		StringTokenizer tokenz = null;
		Iterator lines = lineList.iterator();
		while (lines.hasNext()) {
			line = (String) lines.next();
			tokenz = new StringTokenizer(line);
			if (line.startsWith("VAR_STATE")) {
				if (tokenz.countTokens() == 2) {
					tokenz.nextToken();
					String name = tokenz.nextToken();
					StateVariable var = new StateVariable(name);
					System.err.println("NSM::Creating StateVar: " + name);
					stateVarList.add(var);
					variables.put(name, var);
				}
			}
			if (line.startsWith("VAR_BOOL")) {
				if (tokenz.countTokens() == 2) {
					tokenz.nextToken();
					String name = tokenz.nextToken();
					BooleanVariable var = new BooleanVariable(name);
					System.err.println("NSM::Creating BoolVar: " + name);
					boolVarList.add(var);
					variables.put(name, var);
				}
			}
			if (line.startsWith("VAR_RECORD")) {
				if (tokenz.countTokens() == 2) {
					tokenz.nextToken();
					String name = tokenz.nextToken();
					RecordVariable var = new RecordVariable(name);
					System.err.println("NSM::Creating RecordVar: " + name
							+ " Set to ALERT");
					recordVarList.add(var);
					variables.put(name, var);

					// / Set to ALERT to start with.
					var.currentState = 1;
				}
			}
		}

		// Search for StateVar constants.
		Iterator vars = stateVarList.iterator();
		while (vars.hasNext()) {
			StateVariable var = (StateVariable) vars.next();
			tokenz = null;
			lines = lineList.iterator();
			while (lines.hasNext()) {
				line = (String) lines.next();
				tokenz = new StringTokenizer(line);
				if (line.startsWith(var.name)) {
					if (tokenz.countTokens() == 4) {
						tokenz.nextToken();
						String act = tokenz.nextToken();
						String stt = tokenz.nextToken();
						String con = tokenz.nextToken();
						try {
							int ist = Integer.parseInt(stt);
							var.addState(ist, con);
							var.addTriggerEvent(act, con);
							System.err.println("NSM::Added State: #" + ist
									+ " Trigger: " + act + " Const: " + con
									+ " To: " + var.name);
						} catch (NumberFormatException nx) {
							throw new IllegalArgumentException("Var: "
									+ var.name + " Illegal format: " + nx);
						}
					}
				}
			}
		}

		// Search for BoolVar activators.
		vars = boolVarList.iterator();
		while (vars.hasNext()) {
			BooleanVariable var = (BooleanVariable) vars.next();
			tokenz = null;
			lines = lineList.iterator();
			while (lines.hasNext()) {
				line = (String) lines.next();
				tokenz = new StringTokenizer(line);
				if (line.startsWith(var.name)) {
					if (tokenz.countTokens() == 4) {
						tokenz.nextToken();
						String act = tokenz.nextToken();
						String typ = tokenz.nextToken();
						String con = tokenz.nextToken();
						if (typ.equals("SET")) {
							var.setSetConst(con);
							var.addSetTrigger(act);
							System.err.println("NSM::Added Set Trigger: " + act
									+ " Const: " + con + " To: " + var.name);
						}
						if (typ.equals("CLEAR")) {
							var.setClearConst(con);
							var.addClearTrigger(act);
							System.err.println("NSM::Added Clear Trigger: "
									+ act + " Const: " + con + " To: "
									+ var.name);
						}
					}
				}
			}
		}

		// Search for RecordVar alerts and clears.
		vars = recordVarList.iterator();
		while (vars.hasNext()) {
			RecordVariable var = (RecordVariable) vars.next();
			tokenz = null;
			lines = lineList.iterator();
			while (lines.hasNext()) {
				line = (String) lines.next();
				tokenz = new StringTokenizer(line);
				if (line.startsWith(var.name)) {
					if (tokenz.countTokens() == 3) {
						tokenz.nextToken();
						tokenz.nextToken();
						// ALERT ?
						String alt = tokenz.nextToken();
						var.addAlertTrigger(alt);
						System.err.println("NSM::Added ALERT: " + alt
								+ " For RecordVar: " + var.name);
					}
					if (tokenz.countTokens() == 5) {
						tokenz.nextToken();
						tokenz.nextToken();
						// CLEAR ?
						String clr = tokenz.nextToken();
						tokenz.nextToken();
						String can = tokenz.nextToken();
						var.addClearTrigger(clr, can);
						System.err.println("NSM::Added CLEAR: " + clr
								+ " cancels alert: " + can + " For RecordVar: "
								+ var.name);
					}
				}
			}
		}

		System.err.println("NSM::State Variable List.....");
		vars = stateVarList.iterator();
		while (vars.hasNext()) {
			StateVariable var = (StateVariable) vars.next();
			System.err.println(var.toString());
		}
		System.err.println("NSM::Boolean Variable List.....");
		vars = boolVarList.iterator();
		while (vars.hasNext()) {
			BooleanVariable var = (BooleanVariable) vars.next();
			System.err.println(var.toString());
		}
		System.err.println("NSM::Record Variable List.....");
		vars = recordVarList.iterator();
		while (vars.hasNext()) {
			RecordVariable var = (RecordVariable) vars.next();
			System.err.println(var.toString());
		}

		try {
			in.close();
		} catch (Exception e) {
			System.err.println("NSM::Error closing file: " + e);
		}

		in = null;

		// Dispose of resources.
		stateVarList.clear();
		stateVarList = null;
		boolVarList.clear();
		boolVarList = null;
		recordVarList.clear();
		recordVarList = null;

		// Enumeration e = config.propertyNames();
		// while (e.hasMoreElements()) {
		// String name = (String)e.nextElement();
		// int value = config.getIntValue(name);
		// System.err.println("NSM::NSM:: Add env signal: "+name+" as code "+value);
		// events.put(name, new Integer(value));
		// EventRegistry.subscribe(name, this);
		// }

	}

	public Map getStateInfo() {

		Map map = new HashMap();

		// Iterator iv = variables.keySet().iterator();
		// while (iv.hasNext()) {
		// String varname = (String)iv.next();
		// Variable var = (Variable)variables.get(varname);

		// map.put(varname, var.toCodeString());
		// }

		// return map;

		map.put("SYSTEM", new Integer(system));
		map.put("AXES", new Integer(axes));
		map.put("WEATHER", new Integer(weather));
		map.put("ENCLOSURE", new Integer(enclosure));
		map.put("CONTROL", new Integer(control));
		map.put("NETWORK", new Integer(network));
		map.put("INTENT", new Integer(intent));
		map.put("PERIOD", new Integer(tod));
		map.put("SHUTDOWN", new Integer(run));
		map.put("MIRRCOVER", new Integer(mirrcover));
		map.put("STABILITY", new Integer(stability));

		return map;
	}

	public void addControlActionListener(ControlActionListener cal)
			throws RemoteException {
		if (cals.contains(cal))
			return;
		cals.add(cal);
	}

	public void removeControlActionListener(ControlActionListener cal)
			throws RemoteException {
		if (!cals.contains(cal))
			return;
		cals.remove(cal);
	}

	public void addControlActionImplementor(ControlActionImplementor cal)
			throws RemoteException {
		if (cais.contains(cal))
			return;
		cais.add(cal);
		System.err.println("NSM::SM: Added CAI:" + cal);
	}

	public void removeControlActionImplementor(ControlActionImplementor cal)
			throws RemoteException {
		if (!cais.contains(cal))
			return;
		cais.remove(cal);
	}

	/**
	 * This is where all the work is done...
	 * 
	 */
	public void environmentChanged(EnvironmentChangeEvent cev)
			throws RemoteException {

		// First set the variables based on received event type - this is crude,
		// should use subclasses of the event
		// which carry additional information.

		logger.create().info().level(2).extractCallInfo()
				.msg("EnvironmentChanged() : " + cev).send();

		int type = cev.getType();
		switch (type) {
		case EnvironmentChangeEvent.SYSTEM_OKAY:
		case EnvironmentChangeEvent.SYSTEM_STANDBY:
		case EnvironmentChangeEvent.SYSTEM_SUSPEND:
		case EnvironmentChangeEvent.SYSTEM_FAIL:
			system = type;
			break;
		case EnvironmentChangeEvent.WEATHER_ALERT:
		case EnvironmentChangeEvent.WEATHER_CLEAR:
			weather = type;
			break;
		case EnvironmentChangeEvent.CONTROL_DISABLED:
		case EnvironmentChangeEvent.CONTROL_ENABLED:
			control = type;
			break;
		case EnvironmentChangeEvent.AXES_ERROR:
		case EnvironmentChangeEvent.AXES_OKAY:
			axes = type;
			break;
		case EnvironmentChangeEvent.ENCLOSURE_CLOSED:
		case EnvironmentChangeEvent.ENCLOSURE_ERROR:
		case EnvironmentChangeEvent.ENCLOSURE_OPEN:
			enclosure = type;
			break;
		case EnvironmentChangeEvent.NETWORK_ALERT:
		case EnvironmentChangeEvent.NETWORK_OKAY:
			network = type;
			break;
		case EnvironmentChangeEvent.INTENT_OPERATIONAL:
		case EnvironmentChangeEvent.INTENT_ENGINEERING:
			intent = type;
			break;
		case EnvironmentChangeEvent.DAY_TIME:
		case EnvironmentChangeEvent.NIGHT_TIME:
			tod = type;
			break;
		case EnvironmentChangeEvent.OP_RESTART_ENG:
		case EnvironmentChangeEvent.OP_RESTART_AUTO:
		case EnvironmentChangeEvent.OP_REBOOT:
		case EnvironmentChangeEvent.OP_RESTART_INSTR:
		case EnvironmentChangeEvent.OP_RUN:
			run = type;
			break;
		case EnvironmentChangeEvent.MIRR_COVER_CLOSED:
		case EnvironmentChangeEvent.MIRR_COVER_ERROR:
		case EnvironmentChangeEvent.MIRR_COVER_OPEN:
			mirrcover = type;
			break;

		default:
			System.err.println("NSM::Unknown event type received: " + cev);
			break;
		}

		// release the signal and let state-model advance
		signal.setValue(true);

	}

	public IState getState() {
		return state;
	}

	/** Loop, wait on incoming signal, check for transitions.. */
	public void run() {

		while (true) {

			checkTransitions();

			try {
				signal.waitUntilTrue(poll);
			} catch (InterruptedException ix) {
				logger.create().warn().level(2).extractCallInfo()
						.msg("Loop timeout").send();
			}
			signal.setValue(false);
		}

	}

	/**
	 * Here we check to see which transitions are enabled, select one and fire
	 * it, only one trans per loop.
	 */
	private void checkTransitions() {

		// System.err.println("NSM::CheckTrans: CurrState="+state+" Timer elapsed: "+(timer.elapsed()/1000)+"s");

		cycleNumber++;

		// FIRST check the sun position
		long now = System.currentTimeMillis();
		boolean sunset = false;
		try {
			astro = RCS_Controller.controller.getSiteCalculator();
			SolarCalculator sol = new SolarCalculator();
			double sunlev = astro.getAltitude(sol.getCoordinates(now), now);
			sunset = (sunlev < 0.0);
		} catch (Exception ex) {
			// this can sometimes occur during the first few cycles at startup
			// where the astro calculator
			// has not yet been instantiated, ideally it should have been !

			logger.create()
					.warn()
					.level(2)
					.extractCallInfo()
					.msg("Cycle: " + cycleNumber
							+ " currently unable to access astro calculator: "
							+ ex.getMessage());
		}

		switch (state.getState()) {

		case INIT_STATE:
			// -----------
			// ENGINEERING
			// -----------

			if (intent == EnvironmentChangeEvent.INTENT_OPERATIONAL) {
				switchState(STANDBY_STATE);
			}

			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			break;

		case STANDBY_STATE:
			// -------
			// STANDBY
			// -------

			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				switchState(INIT_STATE);
			}

			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			// starting-up
			if (system == EnvironmentChangeEvent.SYSTEM_STANDBY
					&& control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& tod == EnvironmentChangeEvent.NIGHT_TIME) {
				timer.start();
				notifyListeners(new ControlAction(ControlAction.STARTUP_ACTION));
				switchState(STARTING_STATE);
			}

			// opening
			if (system == EnvironmentChangeEvent.SYSTEM_OKAY
					&& control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& tod == EnvironmentChangeEvent.NIGHT_TIME
					&& sunset == true
					&& weather == EnvironmentChangeEvent.WEATHER_CLEAR
					&& enclosure != EnvironmentChangeEvent.ENCLOSURE_OPEN) {
				timer.start();
				notifyListeners(new ControlAction(ControlAction.OPEN_ACTION));
				switchState(OPENING_STATE);
			}

			// closing for weather
			if (control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& weather == EnvironmentChangeEvent.WEATHER_ALERT
					&& enclosure != EnvironmentChangeEvent.ENCLOSURE_CLOSED) {
				timer.start();
				notifyListeners(new ControlAction(ControlAction.CLOSE_ACTION));
				switchState(CLOSING_STATE);
			}

			// closing for daytime
			if (control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& tod == EnvironmentChangeEvent.DAY_TIME
					&& enclosure != EnvironmentChangeEvent.ENCLOSURE_CLOSED) {
				timer.start();
				notifyListeners(new ControlAction(ControlAction.CLOSE_ACTION));
				switchState(CLOSING_STATE);
			}

			// stopping-down for daytime and enclosure is closed and axes are
			// not offline
			if (control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& tod == EnvironmentChangeEvent.DAY_TIME
					&& enclosure == EnvironmentChangeEvent.ENCLOSURE_CLOSED
					&& axes != EnvironmentChangeEvent.AXES_ERROR) {
				timer.start();
				notifyListeners(new ControlAction(ControlAction.SHUTDOWN_ACTION));
				switchState(STOPPING_STATE);
			}

			// everything must be ok before going to OPERATIONAL
			if (system == EnvironmentChangeEvent.SYSTEM_OKAY
					&& control == EnvironmentChangeEvent.CONTROL_ENABLED
					&& network == EnvironmentChangeEvent.NETWORK_OKAY
					&& axes == EnvironmentChangeEvent.AXES_OKAY
					&& tod == EnvironmentChangeEvent.NIGHT_TIME
					&& enclosure == EnvironmentChangeEvent.ENCLOSURE_OPEN
					&& weather == EnvironmentChangeEvent.WEATHER_CLEAR) {
				notifyListeners(new ControlAction(
						ControlAction.OPERATIONAL_ACTION));
				switchState(OPERATIONAL_STATE);
			}

			break;

		case STARTING_STATE:
			// --------
			// STARTING
			// --------
			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				switchState(INIT_STATE);
			}

			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}
			// still suspended after 20M
			if (system == EnvironmentChangeEvent.SYSTEM_SUSPEND
					&& timer.elapsed(20)) {
				switchState(STANDBY_STATE);
			}
			// still in standby after 5M
			if (system == EnvironmentChangeEvent.SYSTEM_STANDBY
					&& timer.elapsed(5)) {
				switchState(STANDBY_STATE);
			}

			// expected conditions after startup
			if (system == EnvironmentChangeEvent.SYSTEM_OKAY) {
				switchState(STANDBY_STATE);
			}
			// sun rose while starting up
			if (tod == EnvironmentChangeEvent.DAY_TIME) {
				switchState(STANDBY_STATE);
			}
			break;

		case OPENING_STATE:
			// -------
			// OPENING
			// -------

			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				switchState(INIT_STATE);
			}
			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			// weather goes bad while opening
			if (weather == EnvironmentChangeEvent.WEATHER_ALERT) {
				switchState(STANDBY_STATE);
			}
			// sun rose while opening
			if (tod == EnvironmentChangeEvent.DAY_TIME) {
				switchState(STANDBY_STATE);
			}

			// its actual daytime
			if (!sunset) {
				switchState(STANDBY_STATE);
			}

			// 15 minutes and not open yet
			if (enclosure != EnvironmentChangeEvent.ENCLOSURE_OPEN
					&& timer.elapsed(5)) {
				switchState(STANDBY_STATE);
			}

			// normal conmpletion
			if (enclosure == EnvironmentChangeEvent.ENCLOSURE_OPEN) {
				switchState(STANDBY_STATE);
			}

			break;

		case OPERATIONAL_STATE:
			// -----------
			// OPERATIONAL
			// -----------
			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				notifyListeners(new FastAbortAction(AbortAction.ENG_REQUEST,
						AbortAction.ENG_REQUEST_STR));
				switchState(INIT_STATE);
			}

			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new FastAbortAction(AbortAction.RCS_SHUTDOWN,
						AbortAction.RCS_SHUTDOWN_STR));
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			if (system != EnvironmentChangeEvent.SYSTEM_OKAY) {
				notifyListeners(new AbortAction(AbortAction.SYS_ALERT,
						AbortAction.SYS_ALERT_STR));
				switchState(STANDBY_STATE);
			}

			if (weather == EnvironmentChangeEvent.WEATHER_ALERT) {
				notifyListeners(new AbortAction(AbortAction.BAD_WEATHER,
						AbortAction.BAD_WEATHER_STR));
				switchState(STANDBY_STATE);
			}
			if (control != EnvironmentChangeEvent.CONTROL_ENABLED) {
				notifyListeners(new AbortAction(AbortAction.CTRL_DISABLED,
						AbortAction.CTRL_DISABLED_STR));
				switchState(STANDBY_STATE);

			}
			if (network == EnvironmentChangeEvent.NETWORK_ALERT) {
				notifyListeners(new AbortAction(AbortAction.CIL_NET_ALERT,
						AbortAction.CIL_NET_AKERT_STR));
				switchState(STANDBY_STATE);
			}

			if (enclosure != EnvironmentChangeEvent.ENCLOSURE_OPEN) {
				notifyListeners(new AbortAction(AbortAction.ENC_NOT_OPEN,
						AbortAction.ENC_NOT_OPEN_STR));
				switchState(STANDBY_STATE);
			}

			if (tod == EnvironmentChangeEvent.DAY_TIME) {
				notifyListeners(new AbortAction(AbortAction.DAYTIME,
						AbortAction.DAYTIME_STR));
				switchState(STANDBY_STATE);
			}

			if (axes != EnvironmentChangeEvent.AXES_OKAY) {
				notifyListeners(new FastAbortAction(AbortAction.AXES_ALERT,
						AbortAction.AXES_ALERT_STR));
				switchState(STANDBY_STATE);
			}

			if (stability == 1) {
				notifyListeners(new FastAbortAction(AbortAction.PMC_CLOSED,
						stabilityCriterion));
				switchState(INIT_STATE);
			}

			break;

		case CLOSING_STATE:
			// -------
			// CLOSING
			// -------
			// System.err.println("NSM::CheckTrans: Test in CLOSING");
			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				switchState(INIT_STATE);
			}

			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			// normal closing behaviour
			if (enclosure == EnvironmentChangeEvent.ENCLOSURE_CLOSED) {
				switchState(STANDBY_STATE);
			}

			// 15 minutes and not closed yet
			if (enclosure != EnvironmentChangeEvent.ENCLOSURE_CLOSED
					&& timer.elapsed(5)) {
				switchState(STANDBY_STATE);
			}

			break;

		case STOPPING_STATE:
			// --------
			// STOPPING
			// --------
			if (intent == EnvironmentChangeEvent.INTENT_ENGINEERING) {
				switchState(INIT_STATE);
			}
			if (run != EnvironmentChangeEvent.OP_RUN) {
				notifyListeners(new PowerDownAction(run));
				switchState(SHUTDOWN_STATE);
			}

			// there is no status that can indicate that system is opered off.

			// 25 minutes and axes are still not offline
			if (axes != EnvironmentChangeEvent.AXES_ERROR && timer.elapsed(25)) {
				switchState(STANDBY_STATE);
			}

			// normal completion - VERY SIMILAR TO ABOVE !
			if (axes == EnvironmentChangeEvent.AXES_ERROR && timer.elapsed(25)) {
				switchState(STANDBY_STATE);
			}

			break;
		default:
			System.err
					.println("NSM::CheckTrans:Default handling No-Current-State");
		}

	}

	/** Notify registered ControlActionListeners of a control action. */
	private void notifyListeners(ControlAction action) {

		logger.create().info().level(2).extractCallInfo()
				.msg("Exec Control Action: " + action).send();

		ControlActionListener cal = null;
		Iterator list = cals.iterator();
		while (list.hasNext()) {
			cal = (ControlActionListener) list.next();
			try {
				cal.performAction(action);
			} catch (RemoteException rx) {
				rx.printStackTrace();
				System.err.println("NSM::Removing listener: " + cal);
				list.remove();
			}
		}

		ControlActionImplementor caim = null;
		Iterator list2 = cais.iterator();
		while (list2.hasNext()) {
			caim = (ControlActionImplementor) list2.next();
			try {
				caim.performAction(action, this);
			} catch (RemoteException rx) {
				rx.printStackTrace();
				System.err.println("NSM::Removing listener: " + caim);
				list2.remove();
			}
		}

	}

	/** Notify registered StateChangeListeners of a control action. */
	private void notifyStateListeners(IState oldState, IState newState) {
		StateChangeListener scl = null;
		Iterator list = scls.iterator();
		while (list.hasNext()) {
			scl = (StateChangeListener) list.next();
			try {
				scl.stateChanged(oldState, newState);
			} catch (RemoteException rx) {
				rx.printStackTrace();
				System.err.println("NSM::Removing listener: " + scl);
				list.remove();
			}
		}

	}

	private void switchState(int s) {
		IState oldState = state;
		this.state = new TestState(s, stateToString(s));
		this.intState = s;

		logger.create()
				.info()
				.level(2)
				.extractCallInfo()
				.msg("Switch state from [" + oldState.getStateName() + "] to ["
						+ state.getStateName() + "]").send();

		notifyStateListeners(oldState, state);
	}

	public static String stateToString(int state) {
		switch (state) {
		case INIT_STATE:
			return "INIT";
		case STANDBY_STATE:
			return "STANDBY";
		case OPERATIONAL_STATE:
			return "OPERATIONAL";
		case OPENING_STATE:
			return "OPENING";
		case CLOSING_STATE:
			return "CLOSING";
		case STARTING_STATE:
			return "STARTING";
		case STOPPING_STATE:
			return "STOPPING";
		case SHUTDOWN_STATE:
			return "SHUTDOWN";
		default:
			return "UNKNOWN";
		}
	}

	public IState getCurrentState() throws RemoteException {
		return state;
	}

	public int getIntState() {
		return intState;
	}

	public void addStateChangeListener(StateChangeListener scl)
			throws RemoteException {
		if (scls.contains(scl))
			return;
		scls.add(scl);
		// and notify it right away so it knows the current state..
		try {
			scl.stateChanged(null, state);
		} catch (RemoteException rx) {
			rx.printStackTrace();
			System.err.println("NSM::Removing listener: " + scl);
			scls.remove(scl);
		}
	}

	public void removeStateChangeListener(StateChangeListener scl)
			throws RemoteException {
		if (!scls.contains(scl))
			return;
		scls.remove(scl);
	}

	public void controlActionFailed(String message) throws RemoteException {
		// TODO Auto-generated method stub
		System.err.println("NSM::ControlAction Failed: " + message);

		signal.setValue(true);
	}

	public void controlActionSuccess() throws RemoteException {

		logger.create().info().level(2).extractCallInfo()
				.msg("ControlAction Success").send();

		signal.setValue(true);
	}

	/** Start monitoring stability criteria. */
	public void startStabilityMonitoring(boolean needAutoguider) {

		logger.create()
				.info()
				.level(2)
				.extractCallInfo()
				.msg("Start stability monitoring, A/G "
						+ (needAutoguider ? "required" : "not required"))
				.send();

		// tm.reset();
		// if (needAutoguider)
		// am.reset();
		pm.reset();
		// We want to know about the guide status...
		// //am.addGuideStatusListener(this);
		// //tm.addTrackingStatusListener(this);

	}

	/** Stop monitoring stability criteria. */
	public void stopStabilityMonitoring() {

		logger.create().info().level(2).extractCallInfo()
				.msg("Stop stability monitoring").send();

		// tm.setEnableAlerts(false);
		// am.setEnableAlerts(false);
		pm.setEnableAlerts(false);
		// //am.removeGuideStatusListener(this);
		// //tm.removeTrackingStatusListener(this);
		stability = 0;
	}

	public void notifyEvent(String topic, Object data) {
		// translate event into event code
		System.err
				.println("NSM: notifyEvent(): Received event topic: " + topic);

		try {
			int type = ((Integer) events.get(topic)).intValue();
			System.err.println("NSM: notifyEvent(): Mapped event topic: "
					+ topic + " as " + type + " "
					+ EnvironmentChangeEvent.typeToString(type));
			environmentChanged(new EnvironmentChangeEvent(type));
		} catch (Exception e) {
			System.err.println("NSM::No mapping for event: " + topic);
		}
	}

	public String getSubscriberId() {
		return "NEW_STATE_MODEL";
	}

	// public void setTrackingMonitor(DefaultTrackingMonitor tm){
	// this.tm = tm;
	// }

	// public void setAutoguiderMonitor(DefaultAutoguiderMonitor am) {
	// this.am = am;
	// }

	public void setPmcMonitor(PmcMonitor pm) {
		this.pm = pm;
	}

	/**
	 * Called by stability monitor if the PMC is seen to close during observing.
	 */
	public void pmcClosed() {
		stability = 1;
		stabilityCriterion = AbortAction.PMC_CLOSED_STR;

		logger.create().info().level(2).extractCallInfo()
				.msg("Primary mirror cover has closed, possibly unexpectedly")
				.send();

	}

	// public void trackingLost() {
	// stability = 1;
	// stabilityCriterion = AbortAction.TRACKING_LOST_STR;
	// System.err.println("NSM: TrackListener: TRACKING_LOST Trigger");
	// }

	// public void guideLockLost() {
	// stability = 1;
	// stabilityCriterion = AbortAction.GUIDE_LOST_STR;
	// System.err.println("NSM: AgListener: GUIDE_LOST Trigger");
	// }

	private class Timer {

		long start;

		String name;

		public Timer(String name) {
			this.name = name;
		}

		public void start() {
			start = System.currentTimeMillis();
		}

		public long elapsed() {
			return System.currentTimeMillis() - start;
		}

		public boolean elapsed(int minutes) {
			return (elapsed() >= minutes * 60 * 1000L);
		}

		public void clear() {
			start = 0L;
		}

		@Override
		public String toString() {
			return "Timer: " + name + " elapsed " + (elapsed() / 60000L) + "m";
		}

	}

}

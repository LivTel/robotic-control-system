package ngat.rcs.ops;

import ngat.rcs.*;
import ngat.rcs.newstatemodel.*;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskDescriptor;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskModeController;
import ngat.rcs.tms.TaskModeManagement;
import ngat.rcs.tms.TaskOperations;
import ngat.rcs.tms.TaskWorker;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.iss.*;
import ngat.message.RCS_TCS.*;
import ngat.util.*;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

import java.util.*;
import java.rmi.*;

/** Operations manager. */
public class OperationsManager extends ControlThread implements TaskManager, ControlActionImplementor,
		OperationsMonitor {

	public static final int OP_STATE_IDLE = 1;
	public static final int OP_STATE_INIT = 2;
	public static final int OP_STATE_FINAL = 3;
	public static final int OP_STATE_OP = 4;
	public static final int OP_STATE_OP_SWITCH = 5;

	public static final int TRANS_STATE_IDLE = 11;
	public static final int TRANS_STATE_EXEC_OPEN = 12;
	public static final int TRANS_STATE_EXEC_CLOSE = 13;
	public static final int TRANS_STATE_EXEC_START = 14;
	public static final int TRANS_STATE_EXEC_STOP = 15;

	public static final int OPEN_REQUESTED = 1;
	public static final int OPEN_DONE = 2;
	public static final int OPEN_FAILED = 3;
	public static final int OPEN_ABORTED = 4;

	public static final int CLOSE_REQUESTED = 5;
	public static final int CLOSE_DONE = 6;
	public static final int CLOSE_FAILED = 7;
	public static final int CLOSE_ABORTED = 8;

	public static final int START_REQUESTED = 9;
	public static final int START_DONE = 10;
	public static final int START_FAILED = 11;
	public static final int START_ABORTED = 12;

	public static final int STOP_REQUESTED = 13;
	public static final int STOP_DONE = 14;
	public static final int STOP_FAILED = 15;
	public static final int STOP_ABORTED = 16;

	public static final int OPER_REQUESTED = 17;
	public static final int OPER_ABORT_REQUESTED = 18;
	public static final int OPER_MCA_YIELD = 25; // an MCA is yielding for
	// re-election.
	public static final int OPER_MCA_ABORTED = 26; // an MCA has aborted yield
	// OR threat.
	public static final int OPER_FAST_ABORT_REQUESTED = 27;

	public static final int INIT_FAILED = 19;
	public static final int INIT_ABORTED = 20;
	public static final int INIT_DONE = 21;

	public static final int FINAL_FAILED = 22;
	public static final int FINAL_ABORTED = 23;
	public static final int FINAL_DONE = 24;

	public static final int SHUTDOWN_REQUESTED = 30;
	public static final int SHUTDOWN_FAILED = 31;
	public static final int SHUTDOWN_ABORTED = 32;
	public static final int SHUTDOWN_DONE = 33;

	public static final long POLL_INTERVAL = 500L;

	List opListeners;

	List transEvents;

	List opEvents;

	// SlotBuffer transEvents;

	// SlotBuffer opEvents;

	// SlotBuffer allEvents;

	/** Operations task state. */
	volatile int opState;

	/** Transional task state. */
	volatile int transState;
	
	/** When did we last broadcast state.*/
	volatile long timeOfLastBroadcast = 0L;
	
	volatile int irc = 0;
	volatile int iwc = 0;

	Task openTask;
	Task closeTask;
	Task startTask;
	Task stopTask;
	Task initTask;
	Task finalTask;
	Task powerDownTask;

	DefaultModalTask currentModeController;

	/** Records the abort code if set.*/
	private volatile int abortCode = 0;
	
	/** Records the abortReason if set.*/
	private String abortReason = "none";
	
	
	private int powerDownMode = EnvironmentChangeEvent.OP_RUN;

	private TaskDescriptor descriptor;
	
	/** Logger. */
	private LogGenerator slogger;
	
	public OperationsManager(String name) throws RemoteException {
		super(name, true);
		descriptor = new TaskDescriptor(name, getClass().getSimpleName());
		opListeners = new Vector();
		transEvents = Collections.synchronizedList(new Vector());
		opEvents = Collections.synchronizedList(new Vector());

		// transEvents = new SlotBuffer(100);
		// opEvents = new SlotBuffer(100);
		// allEvents = new SlotBuffer(100);
		opState = OP_STATE_IDLE;
		transState = TRANS_STATE_IDLE;

		Logger alogger = LogManager.getLogger("TASK"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("TMS")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId("OpsMgr");
		
		
		
		// setPriority(6);
		try {
			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskCreated(null, descriptor);

			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskInitialized(descriptor);

			RCS_Controller.controller.getTaskMonitor().notifyListenersTaskStarted(descriptor);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Registers an instance of OperationsEventListenerfor notification of any
	 * OperationsEvents. If the listener is already registered this method
	 * should return silently.
	 * 
	 * @param l
	 *            An instance of OperationsEventListener.
	 * @throws RemoteException
	 */
	public void addOperationsEventListener(OperationsEventListener l) throws RemoteException {
		if (opListeners.contains(l))
			return;
		opListeners.add(l);
		
		// tell them who is in control now.....
		//l.operationsEventNotification(new OperationsStartingEvent(eventTimeStamp, currentModeController.getAgentDesc()));
		
	}

	/**
	 * Remove the specified listener from the list of registered
	 * statusListeners. If the listener is not registered this method should
	 * return silently.
	 * 
	 * @param l
	 *            An instance of OperationsEventListener.
	 * @throws RemoteException
	 */
	public void removeOperationsEventListener(OperationsEventListener l) throws RemoteException {
		if (!opListeners.contains(l))
			return;
		opListeners.remove(l);
	}

	public void notifyOpsListenersModeChange(String oldMode, String newMode) {
		for (int il = 0; il < opListeners.size(); il++) {
			OperationsEventListener ol = (OperationsEventListener) opListeners.get(il);
			try {
				//ol.modeChanged(oldMode, newMode);
				ol.operationsEventNotification(
						new OperationsModeChangedEvent(System.currentTimeMillis(),oldMode, newMode));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}
	
	
	/** This is a badly named method it signifies that a particular ModeController has taken over and is in control.
	 * @param controllerName Actually the name of a mode controller ...
	 */
	public void notifyOpsListenersOperationStarting(String controllerName) {
		for (int il = 0; il < opListeners.size(); il++) {
			OperationsEventListener ol = (OperationsEventListener) opListeners.get(il);
			try {
			
				ol.operationsEventNotification(
						new OperationsStartingEvent(System.currentTimeMillis(), controllerName));			
				slogger.create().info().level(2).extractCallInfo().msg("Starting job: "+controllerName).send();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Request to perform the specified action.
	 * 
	 * @param ca
	 *            A control action to perform.
	 * @param handler
	 *            A ControlActionResponseHandler to accept the response
	 *            callback.
	 * @throws RemoteException
	 */
	public void performAction(ControlAction ca, ControlActionResponseHandler handler) throws RemoteException {
		int type = ca.getType();
		slogger.create().info().level(2).extractCallInfo().msg("Perfoming action: "+ca.getType()).send();
		switch (type) {
		case ControlAction.CLOSE_ACTION:
			performCloseAction();
			break;
		case ControlAction.OPEN_ACTION:
			performOpenAction();
			break;
		case ControlAction.STARTUP_ACTION:
			performStartupAction();
			break;
		case ControlAction.SHUTDOWN_ACTION:
			performShutdownAction();
			break;
		case ControlAction.OPERATIONAL_ACTION:
			performOperationalAction();
			break;
		case ControlAction.ABORT_OPERATIONS_ACTION:
			AbortAction abortAction = (AbortAction)ca;
			performAbortAction(abortAction.getCode(), abortAction.getReason());
			break;
		case ControlAction.FAST_ABORT_OPERATIONS_ACTION:
			FastAbortAction fastAbortAction = (FastAbortAction)ca;
			performFastAbortAction(fastAbortAction.getCode(), fastAbortAction.getReason());
			break;
		case ControlAction.POWERDOWN_ACTION:
			performPowerdownAction(((PowerDownAction) ca).getPowerDownMode());
			break;
		default:
			// do nothing
		}

	}

	private void performCloseAction() {
		// add a CloseTask onto the transitional list
		// System.err.println("OPS:: Perform CLOSE action");
		addTransient(CLOSE_REQUESTED);
	}

	private void performOpenAction() {
		// System.err.println("OPS:: Perform OPEN action");
		addTransient(OPEN_REQUESTED);
	}

	private void performStartupAction() {
		// System.err.println("OPS:: Perform STARTUP action");
		addTransient(START_REQUESTED);
	}

	private void performShutdownAction() {
		// System.err.println("OPS:: Perform SHUTDOWN action");
		addTransient(STOP_REQUESTED);
	}

	private void performOperationalAction() {
		// startup the operational control agent (OCA)
		// System.err.println("OPS:: Perform OPERATIONAL action");
		addOps(OPER_REQUESTED);
	}

	private void performAbortAction(int code, String reason) {
		// abort the operational control agent (OCA)
		// System.err.println("OPS:: Perform ABORT action due to: "+reason);
		abortCode = code;
		abortReason = reason;
		addOps(OPER_ABORT_REQUESTED);
	}

	private void performFastAbortAction(int code, String reason) {
		// abort the operational control agent (OCA)
		// System.err.println("OPS:: Perform ABORT action due to: "+reason);
		abortCode = code;
		abortReason = reason;
		addOps(OPER_FAST_ABORT_REQUESTED);
	}

	private void performPowerdownAction(int mode) {
		powerDownMode = mode;
		addTransient(SHUTDOWN_REQUESTED);
	}
	
	/** Only transients can complete - MCAs never signal completion.*/
	public void sigTaskDone(Task task) {
		slogger.create().info().level(2).extractCallInfo().msg("Task done: " + task.getName()).send();
		if (task == openTask)
			addTransient(OPEN_DONE);
		else if (task == closeTask)
			addTransient(CLOSE_DONE);
		else if (task == startTask)
			addTransient(START_DONE);
		else if (task == stopTask)
			addTransient(STOP_DONE);
		else if (task == initTask)
			addOps(INIT_DONE);
		else if (task == finalTask)
			addOps(FINAL_DONE);
		else if (task == powerDownTask)
			addTransient(SHUTDOWN_DONE);
	}

	/** Only transients can fail - MCAs never signal failure.*/
	public void sigTaskFailed(Task task) {
		slogger.create().info().level(2).extractCallInfo().msg("Task failed: " + task.getName() + ", " + task.getErrorIndicator()).send();
		if (task == openTask)
			addTransient(OPEN_FAILED);
		else if (task == closeTask)
			addTransient(CLOSE_FAILED);
		else if (task == startTask)
			addTransient(START_FAILED);
		else if (task == stopTask)
			addTransient(STOP_FAILED);
		else if (task == initTask)
			addOps(INIT_FAILED);
		else if (task == finalTask)
			addOps(FINAL_FAILED);
		else if (task == powerDownTask)
			addTransient(SHUTDOWN_FAILED);
	}

	/** Transient and MCA Tasks can be aborted. If its an MCA then it is finished.*/
	public void sigTaskAborted(Task task) {
		slogger.create().info().level(2).extractCallInfo().msg("Task aborted: " + task.getName() + " " + task).send();
		if (task == openTask)
			addTransient(OPEN_ABORTED);
		else if (task == closeTask)
			addTransient(CLOSE_ABORTED);
		else if (task == startTask)
			addTransient(START_ABORTED);
		else if (task == stopTask)
			addTransient(STOP_ABORTED);
		else if (task == initTask)
			addOps(INIT_ABORTED);
		else if (task == finalTask)
			addOps(FINAL_ABORTED);
		else if (task == powerDownTask)
			addTransient(SHUTDOWN_ABORTED);
		else if (task instanceof DefaultModalTask) {
			// System.err.println("OPS: Posting ABORTED message");
			addOps(OPER_MCA_ABORTED);

			// System.err.println("OPS: OpsQueue size: "+opEvents.size()+"::"+opEvents);

		}
	}

	public void sigMessage(Task source, int category, Object message) {
	}

	/**
	 * This is to be replaced by an OpsMgr method callback such as:
	 * opsMgr.mcaJobDone(...params...) called by an MCA on completion of a job.
	 * or OpsMgr.yieldControl(...params...)
	 */
	public void notifyEvent(String commandCode, Object data) {
		//if (commandCode.equals(RCS_ControlTask.TASK_YIELD_MESSAGE)) {
			// an MCA is yielding control after completing a job
		//	addOps(OPER_MCA_YIELD);
		//}
	}

	/** Called by modal tasks to yield control.
	 * @param message A message from the yielding task. 
	 * TODO change to some encapsulated object.
	 * */
	public void operationsModeYieldControl(String message) {
		slogger.create().info().level(2).extractCallInfo()
			.msg("Control yielded: "+message)
			.send();
		addOps(OPER_MCA_YIELD);		
	}
	
	/**
	 * EventSubscriber method. will dissappear also
	 */
	public String getSubscriberId() {
		return "OPS_MGR";
	}

	@Override
	public void initialise() {
	}

	/** Main polling loop. */
	@Override
	public void mainTask() {

		try {
			Thread.sleep(POLL_INTERVAL);
		} catch (InterruptedException ix) {
		}
		
		checkTransEvents();

		checkOpEvents();
		
		// broadcast current state every 10 secs at most
		broadcastState();

	}

	@Override
	public void shutdown() {
		System.err.println("OPS::Shutting down operations thread");
	}

	private void addTransient(int code) {
		transEvents.add(new Integer(code));
		// try {
		// allEvents.add(new Integer(code));
		// iwc++;
		// System.err.println("OPS::AddTrans:"+Thread.currentThread()+" Added "+code+" Size="+transEvents.size());
		// } catch (InterruptedException ix) {
		// System.err.println("OPS::AddTrans: Interrupted adding "+code);
		// }
	}

	private int nextTransient() {
		// System.err.println("OPS::NxtTrans:"+Thread.currentThread()+" = "+transEvents);
		if (transEvents.size() < 1)
			return -1;

		return ((Integer) transEvents.remove(0)).intValue();
		// return 1;
	}

	private void addOps(int code) {
		opEvents.add(new Integer(code));
		// try {
		// allEvents.add(new Integer(code));
		// ioc++;
		// System.err.println("OPS::AddOps:"+Thread.currentThread()+" Added "+code+" Size="+opEvents.size());
		// } catch (InterruptedException ix) {
		// System.err.println("OPS::AddOps: Interrupted adding "+code);
		// }

	}

	private int nextOps() {
		// System.err.println("OPS::NxtOps:"+Thread.currentThread()+" = "+opEvents);
		if (opEvents.size() < 1)
			return -1;

		return ((Integer) opEvents.remove(0)).intValue();
		// return 1;
	}

	private void checkTransEvents() {

		int event = nextTransient();
		if (event == -1)
			return;

		slogger.create().info().level(2).extractCallInfo().msg("CheckTransEvents, next event is: " + event).send();
		switch (event) {
		case OPEN_DONE:
		case OPEN_FAILED:
		case OPEN_ABORTED:
			disposeTask(openTask);
			break;
		case CLOSE_DONE:
		case CLOSE_FAILED:
		case CLOSE_ABORTED:
			disposeTask(closeTask);
			break;
		case START_DONE:
		case START_FAILED:
		case START_ABORTED:
			disposeTask(startTask);
			break;
		case STOP_DONE:
		case STOP_FAILED:
		case STOP_ABORTED:
			disposeTask(stopTask);
			break;
		case SHUTDOWN_DONE:
		case SHUTDOWN_FAILED:
		case SHUTDOWN_ABORTED:
			disposeTask(powerDownTask);

			// Now actually shutdown the RCS.
			// RCS_Controller.RESTART_ENGINEERING;
			// RCS_Controller.RESTART_ROBOTIC;
			// RCS_Controller.REBOOT;
			// RCS_Controller.HALT;
			// RCS_Controller.SHUTDOWN;

			slogger.create().info().level(2).extractCallInfo()
				.msg("Shutting down the RCS...powerdown mode: " + powerDownMode).send();
			int exitCode = 0;
			switch (powerDownMode) {
			case EnvironmentChangeEvent.OP_RESTART_ENG:
				exitCode = RCS_Controller.RESTART_ENGINEERING;
				break;
			case EnvironmentChangeEvent.OP_RESTART_AUTO:
				exitCode = RCS_Controller.RESTART_ROBOTIC;
				break;
			case EnvironmentChangeEvent.OP_REBOOT:
				exitCode = RCS_Controller.REBOOT;
				break;
			case EnvironmentChangeEvent.OP_RESTART_INSTR:
				exitCode = RCS_Controller.REBOOT;
				break;
			}
			
			slogger.create().info().level(2).extractCallInfo()
			.msg("Shutting down the RCS..exit code mapped to: " + exitCode).send();
			try {
			RCS_Controller.controller.terminate(exitCode);
			} catch (Exception e) {
				slogger.create().info().level(2).extractCallInfo()
				.msg("Exception shutting down RCS: "+e).send();
				e.printStackTrace();
			}
			System.exit(exitCode - 605000);
			break;
		}

		switch (event) {
		case OPEN_REQUESTED:
			openTask = new Enclosure_Task("TRANS_OPEN", this, ENCLOSURE.BOTH, ENCLOSURE.OPEN);
			startTask(openTask);
			// switchTransState(TRANS_STATE_EXEC_OPEN);
			break;
		case CLOSE_REQUESTED:
			closeTask = new CloseTask("TRANS_CLOSE", this);
			startTask(closeTask);
			// switchTransState(TRANS_STATE_EXEC_CLOSE);
			break;
		case START_REQUESTED:
			startTask = new Operational_Task("TRANS_START", this, OPERATIONAL.ON);
			startTask(startTask);
			// switchTransState(TRANS_STATE_EXEC_START);
			break;
		case STOP_REQUESTED:
			stopTask = new Operational_Task("TRANS_STOP", this, OPERATIONAL.OFF);
			startTask(stopTask);
			// switchTransState(TRANS_STATE_EXEC_STOP);
			break;
		case SHUTDOWN_REQUESTED:
			System.err.println("OPS:: CheckTransEvents: Starting powerdown task");
			// determine rcs and instr options
			int rcsCode = RCS_Controller.RESTART_ENGINEERING;
			int instCode = PowerDownTask.INST_NO_ACTION;
			switch (powerDownMode) {
			case EnvironmentChangeEvent.OP_RESTART_ENG:
				rcsCode = RCS_Controller.RESTART_ENGINEERING;
				instCode = PowerDownTask.INST_NO_ACTION;
				break;
			case EnvironmentChangeEvent.OP_RESTART_AUTO:
				rcsCode = RCS_Controller.RESTART_ROBOTIC;
				instCode = PowerDownTask.INST_NO_ACTION;
				break;
			case EnvironmentChangeEvent.OP_REBOOT:
				rcsCode = RCS_Controller.REBOOT;
				instCode = PowerDownTask.INST_NO_ACTION;
				break;
			case EnvironmentChangeEvent.OP_RESTART_INSTR: // we reboot
				// everything..
				rcsCode = RCS_Controller.REBOOT;
				instCode = PowerDownTask.INST_REBOOT;
				break;
			}

			powerDownTask = new PowerDownTask("TRANS_QUIT", this);
			((PowerDownTask) powerDownTask).setInstrumentAction(instCode);
			((PowerDownTask) powerDownTask).setShutdownAction(rcsCode);
			powerDownTask.setDelay(10000L);
			startTask(powerDownTask);
			break;
		}

	}

	private void checkOpEvents() {

		int event = nextOps();
		if (event == -1)
			return;

		slogger.create().info().level(2).extractCallInfo()
		.msg("CheckOpEvents in Opstate: " + toStateString(opState) + ", This event is: " + event).send();
		switch (event) {
		case INIT_DONE:
		case INIT_FAILED:
		case INIT_ABORTED:
			disposeTask(initTask);
			break;
		case FINAL_DONE:
		case FINAL_FAILED:
		case FINAL_ABORTED:
			disposeTask(finalTask);
			break;
		}

		switch (opState) {

		case OP_STATE_IDLE:
			switch (event) {
			case OPER_REQUESTED:
				initTask = new InitializeTask("OPER_INIT", this);
				startTask(initTask);
				switchOpState(OP_STATE_INIT);
				break;
			}
			break;
		case OP_STATE_INIT:
			switch (event) {
			case OPER_ABORT_REQUESTED:
				initTask.abort();
				finalTask = new FinalizeTask("OPER_FINAL", this);
				startTask(finalTask);
				switchOpState(OP_STATE_FINAL);
				break;
			case INIT_DONE:
			case INIT_FAILED:
			case INIT_ABORTED:
				currentModeController = selectOperationsController();
				// check next MCA and work out timeTillNextMca to pass as
				// done time..
				long ttn = timeOfNextMcaGiven(currentModeController);
				startOperationsController(currentModeController, ttn);
				RCS_Controller.controller.setOperational(true);
				switchOpState(OP_STATE_OP);
				break;
			case OPER_FAST_ABORT_REQUESTED:
				switchOpState(OP_STATE_IDLE);
				currentModeController = null;
				break;
			}
			break;
		case OP_STATE_FINAL:
			switch (event) {
			case OPER_REQUESTED:
				finalTask.abort();
				initTask = new InitializeTask("OPER_INIT", this);
				startTask(initTask);
				switchOpState(OP_STATE_INIT);
				break;
			case FINAL_DONE:
			case FINAL_FAILED:
			case FINAL_ABORTED:
				switchOpState(OP_STATE_IDLE);
				currentModeController = null;
				break;
			case OPER_FAST_ABORT_REQUESTED:
				switchOpState(OP_STATE_IDLE);
				currentModeController = null;
				break;
			}
			break;
		case OP_STATE_OP:
			
			switch (event) {
			case OPER_ABORT_REQUESTED:
				abortOperationsController(abortCode, abortReason);
				finalTask = new FinalizeTask("OPER_FINAL", this);
				startTask(finalTask);
				RCS_Controller.controller.setOperational(false);
				switchOpState(OP_STATE_FINAL);
				break;
			case OPER_FAST_ABORT_REQUESTED:
				abortOperationsController(abortCode, abortReason);
				RCS_Controller.controller.setOperational(false);
				switchOpState(OP_STATE_IDLE);
				currentModeController = null;
				break;
			case OPER_MCA_YIELD:
				// An MCA is yielding for re-election
				DefaultModalTask nextModeController = selectOperationsController();
				if (nextModeController == currentModeController) {
					// just do another job.
					String cmname = "unknown";
					try {	
						cmname = currentModeController.getModeName();
					}catch (Exception e) {
					}
					
					slogger.create().info().level(2).extractCallInfo()
						.msg("Current MCA: " + cmname + " next job...")
						.send();
					
					currentModeController.nextJob();
				} else {
					String nmname = "unknown";
					try {
						nmname = nextModeController.getModeName();
					} catch (Exception e) {
					}

					currentModeController.setAbortCode(600111, "OVERRIDE: Pre-empted by higher priority mode: " + nmname);
					currentModeController.abort();
					switchOpState(OP_STATE_OP_SWITCH);
					// System.err.println("OPS:: Switched opstate with aborting CMC="+currentModeController);

				}
				break;
			}
			break;
		case OP_STATE_OP_SWITCH:
			switch (event) {
			case OPER_MCA_ABORTED:
				// weve aborted an MCA to allow a new MCA to take over...
				// Teardown as mode aborted.
				invokeGenericModeTeardown();

				currentModeController = selectOperationsController();
				// TODO
				long ttn = timeOfNextMcaGiven(currentModeController);
				// startOperationsController(currentModeController,
				// timeTillNextMca);
				startOperationsController(currentModeController, ttn);
				switchOpState(OP_STATE_OP);
				break;
			case OPER_ABORT_REQUESTED:
				// Abort requested while switching and waiting an MCA aborted...
				finalTask = new FinalizeTask("OPER_FINAL", this);
				startTask(finalTask);
				switchOpState(OP_STATE_FINAL);
			case OPER_FAST_ABORT_REQUESTED:
				// Abort requested while switching and waiting an MCA aborted...
				switchOpState(OP_STATE_IDLE);
				currentModeController = null;
			}
			break;
		}
	}
	
	/** Broadcast state every 10 sec or so, this is for the benefit of external listeners as the state may not actually
	 * change very often.*/
	private void broadcastState() {
		if (opState == OP_STATE_OP) {
		
		if (System.currentTimeMillis() - timeOfLastBroadcast > 10*1000L) {
			
			DefaultModalTask cmt = currentModeController;
			String mode = null;
			if (cmt != null)
				mode = cmt.getAgentId();
			notifyOpsListenersModeChange(mode, mode);
			
			timeOfLastBroadcast = System.currentTimeMillis();
		}
		}
	}

	/** Select and start a new operations controller (MCA). */
	private DefaultModalTask selectOperationsController() {
		// System.err.println("OPS::Selecting operations controller...");
		TaskModeManagement modeManager = TaskOperations.getInstance();
		long now = System.currentTimeMillis();
		int hip = 99;
		TaskModeController hit = null;
		try {
			List mcas = modeManager.listModeControllers();
			Iterator it = mcas.iterator();
			while (it.hasNext()) {
				TaskModeController tmc = (TaskModeController) it.next();
				// System.err.println("OPM:SelectOpsController: Testing: "+tmc.getModeName());
				if (tmc.getPriority() < hip && tmc.wantsControl(now)) {
					hip = tmc.getPriority();
					hit = tmc;
				}
			}

			// selected a TMC

			// ((TaskModeControllerManagement)hit).promote();
			DefaultModalTask mca = (DefaultModalTask) hit;
			return mca;

		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.err.println("OPS:: selectopsctrl: returning: NULL");
		return null;

	}

	/**
	 * start the specified operations controller with given time limit (an
	 * actual time).
	 * @param mca Current MCA.
	 * @param ton Time of next (higher priority) MCA wanting control.
	 */
	private void startOperationsController(DefaultModalTask mca, long ton) {
		// ((TaskModeControllerManagement)hit).promote();
		try {
		
			slogger.create().info().level(2).extractCallInfo()
				.msg("Starting new MCA: " + mca.getModeName() + " with end time: " + (new Date(ton))).send();
			
		} catch (Exception e) {		
			slogger.create().error().level(1).extractCallInfo()
				.msg("Unable to start operations controller: "+mca.getAgentId()+" due to: "+e)
				.send();
		}

		// Here is where we have the chance to do any generic mode
		// initialization stuff
		invokeGenericModeInitialization();

		mca.reset();
		mca.init();
		mca.setTimeLimit(ton - System.currentTimeMillis());
		mca.setTimeConstrained(true);

		(new TaskWorker(mca.getName(), mca)).beginJob();
		// TODO notify mode change
		notifyOpsListenersModeChange((currentModeController != null ? currentModeController.getAgentId() : null),
				mca.getAgentId());
	}

	/** Abort the current operations controller (MCA). */
	private void abortOperationsController(int code, String reason) {
		// System.err.println("OPS::Aborting operations controller...");
		currentModeController.setAbortCode(code, reason);
		currentModeController.abort();
		// TODO notify mode change
		notifyOpsListenersModeChange((currentModeController != null ? currentModeController.getAgentId() : null), null);
	}

	/**
	 * How long till next (higher priority) MCA will want control given current
	 * MCA. TODO THIS IS ACTUALLY RETURNING A TIME NOT A DIFFERENCE
	 * timeOfNextMcaGiven(mca)
	 */
	private long timeOfNextMcaGiven(DefaultModalTask mca) {

		TaskModeManagement modeManager = TaskOperations.getInstance();
		long now = System.currentTimeMillis();
		long ton = now + 5 * 24 * 3600 * 1000L;

		TaskModeController hit = null;
		try {

			// this is our controller's priority (to start with)
			int hip = ((TaskModeController) mca).getPriority();

			List mcas = modeManager.listModeControllers();
			Iterator it = mcas.iterator();
			while (it.hasNext()) {
				TaskModeController tmc = (TaskModeController) it.next();

				// IF the other one wantscontrol sooner than best-so-far and its
				// "lower" priority than current
				long toc = tmc.nextWantsControl(now);
				

				slogger.create().info().level(3).extractCallInfo()
					.msg("Lookahead Testing: " + tmc.getModeName() + 
						" Priority: "+ tmc.getPriority() + 
						" Next higher MCA at: " + (new Date(toc)))
						.send();
					
				if (tmc.getPriority() < hip && toc < ton) {
				
					ton = toc;
					hit = tmc;
					
					slogger.create().info().level(3).extractCallInfo()
					.msg("Best candidate so far for next MCA is: " + hit).send();
					
				}
			}

			// selected a TMC
			slogger.create().info().level(3).extractCallInfo()
			.msg("Next MCA given: " + mca.getModeName() + 
					" will be "+ (hit == null ? "NO_MCA" : hit.getModeName() + 
					" in " + ((ton - now) / 1000) + "s"))
					.send();
			
			return ton;

		} catch (Exception e) {
			e.printStackTrace();
		}

		// fudge we cant find anyone for now...
		// TODO THIS IS WRONG recall we are actually returning the time not a
		// time-diff
		return 24 * 3600 * 1000L;

	}

	/**
	 * Do any generic stuff that needs doing as we start any operating mode
	 * controller.
	 */
	private void invokeGenericModeInitialization() {

		System.err.println("OPM:Generic MCA Initialization");

		FITS_HeaderInfo.setRotatorSkyCorrection(0.0);
		// Re-enable MOVE_FOLDS sent by instruments.
		ngat.rcs.iss.ISS_MOVE_FOLD_CommandImpl.setOverrideForwarding(false);

		ISS.setCurrentFocusOffset(0.0);
		ISS.setInstrumentFocusOffset(0.0);
		// User and instrument required position offsets
		// ISS.setInstrumentOffsets(0.0, 0.0);
		ISS.setUserOffsets(0.0, 0.0);
		ISS.setBeamControlInstrument(null);
	}

	/**
	 * Do any generic stuff that needs doing as we exit any operating mode
	 * controller.
	 */
	private void invokeGenericModeTeardown() {	
		slogger.create().info().level(3).extractCallInfo()
			.msg("Generic MCA Teardown")
			.send();
	}

	private void startTask(Task task) {
		// System.err.println("OPS::Initialize: "+task.getName());
		task.init();
		// System.err.println("OPS::Starting: "+task.getName());
		(new TaskWorker(task.getName(), task)).beginJob();
	}

	private void disposeTask(Task task) {
		// System.err.println("OPS::Disposing: "+task.getName());
		task.dispose();
	}

	private void switchTransState(int state) {
		// System.err.println("OPS::Switch TRANS state: "+toStateString(state));
		transState = state;
	}

	private void switchOpState(int state) {
		// System.err.println("OPS::Switch OP state: "+toStateString(state));
		opState = state;
	}

	public DefaultModalTask getCurrentModeController() {
		return currentModeController;
	}

	/** Returns the operations state. */
	public int getOperationsState() {
		return opState;
	}

	private String toStateString(int state) {

		switch (state) {
		case OP_STATE_IDLE:
			return "OP_IDLE";
		case OP_STATE_INIT:
			return "OP_INITIALIZE";
		case OP_STATE_FINAL:
			return "OP_FINALIZE";
		case OP_STATE_OP:
			return "OP_OBSERVING";
		case OP_STATE_OP_SWITCH:
			return "OP_SWITCH_MODE";
		case TRANS_STATE_IDLE:
			return "TRANS_IDLE";
		case TRANS_STATE_EXEC_OPEN:
			return "OPENING";
		case TRANS_STATE_EXEC_CLOSE:
			return "CLOSING";
		case TRANS_STATE_EXEC_START:
			return "STARTING";
		case TRANS_STATE_EXEC_STOP:
			return "STOPPING";
		}
		return "UNKNOWN";
	}

	public String getManagerName() {
		return "OPSMGR";
	}

	public TaskDescriptor getDescriptor() {
		return descriptor;
	}

}
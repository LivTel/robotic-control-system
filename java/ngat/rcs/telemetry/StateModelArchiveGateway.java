/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.net.telemetry.SecondaryCache;
import ngat.rcs.newstatemodel.IState;
import ngat.rcs.newstatemodel.StateChangeListener;
import ngat.rcs.newstatemodel.StateChangedEvent;
import ngat.rcs.newstatemodel.StateModelArchive;
import ngat.rcs.newstatemodel.StateModelEvent;
import ngat.rcs.newstatemodel.StateModelMonitor;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class StateModelArchiveGateway extends UnicastRemoteObject implements
		StateChangeListener, StateModelMonitor, StateModelArchive {


	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered OperationsEventListeners. */
	private List<StateChangeListener> listeners;

	/** A list of candidate OperationsEventListeners. */
	private List<StateChangeListener> addListeners;

	/** A list of OperationsEventListeners to delete. */
	private List<StateChangeListener> deleteListeners;

	private List<StateModelEvent> archive;

	/** Monitor to register with for StateModelEvents.*/
	private StateModelMonitor stateModelMonitor;
	
	/**
	 * Counts the number of archive entries which have been forwarded to current
	 * listeners.
	 */
	private volatile int processedCount;

	// START TEMPLATE CODE

	/** Processor cycle interval. Default to 10 sec. */
	private long processInterval = 10 * 1000L;

	/** How often do we check for culling. Default to 30 minutes. */
	private long cullInterval = 30 * 60 * 1000L;

	/** Age of oldest data to keep in local cache. Default to 1 hour. */
	private long backingStoreAgeLimit = 60 * 60 * 1000L;

	/**
	 * How often relative to process sweep do we check for culling. Default to
	 * every 10 sweeps.
	 */
	private int cullSweepIndicator = 10;

	/** the secondary cache. */
	private SecondaryCache backingStore;

	// END TEMPLATE CODE

	/**
	 * @param opmon
	 * @throws RemoteException
	 */
	public StateModelArchiveGateway(StateModelMonitor stateModelMonitor) throws RemoteException {
		super();
		this.stateModelMonitor = stateModelMonitor;
		
		Logger alogger = LogManager.getLogger("OPS"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("SMGateway");

	
		archive = new Vector<StateModelEvent>();

		listeners = new Vector<StateChangeListener>();
		addListeners = new Vector<StateChangeListener>();
		deleteListeners = new Vector<StateChangeListener>();

		// bind the raw provider after we have set up our listeners and archives,
		// as sm calls stateChanged as soon as we bind ....
		stateModelMonitor.addStateChangeListener(this);
		
		
		processedCount = 0;
	}
	
	

	/**
	 * @return the processInterval
	 */
	public long getProcessInterval() {
		return processInterval;
	}



	/**
	 * @param processInterval the processInterval to set
	 */
	public void setProcessInterval(long processInterval) {
		this.processInterval = processInterval;
	}



	/**
	 * @return the cullInterval
	 */
	public long getCullInterval() {
		return cullInterval;
	}



	/**
	 * @param cullInterval the cullInterval to set
	 */
	public void setCullInterval(long cullInterval) {
		this.cullInterval = cullInterval;
	}



	/**
	 * @return the backingStoreAgeLimit
	 */
	public long getBackingStoreAgeLimit() {
		return backingStoreAgeLimit;
	}



	/**
	 * @param backingStoreAgeLimit the backingStoreAgeLimit to set
	 */
	public void setBackingStoreAgeLimit(long backingStoreAgeLimit) {
		this.backingStoreAgeLimit = backingStoreAgeLimit;
	}



	/**
	 * @return the backingStore
	 */
	public SecondaryCache getBackingStore() {
		return backingStore;
	}



	/**
	 * @param backingStore the backingStore to set
	 */
	public void setBackingStore(SecondaryCache backingStore) {
		this.backingStore = backingStore;
	}



	private void notifyListenersEventUpdate(StateModelEvent event) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				StateChangeListener l = deleteListeners.get(id);
				if (listeners.contains(l)) {
					listeners.remove(l);
					slogger.create().info().level(2).msg("Removing listener " + l).send();
				}
			}
		}
		deleteListeners.clear();

		// add new listeners
		if (!addListeners.isEmpty()) {
			for (int ia = 0; ia < addListeners.size(); ia++) {
				StateChangeListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}
		addListeners.clear();

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			StateChangeListener l = null;
			try {
				l = listeners.get(il);
				// what sort of event is this one ????????
				if (event instanceof StateChangedEvent) {
					StateChangedEvent sce = (StateChangedEvent)event;
					l.stateChanged(sce.getOldState(), sce.getNewState());						
				}
						
			} catch (Exception e) {
				if (l != null) {
					deleteListeners.add(l);
					slogger.create().info().level(2)
							.msg("Adding unresponsive listener: " + l + " to kill list due to: " + e).send();
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.OperationsEventListener#operationsEventNotification(ngat
	 * .rcs.tmm.events.OperationsEvent)
	 */
	public void stateChanged(IState oldState, IState newState) throws RemoteException {

		StateChangedEvent sce = new StateChangedEvent(System.currentTimeMillis());
		sce.setOldState(oldState);
		sce.setNewState(newState);
		slogger.create().info().level(2).msg("Add event: " + archive.size() + " to archive: " + sce).send();
		archive.add(sce);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.nsm.StateModelArchive#getOperationsHistory(long, long)
	 */
	public List<StateModelEvent> getStateChangeHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
				.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();

		List<StateModelEvent> list = new Vector<StateModelEvent>();

		for (int is = 0; is < archive.size(); is++) {
			StateModelEvent event = archive.get(is);
			long time = event.getStatusTimeStamp();
			if (time >= t1 && time <= t2)
				list.add(event);
		}
		slogger.create().info().level(2).msg("Returning " + list.size() + " entries").send();
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.OperationsMonitor#addOperationsEventListener(ngat.rcs.tmm
	 * .OperationsEventListener)
	 */
	public void addStateChangeListener(StateChangeListener l) throws RemoteException {

		// ignore listener already registered
		if (listeners.contains(l))
			return;

		// note current time

		// find all archived data from now-1 hour to now

		// send data to new listener

		// add new listener to new list
		slogger.create().info().level(2).msg("Received request to add new listener: " + l).send();
		addListeners.add(l);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.OperationsMonitor#removeOperationsEventListener(ngat.rcs
	 * .tmm.OperationsEventListener)
	 */
	public void removeStateChangeListener(StateChangeListener l) throws RemoteException {
		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public void startProcessor() {
		ProcessorThread pt = new ProcessorThread(5000L);
		pt.start();
	}

	private class ProcessorThread extends ControlThread {

		/** Count cycles. */
		private int ipcc = 0;

		private long interval;

		/**
		 * @param interval
		 */
		public ProcessorThread(long interval) {
			super("OPS_G_PT", true);
			this.interval = interval;
		}

		@Override
		protected void initialise() {
			// TODO Auto-generated method stub

		}

		@Override
		protected void mainTask() {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException ix) {
			}

			slogger.create().info().level(3).msg("Processor sweep: " + ipcc).send();

			// Cull aged items
			backingStoreCull();

			// Process pending items
			processPendingStatus();

			ipcc++;

		}

		private void backingStoreCull() {

			// check backing store every so often ci/pi OR 10 whichever is
			// larger
			double ratio = (double) cullInterval / (double) processInterval;
			if (Double.isNaN(ratio) || Double.isInfinite(ratio))
				cullSweepIndicator = 10;
			else
				cullSweepIndicator = Math.max(10, (int) (Math.floor(ratio + 1.0)));

			slogger.create().info().level(3).msg("Sweep indicator set to: " + cullSweepIndicator).send();

			if (ipcc % cullSweepIndicator == 0) {

				List<StateModelEvent> dumpList = new Vector<StateModelEvent>();

				long cutoffTime = System.currentTimeMillis() - backingStoreAgeLimit;
				slogger.create()
						.info()
						.level(3)
						.msg(String.format("Purge to backing store, items dated before: %tF %tT \n", cutoffTime,
								cutoffTime)).send();

				int cullCount = 0;
				// purge oldest data into backingStore.
				for (int is = 0; is < processedCount; is++) {
					StateModelEvent status = archive.get(is);

					if (status.getStatusTimeStamp() < cutoffTime) {
						cullCount++;
						dumpList.add(status);
					}

				}

				slogger.create().info().level(3)
						.msg("Checked " + processedCount + " entries, found " + cullCount + " aged items").send();

				// push the culled data into the backing store
				if (backingStore != null) {
					int ntb = 0;
					for (int is = 0; is < cullCount; is++) {
						StateModelEvent status = archive.get(is);
						try {
							backingStore.storeStatus(status);
							ntb++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					slogger.create().info().level(3)
							.msg("Successfully dumped " + ntb + " of " + cullCount + " items to backing store").send();
				} else {
					slogger.create().info().level(3).msg("No backing store so culled items will be lost").send();
				}

				// chop the culled data out of the local cache
				archive.removeAll(dumpList);

				processedCount -= cullCount;
				int ias = archive.size();
				slogger.create()
						.info()
						.level(3)
						.msg("Removed aged entries from live cache, size now" + ias + ", processing starts at: "
								+ processedCount).send();

			}

		}

		private void processPendingStatus() {
			// loop thro pending events
			int ias = archive.size();
			slogger.create().info().level(2).msg("Processing archived status from: " + processedCount + " to " + ias)
					.send();

			for (int is = processedCount; is < ias; is++) {
				StateModelEvent event = archive.get(is);
				notifyListenersEventUpdate(event);
			}
			// we have processed all known archived status
			processedCount = ias;
		}

		@Override
		protected void shutdown() {
			// TODO Auto-generated method stub

		}

	}

	
	
	
	
	
	
	
}

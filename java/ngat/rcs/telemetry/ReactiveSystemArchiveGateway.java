/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.net.telemetry.SecondaryCache;
import ngat.rcs.ers.CriterionUpdateEvent;
import ngat.rcs.ers.FilterUpdateEvent;
import ngat.rcs.ers.ReactiveEvent;
import ngat.rcs.ers.ReactiveSystemMonitor;
import ngat.rcs.ers.ReactiveSystemUpdateListener;
import ngat.rcs.ers.ReactiveSystemArchive;
import ngat.rcs.ers.RuleUpdateEvent;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class ReactiveSystemArchiveGateway extends UnicastRemoteObject
		implements ReactiveSystemMonitor, ReactiveSystemArchive, ReactiveSystemUpdateListener {

	/** Logger. */
	private LogGenerator slogger;

	private ReactiveSystemMonitor monitor;
	
	/** A list of registered OperationsEventListeners. */
	private List<ReactiveSystemUpdateListener> listeners;

	/** A list of candidate OperationsEventListeners. */
	private List<ReactiveSystemUpdateListener> addListeners;

	/** A list of OperationsEventListeners to delete. */
	private List<ReactiveSystemUpdateListener> deleteListeners;

	private List<ReactiveEvent> archive;
	
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
	 * @param monitor
	 * @throws RemoteException
	 */
	public ReactiveSystemArchiveGateway(ReactiveSystemMonitor monitor)
			throws RemoteException {
		super();
		this.monitor = monitor;
		
		Logger alogger = LogManager.getLogger("ERS"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("OperationsGateway");

		monitor.addReactiveSystemUpdateListener(this);
		archive = new Vector<ReactiveEvent>();

		listeners = new Vector<ReactiveSystemUpdateListener>();
		addListeners = new Vector<ReactiveSystemUpdateListener>();
		deleteListeners = new Vector<ReactiveSystemUpdateListener>();

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


	private void notifyListenersEventUpdate(ReactiveEvent event) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				ReactiveSystemUpdateListener l = deleteListeners.get(id);
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
				ReactiveSystemUpdateListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}
		addListeners.clear();

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			ReactiveSystemUpdateListener l = null;
			try {
				l = listeners.get(il);
				if (event instanceof FilterUpdateEvent) {
					FilterUpdateEvent fev = (FilterUpdateEvent)event;
					l.filterUpdated(fev.getFilterName(), fev.getStatusTimeStamp(), fev.getSensorInput(), fev.getFilterOutput());
				} else if
				(event instanceof CriterionUpdateEvent) {
					CriterionUpdateEvent cev = (CriterionUpdateEvent) event;
					l.criterionUpdated(cev.getCriterionName(), cev.getStatusTimeStamp(), cev.isCriterionOutput());
				} else if
				(event instanceof RuleUpdateEvent) {
					RuleUpdateEvent rev = (RuleUpdateEvent) event;
					l.ruleUpdated(rev.getRuleName(), rev.getStatusTimeStamp(), rev.isRuleTriggered());
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
	 * @see ngat.rcs.tmm.OperationsArchive#getOperationsHistory(long, long)
	 */
	public List<ReactiveEvent> getReactiveSystemHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
				.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();

		List<ReactiveEvent> list = new Vector<ReactiveEvent>();

		for (int is = 0; is < archive.size(); is++) {
			ReactiveEvent event = archive.get(is);
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
	public void addReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {

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
	public void removeReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {
		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	
	
	

	public void filterUpdated(String filterName, long time, Number updateValue,
			Number filterOutputValue) throws RemoteException {
	    FilterUpdateEvent fev = new FilterUpdateEvent(time, filterName, updateValue, filterOutputValue);
	    archive.add(fev);
		
	}

	public void criterionUpdated(String critName, long time,
			boolean critOutputValue) throws RemoteException {
	    CriterionUpdateEvent cev = new CriterionUpdateEvent(time, critName, critOutputValue);
	    archive.add(cev);
		
	}

	public void ruleUpdated(String ruleName, long time, boolean ruleOutputValue)
			throws RemoteException {
	    RuleUpdateEvent rev = new RuleUpdateEvent(time, ruleName, ruleOutputValue);
	    archive.add(rev);
		
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
			super("ERS_G_PT", true);
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

				List<ReactiveEvent> dumpList = new Vector<ReactiveEvent>();

				long cutoffTime = System.currentTimeMillis() - backingStoreAgeLimit;
				slogger.create()
						.info()
						.level(3)
						.msg(String.format("Purge to backing store, items dated before: %tF %tT \n", cutoffTime,
								cutoffTime)).send();

				int cullCount = 0;
				// purge oldest data into backingStore.
				for (int is = 0; is < processedCount; is++) {
					ReactiveEvent status = archive.get(is);

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
						ReactiveEvent status = archive.get(is);
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
				ReactiveEvent event = archive.get(is);
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

package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.net.telemetry.SecondaryCache;
import ngat.rcs.tms.TaskArchive;
import ngat.rcs.tms.TaskLifecycleListener;
import ngat.rcs.tms.TaskMonitor;
import ngat.rcs.tms.events.TaskLifecycleEvent;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

public class TaskArchiveGateway extends UnicastRemoteObject implements TaskMonitor, TaskArchive, TaskLifecycleListener {

	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered TaskLifecycleEventListeners. */
	private List<TaskLifecycleListener> listeners;

	/** A list of candidate TaskLifecycleEventListeners. */
	private List<TaskLifecycleListener> addListeners;

	/** A list of TaskLifecycleEventListeners to delete. */
	private List<TaskLifecycleListener> deleteListeners;

	private List<TaskLifecycleEvent> archive;

	private TaskMonitor tmon;
	
	/**
	 * Counts the number of archive entries which have been forwarded to current
	 * listeners.
	 */
	private volatile int processedCount;

	// START TEMPLATE CODE
	
    /** Processor cycle interval. Default to 10 sec.*/
    private long processInterval = 10*1000L;

    /** How often do we check for culling. Default to 30 minutes.*/
    private long cullInterval = 30*60*1000L;
	
    /** Age of oldest data to keep in local cache. Default to 1 hour. */
    private long backingStoreAgeLimit = 60*60*1000L;
	
    /** How often relative to process sweep do we check for culling. Default to every 10 sweeps.*/
    private int cullSweepIndicator = 10;

    /** the secondary cache. */
    private SecondaryCache backingStore;

    // END TEMPLATE CODE
	
	
	public TaskArchiveGateway(TaskMonitor tmon) throws RemoteException {
		super();
		this.tmon = tmon;
		Logger alogger = LogManager.getLogger("TASK"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("TaskMonitorGateway");

		tmon.addTaskEventListener(this);
		
		archive = new Vector<TaskLifecycleEvent>();

		listeners = new Vector<TaskLifecycleListener>();
		addListeners = new Vector<TaskLifecycleListener>();
		deleteListeners = new Vector<TaskLifecycleListener>();

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




	private void notifyListenersEventUpdate(TaskLifecycleEvent event) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				TaskLifecycleListener l = deleteListeners.get(id);
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
				TaskLifecycleListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}
		addListeners.clear();

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			TaskLifecycleListener l = null;
			try {
				l = listeners.get(il);
				l.taskLifecycleEventNotification(event);			
			} catch (Exception e) {
				if (l != null) {
					deleteListeners.add(l);
					slogger.create().info().level(2).msg("Adding unresponsive listener: " + l + " to kill list due to: "+e).send();
				}
			}
		}

	}

	public List<TaskLifecycleEvent> getTaskLifecycleHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
				.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();

		List<TaskLifecycleEvent> list = new Vector<TaskLifecycleEvent>();

		for (int is = 0; is < archive.size(); is++) {
			TaskLifecycleEvent event = archive.get(is);
			long time = event.getEventTimeStamp();
			if (time >= t1 && time <= t2)
				list.add(event);
		}
		slogger.create().info().level(2).msg("Returning " + list.size() + " entries").send();
		return list;
	}

	public void addTaskEventListener(TaskLifecycleListener l) throws RemoteException {

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

	public void removeTaskEventListener(TaskLifecycleListener l) throws RemoteException {

		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public void startProcessor() {
		ProcessorThread pt = new ProcessorThread();
		pt.start();
	}

	private class ProcessorThread extends ControlThread {

		/** Count cycles. */
		private int ipcc = 0;
		
		/**
		 * @param interval
		 */
		public ProcessorThread() {
			super("TLA_G_PT", true);			
		}

		@Override
		protected void initialise() {
			// TODO Auto-generated method stub

		}

		@Override
		protected void mainTask() {
			try {
				Thread.sleep(processInterval);
			} catch (InterruptedException ix) {
			}
			
			  slogger.create().info().level(3).msg("Processor sweep: "+ipcc).send();
				
			    // Cull aged items
			    backingStoreCull();
					
			    // Process pending items		
			    processPendingStatus();
					
			    ipcc++;
			
		}
		private void backingStoreCull() {
			
		    // check backing store every so often ci/pi OR 10 whichever is larger
		    double ratio = (double)cullInterval / (double)processInterval;
		    if (Double.isNaN(ratio) || Double.isInfinite(ratio))
			cullSweepIndicator = 10;
		    else
			cullSweepIndicator = Math.max(10, (int)(Math.floor(ratio +1.0)));
				
		    slogger.create().info().level(3).msg("Sweep indicator set to: "+cullSweepIndicator).send();
				
		    if (ipcc % cullSweepIndicator == 0) {
					
			List<TaskLifecycleEvent > dumpList = new Vector<TaskLifecycleEvent >();
					
			long cutoffTime = System.currentTimeMillis() - backingStoreAgeLimit;
			slogger.create().info().level(3)
			    .msg(String.format("Purge to backing store, items dated before: %tF %tT \n", 
					       cutoffTime, cutoffTime)).send();
						
			int cullCount = 0;
			// purge oldest data into backingStore.
			for (int is = 0; is < processedCount; is++) {
			    TaskLifecycleEvent  status = archive.get(is);
						
			    if (status.getEventTimeStamp() < cutoffTime) {
				cullCount++;
				dumpList.add(status);
			    }

			}

			slogger.create().info().level(3)
			    .msg("Checked "+processedCount+" entries, found "+cullCount+" aged items").send();
					
			// push the culled data into the backing store
			if (backingStore != null) {
			    int ntb = 0;
			    for (int is = 0; is < cullCount; is++) {
				TaskLifecycleEvent  status = archive.get(is);
				try {
				    backingStore.storeStatus(status);
				    ntb++;
				} catch (Exception e) {
				    e.printStackTrace();
				}
			    }
			    slogger.create().info().level(3)
				.msg("Successfully dumped "+ntb+" of "+cullCount+" items to backing store").send();
			} else {
			    slogger.create().info().level(3)
				.msg("No backing store so culled items will be lost").send();
			}

			// chop the culled data out of the local cache	
			archive.removeAll(dumpList);			
					
			processedCount -= cullCount;	
			int ias = archive.size();
			slogger.create().info().level(3)
			    .msg("Removed aged entries from live cache, size now"+ias+", processing starts at: "+processedCount).send();
				
					
		    }

		}
			
		private void processPendingStatus() {
		    int ias = archive.size();
		    slogger.create().info().level(2).msg("Processing archived status from: " + processedCount + " to " + ias)
			.send();

		    for (int is = processedCount; is < ias; is++) {
			TaskLifecycleEvent status = archive.get(is);			
			notifyListenersEventUpdate(status);
		    }
		    // we have processed all known archived status
		    processedCount = ias;
		}

		@Override
		protected void shutdown() {
			// TODO Auto-generated method stub

		}

	}

	public void taskLifecycleEventNotification(TaskLifecycleEvent event) throws RemoteException {		
		slogger.create().info().level(2).msg("Add event: " + archive.size() + " to archive: " + event).send();
		archive.add(event);
	}

}
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusArchive;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatusUpdateListener;
import ngat.icm.InstrumentStatusProvider;
import ngat.net.telemetry.SecondaryCache;
import ngat.phase2.IInstrumentConfig;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

public class InstrumentArchiveGateway extends UnicastRemoteObject implements InstrumentStatusUpdateListener,
									     InstrumentStatusProvider, InstrumentStatusArchive {

    /** Logger. */
    private LogGenerator slogger;

    /** A list of registered InstrumentStatusUpdateListeners. */
    private List<InstrumentStatusUpdateListener> listeners;

    /** A list of candidate InstrumentStatusUpdateListeners. */
    private List<InstrumentStatusUpdateListener> addListeners;

    /** A list of InstrumentStatusUpdateListeners to delete. */
    private List<InstrumentStatusUpdateListener> deleteListeners;

    /** Instrument status source. */
    private InstrumentRegistry ireg;

    private List<InstrumentStatus> archive;

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
	
    /**
     * Create an InstrumentArchiveGateway
     * 
     * @param isp
     *            The status provider.
     * @throws RemoteException
     */
    public InstrumentArchiveGateway(InstrumentRegistry ireg) throws RemoteException {
	super();
	this.ireg = ireg;

	Logger alogger = LogManager.getLogger("ICM"); // probably should be
	// RCS.Telem
	slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
	    .srcCompId("InstrumentGateway");

	List insts = ireg.listInstruments();
	for (int ii = 0; ii < insts.size(); ii++) {
	    InstrumentDescriptor instId = (InstrumentDescriptor) insts.get(ii);
	    InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
	    isp.addInstrumentStatusUpdateListener(this);
	}

	archive = new Vector<InstrumentStatus>();
		
	listeners = new Vector<InstrumentStatusUpdateListener>();
	addListeners = new Vector<InstrumentStatusUpdateListener>();
	deleteListeners = new Vector<InstrumentStatusUpdateListener>();

	processedCount = 0;
    }

    public void addInstrumentStatusUpdateListener(InstrumentStatusUpdateListener l) throws RemoteException {

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

    public IInstrumentConfig getCurrentConfig() throws RemoteException {
	// return isp.getCurrentConfig();
	return null; // OR THROW UNSuportedException !
    }

    public InstrumentStatus getStatus() throws RemoteException {
	// return isp.getStatus();
	return null; // OR THROW UNSuportedException !
    }

    /**
     * @return the processInterval
     */
    public long getProcessInterval() {
	return processInterval;
    }

    /**
     * @param processInterval
     *            the processInterval to set
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
     * @param backingStoreAgeLimit
     *            the backingStoreAgeLimit to set
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
     * @param backingStore
     *            the backingStore to set
     */
    public void setBackingStore(SecondaryCache backingStore) {
	this.backingStore = backingStore;
    }

    public void removeInstrumentStatusUpdateListener(InstrumentStatusUpdateListener l) throws RemoteException {

	if (!listeners.contains(l))
	    return;

	// add to kill list
	slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
	deleteListeners.add(l);

    }

    public void instrumentStatusUpdated(InstrumentStatus status) throws RemoteException {
	slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + status).send();
	//status.setInstrumentName(instId.getInstrumentName());
	archive.add(status);
	//System.err.println("Status update for "+instId.getInstrumentName());
    }

    public List<InstrumentStatus> getInstrumentStatusHistory(long t1, long t2) throws RemoteException {
	slogger.create().info().level(2)
	    .msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();
	List<InstrumentStatus> list = new Vector<InstrumentStatus>();

	for (int is = 0; is < archive.size(); is++) {
	    InstrumentStatus status = archive.get(is);
	    long time = status.getStatusTimeStamp();
	    if (time >= t1 && time <= t2)
		list.add(status);
	}
	slogger.create().info().level(2).msg("Returning " + list.size() + " entries").send();
	return list;
    }

    private void notifyListenersInstrumentStatusUpdate(InstrumentStatus status) {
	// remove any kill items
	if (!deleteListeners.isEmpty()) {
	    for (int id = 0; id < deleteListeners.size(); id++) {
		InstrumentStatusUpdateListener l = deleteListeners.get(id);
		if (listeners.contains(l)) {
		    listeners.remove(l);
		    slogger.create().info().level(2).msg("Removing listener " + l).send();
		}
	    }
	}

	// add new listeners
	if (!addListeners.isEmpty()) {
	    for (int ia = 0; ia < addListeners.size(); ia++) {
		InstrumentStatusUpdateListener l = addListeners.get(ia);
		if (!listeners.contains(l)) {
		    listeners.add(l);
		    slogger.create().info().level(2).msg("Adding new listener " + l).send();
		}
	    }

	}

	// broadcast
	for (int il = 0; il < listeners.size(); il++) {
	    InstrumentStatusUpdateListener l = null;
	    try {
		l = listeners.get(il);
		l.instrumentStatusUpdated(status);
	    } catch (Exception e) {
		if (l != null) {
		    deleteListeners.add(l);
		    slogger.create().info().level(2).msg("Adding unresponsive listener: " + l + " to kill list").send();
		}
	    }
	}

    }

    public void startProcessor() {
	ProcessorThread pt = new ProcessorThread();
	pt.start();
    }

    private class ProcessorThread extends ControlThread {

	/** Count cycles. */
	private int ipcc = 0;

	/*
	 * Create a ProcessorThread.
	 */
	public ProcessorThread() {
	    super("ICM_G_PT", true);
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
				
		List<InstrumentStatus> dumpList = new Vector<InstrumentStatus>();
				
		long cutoffTime = System.currentTimeMillis() - backingStoreAgeLimit;
		slogger.create().info().level(3)
		    .msg(String.format("Purge to backing store, items dated before: %tF %tT \n", 
				       cutoffTime, cutoffTime)).send();
					
		int cullCount = 0;
		// purge oldest data into backingStore.
		for (int is = 0; is < processedCount; is++) {
		    InstrumentStatus status = archive.get(is);
					
		    if (status.getStatusTimeStamp() < cutoffTime) {
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
			InstrumentStatus status = archive.get(is);
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
		InstrumentStatus status = archive.get(is);			
		notifyListenersInstrumentStatusUpdate(status);
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

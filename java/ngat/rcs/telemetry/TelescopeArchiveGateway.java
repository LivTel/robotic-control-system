/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusArchive;
import ngat.tcm.TelescopeStatusProvider;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class TelescopeArchiveGateway extends UnicastRemoteObject implements TelescopeStatusUpdateListener,
		TelescopeStatusProvider, TelescopeStatusArchive {

	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered TelescopeStatusUpdateListeners. */
	private List<TelescopeStatusUpdateListener> listeners;

	/** A list of candidate TelescopeStatusUpdateListeners. */
	private List<TelescopeStatusUpdateListener> addListeners;

	/** A list of TelescopeStatusUpdateListeners to delete. */
	private List<TelescopeStatusUpdateListener> deleteListeners;

	/** Telescope status source. */
	private TelescopeStatusProvider tsp;

	private List<TelescopeStatus> archive;
	
	/** Counts the number of archive entries which have been forwarded to current listeners.*/
	private int processedCount;

	/**
	 * Create a TelescopeArchiveGateway.
	 * 
	 * @throws RemoteException
	 */
	public TelescopeArchiveGateway(TelescopeStatusProvider tsp) throws RemoteException {
		super();
		this.tsp = tsp;

		Logger alogger = LogManager.getLogger("TCM"); // probably should be
														// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("TCM_Gateway");

		tsp.addTelescopeStatusUpdateListener(this);
		
		archive = new Vector<TelescopeStatus>();
		
		listeners = new Vector<TelescopeStatusUpdateListener>();
		addListeners = new Vector<TelescopeStatusUpdateListener>();
		deleteListeners = new Vector<TelescopeStatusUpdateListener>();
		
		processedCount = 0;
	}

	public void addTelescopeStatusUpdateListener(TelescopeStatusUpdateListener l) throws RemoteException {

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

	public void removeTelescopeStatusUpdateListener(TelescopeStatusUpdateListener l) throws RemoteException {

		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public void telescopeStatusUpdate(TelescopeStatus status) throws RemoteException {
		slogger.create().info().level(2)
			.msg("Add status update: "+archive.size()+" to archive: "+status)
			.send();
		archive.add(status);
	}

	private void notifyListenersTelescopeStatusUpdate(TelescopeStatus status) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				TelescopeStatusUpdateListener l = deleteListeners.get(id);
				if (listeners.contains(l)) {
					listeners.remove(l);
					slogger.create().info().level(2).msg("Removing listener " + l).send();
				}
			}
		}

		// add new listeners
		if (!addListeners.isEmpty()) {
			for (int ia = 0; ia < addListeners.size(); ia++) {
				TelescopeStatusUpdateListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			TelescopeStatusUpdateListener l = null;
			try {
				l = listeners.get(il);
				l.telescopeStatusUpdate(status);
			} catch (Exception e) {
				if (l != null) {
					deleteListeners.add(l);
					slogger.create().info().level(2)
						.msg("Adding unresponsive listener: " + l + " to kill list")
						.send();
				}
			}
		}

	}
	
	public List<TelescopeStatus> getTelescopeStatusHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
			.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1,t1,t2,t2))
			.send();
		List<TelescopeStatus> list = new Vector<TelescopeStatus>();
		
		for (int is = 0; is < archive.size(); is++) {
			TelescopeStatus status = archive.get(is);
			long time = status.getStatusTimeStamp();
			if (time >= t1 && time <= t2)
				list.add(status);
		}
		slogger.create().info().level(2)
			.msg("Returning "+list.size()+" entries")
			.send();
		return list;
	}

	public void startProcessor() {
		ProcessorThread pt = new ProcessorThread(5000L);
		pt.start();
	}
	
	private class ProcessorThread extends ControlThread {

		private long interval;

		/**
		 * @param interval
		 */
		public ProcessorThread(long interval) {
			super("TCM_G_PT", true);
			this.interval = interval;
		}

		@Override
		protected void initialise() {
			// TODO Auto-generated method stub

		}

		@Override
		protected void mainTask() {
					try {Thread.sleep(interval); } catch (InterruptedException ix) {}	
				// loop thro pending statii
				int ias = archive.size();		
				slogger.create().info().level(2)
				.msg("Processing archived status from: "+processedCount+" to "+ias).send();
			
				for (int is = processedCount; is < ias; is++) {
					TelescopeStatus status = archive.get(is);				
					notifyListenersTelescopeStatusUpdate(status);
				}
				// we have processed all known archived status
				processedCount = ias;
		}

		@Override
		protected void shutdown() {
			// TODO Auto-generated method stub

		}

	}

	public void telescopeNetworkFailure(long time, String message) throws RemoteException {
		// TODO need a way to store TCS comms failure notifications ie a TelStatus for CommsFailure
		// or just a telstatus with a comms flag set to offline ??
		slogger.create().info().level(2)
		.msg("TODO Add comms status update: "+archive.size()+" to archive: "+message)
		.send();
	}

	
	
}

/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusArchive;
import ngat.ems.MeteorologyStatusProvider;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class MeteorologyArchiveGateway extends UnicastRemoteObject implements MeteorologyStatusUpdateListener,
		MeteorologyStatusProvider, MeteorologyStatusArchive {

    private int bcscount = 0;

	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered MeteorologyStatusUpdateListeners. */
	private List<MeteorologyStatusUpdateListener> listeners;

	/** A list of candidate MeteorologyStatusUpdateListeners. */
	private List<MeteorologyStatusUpdateListener> addListeners;

	/** A list of MeteorologyStatusUpdateListeners to delete. */
	private List<MeteorologyStatusUpdateListener> deleteListeners;

	/** Meteorology status source. */
	private MeteorologyStatusProvider tsp;

	private List<MeteorologyStatus> archive;
	
	/** Counts the number of archive entries which have been forwarded to current listeners.*/
	private int processedCount;

	/**
	 * Create a MeteorologyArchiveGateway.
	 * 
	 * @throws RemoteException
	 */
	public MeteorologyArchiveGateway(MeteorologyStatusProvider tsp) throws RemoteException {
		super();
		this.tsp = tsp;

		Logger alogger = LogManager.getLogger("EMS"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("Telemetry")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId("EMS_Gateway");

		tsp.addMeteorologyStatusUpdateListener(this);
		
		archive = new Vector<MeteorologyStatus>();
		
		listeners = new Vector<MeteorologyStatusUpdateListener>();
		addListeners = new Vector<MeteorologyStatusUpdateListener>();
		deleteListeners = new Vector<MeteorologyStatusUpdateListener>();
		
		processedCount = 0;
	}

	public void addMeteorologyStatusUpdateListener(MeteorologyStatusUpdateListener l) throws RemoteException {

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

	public void removeMeteorologyStatusUpdateListener(MeteorologyStatusUpdateListener l) throws RemoteException {

 		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public void meteorologyStatusUpdate(MeteorologyStatus status) throws RemoteException {
		slogger.create().info().level(2)
		    .msg(String.format("Add status update: %4d to archive: %tF %tT %s\n",
				       archive.size(),
				       status.getStatusTimeStamp(),
				       status.getStatusTimeStamp(),
				       status))
		    .send();
		archive.add(status);

		// every 5th BCS input we dump all data out
		/*if (status instanceof CloudStatus) {

		    slogger.create().info().level(2)
			.msg("Dumping cloud statii").send();
		    
		    for (int is = 0; is < archive.size(); is++) {
                        MeteorologyStatus astatus = archive.get(is);
			if (astatus instanceof CloudStatus) {
			    long time = astatus.getStatusTimeStamp();
			    slogger.create().info().level(2)
				.msg(String.format("Checking archived bcs status: %6d of %6d %tF% tT: %s",
						   is,
						   archive.size(),
						   time,
						   time,
						   astatus))
				.send();
			}
		    }
		    
		}*/
		
		
	}
    
	private void notifyListenersMeteorologyStatusUpdate(MeteorologyStatus status) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				MeteorologyStatusUpdateListener l = deleteListeners.get(id);
				if (listeners.contains(l)) {
					listeners.remove(l);
					slogger.create().info().level(2).msg("Removing listener " + l).send();
				}
			}
		}

		// add new listeners
		if (!addListeners.isEmpty()) {
			for (int ia = 0; ia < addListeners.size(); ia++) {
				MeteorologyStatusUpdateListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			MeteorologyStatusUpdateListener l = null;
			try {
				l = listeners.get(il);
				l.meteorologyStatusUpdate(status);
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
	
	public List<MeteorologyStatus> getMeteorologyStatusHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
			.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1,t1,t2,t2))
			.send();
		List<MeteorologyStatus> list = new Vector<MeteorologyStatus>();
		
		slogger.create().info().level(2)
			.msg("Archive contains: "+archive.size()+" entries")
			.send();
		
		for (int is = 0; is < archive.size(); is++) {
			MeteorologyStatus status = archive.get(is);
			long time = status.getStatusTimeStamp();
			slogger.create().info().level(2) 
				.msg(String.format("Checking archived status: %6d of %6d %tF%tT ",is,archive.size(),time,time))
				.send();
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
			super("ECM_G_PT", true);
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
					MeteorologyStatus status = archive.get(is);				
					notifyListenersMeteorologyStatusUpdate(status);
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

/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.ems.SkyModelArchive;
import ngat.ems.SkyModelExtinctionUpdate;
import ngat.ems.SkyModelMonitor;
import ngat.ems.SkyModelSeeingUpdate;
import ngat.ems.SkyModelUpdate;
import ngat.ems.SkyModelUpdateListener;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class SkyModelArchiveGateway extends UnicastRemoteObject implements SkyModelMonitor, SkyModelUpdateListener,
		SkyModelArchive {

	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered SkyModlUpdateListeners. */
	private List<SkyModelUpdateListener> listeners;

	/** A list of candidate SkyModelUpdateListeners. */
	private List<SkyModelUpdateListener> addListeners;

	/** A list of SkyModlUpdateListeners to delete. */
	private List<SkyModelUpdateListener> deleteListeners;

	private SkyModelMonitor sky;

	private List<SkyModelUpdate> archive;

	/**
	 * Counts the number of archive entries which have been forwarded to current
	 * listeners.
	 */
	private int processedCount;

	public SkyModelArchiveGateway(SkyModelMonitor sky) throws RemoteException {
		super();

		Logger alogger = LogManager.getLogger("EMS"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("EMS_Gateway");

		sky.addSkyModelUpdateListener(this);

		archive = new Vector<SkyModelUpdate>();

		listeners = new Vector<SkyModelUpdateListener>();
		addListeners = new Vector<SkyModelUpdateListener>();
		deleteListeners = new Vector<SkyModelUpdateListener>();

		processedCount = 0;
	}

	public void seeingUpdated(long time, double rawSeeing, double correctedSeeing, double prediction, double alt,
			double azm, double wav, boolean standard, String source, String targetName) throws RemoteException {
		SkyModelSeeingUpdate status = new SkyModelSeeingUpdate(time, rawSeeing, correctedSeeing, prediction, standard,
				source);
		status.setAzimuth(azm);
		status.setElevation(alt);
		status.setWavelength(wav);
		status.setTargetName(targetName);
		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + status).send();
		archive.add(status);
	}

	public void extinctionUpdated(long time, double ext) throws RemoteException {
		SkyModelExtinctionUpdate status = new SkyModelExtinctionUpdate(time, ext);
		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + status).send();
		archive.add(status);
	}

	public void addSkyModelUpdateListener(SkyModelUpdateListener l) throws RemoteException {
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

	public void removeSkyModelUpdateListener(SkyModelUpdateListener l) throws RemoteException {
		// TODO Auto-generated method stub
		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public List<SkyModelUpdate> getSkyModelHistory(long t1, long t2) throws RemoteException {
		slogger.create().info().level(2)
				.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();
		List<SkyModelUpdate> list = new Vector<SkyModelUpdate>();

		for (int is = 0; is < archive.size(); is++) {
			SkyModelUpdate status = archive.get(is);
			long time = status.getStatusTimeStamp();
			if (time >= t1 && time <= t2)
				list.add(status);
		}
		slogger.create().info().level(2).msg("Returning " + list.size() + " entries").send();
		return list;
	}

	private void notifyListenersSkyModelUpdate(SkyModelUpdate status) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				SkyModelUpdateListener l = deleteListeners.get(id);
				if (listeners.contains(l)) {
					listeners.remove(l);
					slogger.create().info().level(2).msg("Removing listener " + l).send();
				}
			}
		}

		// add new listeners
		if (!addListeners.isEmpty()) {
			for (int ia = 0; ia < addListeners.size(); ia++) {
				SkyModelUpdateListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			SkyModelUpdateListener l = null;
			try {
				l = listeners.get(il);
				if (status instanceof SkyModelSeeingUpdate) {
					SkyModelSeeingUpdate seeing = (SkyModelSeeingUpdate) status;
					l.seeingUpdated(seeing.getStatusTimeStamp(), seeing.getRawSeeing(), seeing.getCorrectedSeeing(),
							seeing.getPredictedSeeing(), seeing.getElevation(), seeing.getAzimuth(),
							seeing.getWavelength(), seeing.isStandard(), seeing.getSource(), seeing.getTargetName());
				} else if (status instanceof SkyModelExtinctionUpdate) {
					SkyModelExtinctionUpdate photom = (SkyModelExtinctionUpdate) status;
					l.extinctionUpdated(photom.getStatusTimeStamp(), photom.getExtinction());
				}

			} catch (Exception e) {
				if (l != null) {
					deleteListeners.add(l);
					slogger.create().info().level(2).msg("Adding unresponsive listener: " + l + " to kill list").send();
				}
			}
		}

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
			super("EMS_G_PT", true);
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
			// loop thro pending statii
			int ias = archive.size();
			slogger.create().info().level(2).msg("Processing archived status from: " + processedCount + " to " + ias)
					.send();

			for (int is = processedCount; is < ias; is++) {
				SkyModelUpdate status = archive.get(is);
				notifyListenersSkyModelUpdate(status);
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

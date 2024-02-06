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
package ngat.rcs.scm.collation;

import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.ems.WmsStatus;
import ngat.message.RCS_TCS.*;
import ngat.tcm.AstrometryData;
import ngat.tcm.AutoguiderStatus;
import ngat.tcm.AuxilliaryMechanismStatus;
import ngat.tcm.FocusStatus;
import ngat.tcm.SourceData;
import ngat.tcm.TimeData;
import ngat.tcm.PrimaryAxisStatus;
import ngat.tcm.RotatorAxisStatus;
import ngat.tcm.TelescopeControlSystemStatus;
import ngat.tcm.TelescopeEnvironmentStatus;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.util.logging.*;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Acts as the globally accessable resource for obtaining the TCS status
 * information. When an status object arrives (i.e. updates the stored status
 * value) any Observers of the specified segment are notified. Each segment has
 * an update time associated. <br>
 * <br>
 * $IdS
 */
public class StatusPool extends Observable implements TelescopeStatusUpdateListener, MeteorologyStatusUpdateListener {

	/**
	 * Event Constant: Signifies that an update of the TCS_Status ASTROMETRY
	 * segment has occurred.
	 */
	public static final int ASTROMETRY_UPDATE_EVENT = 0;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status AUTOGUIDER
	 * segment has occurred.
	 */
	public static final int AUTOGUIDER_UPDATE_EVENT = 1;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status CALIBRATE
	 * segment has occurred.
	 */
	public static final int CALIBRATE_UPDATE_EVENT = 2;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status FOCAL_STATION
	 * segment has occurred.
	 */
	public static final int FOCAL_STATION_UPDATE_EVENT = 3;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status LIMITS segment
	 * has occurred.
	 */
	public static final int LIMITS_UPDATE_EVENT = 4;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status MECHANISMS
	 * segment has occurred.
	 */
	public static final int MECHANISMS_UPDATE_EVENT = 5;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status METEOROLOGY
	 * segment has occurred.
	 */
	public static final int METEOROLOGY_UPDATE_EVENT = 6;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status SOURCE segment
	 * has occurred.
	 */
	public static final int SOURCE_UPDATE_EVENT = 7;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status SERVICES
	 * segment has occurred.
	 */
	public static final int SERVICES_UPDATE_EVENT = 8;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status STATE segment
	 * has occurred.
	 */
	public static final int STATE_UPDATE_EVENT = 9;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status TIME segment
	 * has occurred.
	 */
	public static final int TIME_UPDATE_EVENT = 10;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status VERSION
	 * segment has occurred.
	 */
	public static final int VERSION_UPDATE_EVENT = 11;

	/**
	 * Event Constant: Signifies that an update of the TCS_Status NETWORK
	 * segment has occurred.
	 */
	public static final int NETWORK_UPDATE_EVENT = 12;

	/** The internal storage structure. */
	private static TCS_Status status;

	/** The singleton instance of StatusPool. */
	private static StatusPool instance = null;

	private static Map cats = new HashMap();

	protected Vector[] observers;

	protected static Logger logger = null;

	/** Private constructor for singleton instance. */
	private StatusPool() {
		super();
		status = new TCS_Status();
		observers = new Vector[13];
		for (int i = 0; i < 13; i++) {
			observers[i] = new Vector();
		}
	}

	/** Static initializer for singleton. */
	public static void initialize(int maxSize) {
		if (instance == null)
			instance = new StatusPool();
		logger = LogManager.getLogger("STATUS");
	}

	/** @return The singleton instance or null if not defined. */
	public static StatusPool getInstance() {
		return instance;
	}

	/** @return The latest (i.e. most recently deposited) TCS_Status. */
	public static TCS_Status latest() {
		return status;
	}

	/** @return The last but n deposited TCS_Status. ## DEPRECIATED ## */
	public static TCS_Status lastBut(int n) {
		return (status.copy());
	}

	/** @return The size of the underlying storage structure. */
	public static int getSize() {
		return 1;
	}

	/**
	 * Insert a TCS_Status.
	 * 
	 * @param update
	 *            The TCS_Status.Segment to insert.
	 */
	public static synchronized void insert(TCS_Status.Segment update) {
		// The current status needs updating using the relevant parts of the
		// recieved status.
		// But note that the times for the diferent parts will be unsynched.

		int updateCode = -1;

		// Store it for now.
		logger.log(1, "StatusPool", "", "insert", "Updating with a : " + update.getClass().getName() + ", Content:"
				+ update.toString());

		if (update instanceof TCS_Status.Astrometry) {
			updateCode = ASTROMETRY_UPDATE_EVENT;
			insertAstrometry((TCS_Status.Astrometry) update);
		} else if (update instanceof TCS_Status.Autoguider) {
			updateCode = AUTOGUIDER_UPDATE_EVENT;
			insertAutoguider((TCS_Status.Autoguider) update);
		} else if (update instanceof TCS_Status.Calibrate) {
			updateCode = CALIBRATE_UPDATE_EVENT;
			insertCalibrate((TCS_Status.Calibrate) update);
		} else if (update instanceof TCS_Status.FocalStation) {
			updateCode = FOCAL_STATION_UPDATE_EVENT;
			insertFocus((TCS_Status.FocalStation) update);
		} else if (update instanceof TCS_Status.Limits) {
			// System.err.println("StatusPool::Received LIMITS segment");
			updateCode = LIMITS_UPDATE_EVENT;
			insertLimits((TCS_Status.Limits) update);
		} else if (update instanceof TCS_Status.Mechanisms) {
			updateCode = MECHANISMS_UPDATE_EVENT;
			insertMechanisms((TCS_Status.Mechanisms) update);
		} else if (update instanceof TCS_Status.Meteorology) {
			// System.err.println("Tested OK as a MetSegment.");
			updateCode = METEOROLOGY_UPDATE_EVENT;
			insertMeteorology((TCS_Status.Meteorology) update);
		} else if (update instanceof TCS_Status.SourceBlock) {
			updateCode = SOURCE_UPDATE_EVENT;
			insertSource((TCS_Status.SourceBlock) update);
		} else if (update instanceof TCS_Status.Services) {
			updateCode = SERVICES_UPDATE_EVENT;
			insertState((TCS_Status.State) update);
		} else if (update instanceof TCS_Status.State) {
			updateCode = STATE_UPDATE_EVENT;
			insertState((TCS_Status.State) update);
		} else if (update instanceof TCS_Status.Time) {
			updateCode = TIME_UPDATE_EVENT;
			insertTime((TCS_Status.Time) update);
		} else if (update instanceof TCS_Status.Version) {
			updateCode = VERSION_UPDATE_EVENT;
			insertVersion((TCS_Status.Version) update);
		} else if (update instanceof TCS_Status.Network) {
			updateCode = NETWORK_UPDATE_EVENT;
			insertNetwork((TCS_Status.Network) update);
		} else
			return;
		// instance.status = (TCS_Status)(status.copy());

		// long s = System.currentTimeMillis();
		// ## WE SEND THE SEGMENT ONLY ALSO TEMP set the timestamp here also
		// ######!!!!!
		update.timeStamp = System.currentTimeMillis();
		// System.err.println("Notifing Observers: "+updateCode);
		instance.notifyObservers(updateCode, update);

		// System.err.println("Notified Observers: "+updateCode);
		// long t = System.currentTimeMillis() - s;

	}

	/**
	 * Registers the specified object with the singleton as an Observer. If the
	 * object is already registered this call has no effect.
	 * 
	 * @param observer
	 *            The object to register as Observer.
	 */
	public static void register(Observer observer, int updateCode) {
		logger.log(1, "StatusPool", "", "register", "Registered Observer: " + observer + " for update-code: "
				+ updateCode);
		instance.addObserver(observer, updateCode);
		// log("There are now: "+instance.countObservers(updateCode)+" Observers:");
	}

	/**
	 * De-registers the specified object with the singleton as an Observer. If
	 * the object is not registered the effect is not specified in the
	 * documentation for java.util.Observable.
	 * 
	 * @param observer
	 *            The object to de-register as Observer.
	 */
	public static void unregister(Observer observer, int updateCode) {
		instance.deleteObserver(observer, updateCode);
	}

	/** De-registers <b>all</b> registered Observers of the singleton. */
	public static void clear(int updateCode) {
		instance.deleteObservers(updateCode);
	}

	/**
	 * De-registers the specified object with the singleton against all updates
	 * to which it is registered.
	 */
	public static void unregisterAll(Observer observer) {
		instance.removeRegistrations(observer);
	}

	/**
	 * Adds the specified Observer to the list of Observers which should be
	 * notified when a specified update occurs. If the code is out of the range
	 * of valid codes this method returns silently. If the Observer is already
	 * registered for the specified code no action is taken.
	 * 
	 * @param observer
	 *            The Observer to register.
	 * @param updateCode
	 *            The update code to register against.
	 */
	protected void addObserver(Observer observer, int updateCode) {
		if (updateCode < 0 || updateCode > 12)
			return; // silently.
		if (observers[updateCode].contains(observer))
			return; // silently.
		observers[updateCode].add(observer);
	}

	/**
	 * Removes the specified Observer from the list of Observers which should be
	 * notified when a specified update occurs. If the code is out of the range
	 * of valid codes this method returns silently. If the Observer is NOT
	 * registered for the specified code no action is taken.
	 * 
	 * @param observer
	 *            The Observer to register.
	 * @param updateCode
	 *            The update code to remove registration from.
	 */
	protected void deleteObserver(Observer observer, int updateCode) {
		if (updateCode < 0 || updateCode > 12)
			return; // silently.
		if (!(observers[updateCode].contains(observer)))
			return; // silently.
		observers[updateCode].remove(observer);
	}

	/**
	 * Removes ALL Observers from the list of Observers which should be notified
	 * when a specified update occurs. If the code is out of the range of valid
	 * codes this method returns silently. If NO Observers are registered for
	 * the specified code no action is taken.
	 * 
	 * @param updateCode
	 *            The update code to remove ALL registration from.
	 */
	protected void deleteObservers(int updateCode) {
		if (updateCode < 0 || updateCode > 12)
			return; // silently.
		if ((observers[updateCode].isEmpty()))
			return; // silently.
		observers[updateCode].clear();
	}

	/**
	 * Counts the number of Observers of the specified update. If the update
	 * code is out of the range of valid codes this method will return 0 (zero).
	 * 
	 * @param updateCode
	 *            The update code to count Observers for.
	 */
	protected int countObservers(int updateCode) {
		if (updateCode < 0 || updateCode > 12)
			return 0;
		return observers[updateCode].size();
	}

	/**
	 * Passes the notification of an update to any registered Observers. Note:
	 * That the full TCS_Status is passed as the argument, not just the updated
	 * segment.
	 * 
	 * @param updateCode
	 *            The update code for which Observers are to be notified.
	 */
	protected void notifyObservers(int updateCode, TCS_Status.Segment update) {
		Iterator notificants = observers[updateCode].iterator();
		while (notificants.hasNext()) {
			Observer observer = (Observer) notificants.next();
			logger.log(1, "StatusPool", "", "notifyObservers", "About to notify: " + observer);
			// #### Returns the full works - CLONED.
			observer.update(this, latest());
		}
	}

	/**
	 * Counts the number of registrations of the specified Observer against any
	 * update codes.
	 * 
	 * @param observer
	 *            The Observer to count registrations for.
	 */
	protected int countRegistrations(Observer observer) {
		int count = 0;
		for (int i = 0; i < 12; i++) {
			if (observers[i].contains(observer))
				count++;
		}
		return count;
	}

	/**
	 * Removes ALL registrations for the specified Observer against any updates
	 * which may occur. Calls deleteObserver(observer) for all possible update
	 * codes. These fail silently if observer is not registered against the
	 * code.
	 * 
	 * @param observer
	 *            The Observer to de-register.
	 */
	protected void removeRegistrations(Observer observer) {
		for (int i = 0; i < 12; i++) {
			deleteObserver(observer, i);
		}
	}

	private static void insertAstrometry(TCS_Status.Astrometry update) {
		// System.err.println("StatusPool::Inserting the ASTRO stuff");
		status.astrometry.timeStamp = update.timeStamp;

		status.astrometry.refractionPressure = update.refractionPressure;
		status.astrometry.refractionTemperature = update.refractionTemperature;
		status.astrometry.refractionHumidity = update.refractionHumidity;
		status.astrometry.refractionWavelength = update.refractionWavelength;
		status.astrometry.ut1_utc = update.ut1_utc;
		status.astrometry.tdt_utc = update.tdt_utc;
		status.astrometry.polarMotion_X = update.polarMotion_X;
		status.astrometry.polarMotion_Y = update.polarMotion_Y;
		status.astrometry.airmass = update.airmass;
		status.astrometry.agwavelength = update.agwavelength;
	}

	private static void insertAutoguider(TCS_Status.Autoguider update) {
		// System.err.println("StatusPool::Inserting AUTOGUIDER update: "+update);
		status.autoguider.timeStamp = update.timeStamp;

		status.autoguider.agSelected = update.agSelected;
		status.autoguider.agStatus = update.agStatus;
		status.autoguider.agSwState = update.agSwState;

		status.autoguider.agMode = update.agMode;
		status.autoguider.guideStarMagnitude = update.guideStarMagnitude;
		status.autoguider.fwhm = update.fwhm;

		status.autoguider.agMirrorDemand = update.agMirrorDemand;
		status.autoguider.agMirrorPos = update.agMirrorPos;
		status.autoguider.agMirrorStatus = update.agMirrorStatus;

		status.autoguider.agFocusDemand = update.agFocusDemand;
		status.autoguider.agFocusPos = update.agFocusPos;
		status.autoguider.agFocusStatus = update.agFocusStatus;

		status.autoguider.agFilterDemand = update.agFilterDemand;
		status.autoguider.agFilterPos = update.agFilterPos;
		status.autoguider.agFilterStatus = update.agFilterStatus;
	}

	private static void insertCalibrate(TCS_Status.Calibrate update) {
		// System.err.println("StatusPool::Inserting the CAL stuff");
		status.calibrate.timeStamp = update.timeStamp;

		status.calibrate.defAzError = update.defAzError;
		status.calibrate.defAltError = update.defAltError;
		status.calibrate.defCollError = update.defCollError;

		status.calibrate.currAzError = update.currAzError;
		status.calibrate.currAltError = update.currAltError;
		status.calibrate.currCollError = update.currCollError;

		status.calibrate.lastAzError = update.lastAzError;
		status.calibrate.lastAzRms = update.lastAzRms;
		status.calibrate.lastAltError = update.lastAltError;
		status.calibrate.lastAltRms = update.lastAltRms;
		status.calibrate.lastCollError = update.lastCollError;
		status.calibrate.lastCollRms = update.lastCollRms;

		status.calibrate.lastSkyRms = update.lastSkyRms;
	}

	private static void insertFocus(TCS_Status.FocalStation update) {
		// System.err.println("StatusPool::Inserting the FOCUS stuff");
		status.focalStation.timeStamp = update.timeStamp;

		status.focalStation.station = update.station;
		status.focalStation.instr = update.instr;
		status.focalStation.ag = update.ag;
	}

	private static void insertLimits(TCS_Status.Limits update) {

		status.limits.timeStamp = update.timeStamp;

		status.limits.azPosLimit = update.azPosLimit;
		status.limits.azNegLimit = update.azNegLimit;
		// System.err.println("StatusPool::Inserting the LIMIT data: "+
		// status.limits.azPosLimit+" - "+status.limits.azNegLimit);
		status.limits.altPosLimit = update.altPosLimit;
		status.limits.altNegLimit = update.altNegLimit;

		status.limits.rotPosLimit = update.rotPosLimit;
		status.limits.rotNegLimit = update.rotNegLimit;

		status.limits.timeToAzLimit = update.timeToAzLimit;
		status.limits.azLimitSense = update.azLimitSense;

		status.limits.timeToAltLimit = update.timeToAltLimit;
		status.limits.altLimitSense = update.altLimitSense;

		status.limits.timeToRotLimit = update.timeToRotLimit;
		status.limits.rotLimitSense = update.rotLimitSense;
	}

	private static void insertMechanisms(TCS_Status.Mechanisms update) {
		System.err.println("StatusPool::Inserting the MECH stuff");
		status.mechanisms.timeStamp = update.timeStamp;

		status.mechanisms.azName = update.azName;
		status.mechanisms.azDemand = update.azDemand;
		status.mechanisms.azPos = update.azPos;
		status.mechanisms.azStatus = update.azStatus;

		status.mechanisms.altName = update.altName;
		status.mechanisms.altDemand = update.altDemand;
		status.mechanisms.altPos = update.altPos;
		status.mechanisms.altStatus = update.altStatus;

		status.mechanisms.rotName = update.rotName;
		status.mechanisms.rotDemand = update.rotDemand;
		status.mechanisms.rotPos = update.rotPos;
		status.mechanisms.rotMode = update.rotMode;
		status.mechanisms.rotSkyAngle = update.rotSkyAngle;
		status.mechanisms.rotStatus = update.rotStatus;

		status.mechanisms.encShutter1Name = update.encShutter1Name;
		status.mechanisms.encShutter1Demand = update.encShutter1Demand;
		status.mechanisms.encShutter1Pos = update.encShutter1Pos;
		status.mechanisms.encShutter1Status = update.encShutter1Status;

		status.mechanisms.encShutter2Name = update.encShutter2Name;
		status.mechanisms.encShutter2Demand = update.encShutter2Demand;
		status.mechanisms.encShutter2Pos = update.encShutter2Pos;
		status.mechanisms.encShutter2Status = update.encShutter2Status;

		status.mechanisms.foldMirrorName = update.foldMirrorName;
		status.mechanisms.foldMirrorDemand = update.foldMirrorDemand;
		status.mechanisms.foldMirrorPos = update.foldMirrorPos;
		status.mechanisms.foldMirrorStatus = update.foldMirrorStatus;

		status.mechanisms.primMirrorName = update.primMirrorName;
		status.mechanisms.primMirrorCoverDemand = update.primMirrorCoverDemand;
		status.mechanisms.primMirrorCoverPos = update.primMirrorCoverPos;
		status.mechanisms.primMirrorCoverStatus = update.primMirrorCoverStatus;

		status.mechanisms.secMirrorName = update.secMirrorName;
		status.mechanisms.secMirrorDemand = update.secMirrorDemand;
		status.mechanisms.secMirrorPos = update.secMirrorPos;
		System.err.println("StatusPool:: Received mechanism sec mirror pos update: "+status.mechanisms.secMirrorPos);
		status.mechanisms.focusOffset = update.focusOffset;
		System.err.println("StatusPool:: Received mechanism focus offset update: "+status.mechanisms.focusOffset);
		status.mechanisms.secMirrorStatus = update.secMirrorStatus;

		status.mechanisms.primMirrorSysName = update.primMirrorSysName;
		status.mechanisms.primMirrorSysStatus = update.primMirrorSysStatus;
	}

	private static void insertMeteorology(TCS_Status.Meteorology update) {
		// System.err.println("StatusPool::Inserting the WEATHER stuff");

		status.meteorology.timeStamp = update.timeStamp;

		status.meteorology.wmsStatus = update.wmsStatus;
		status.meteorology.rainState = update.rainState;
		status.meteorology.moistureFraction = update.moistureFraction;
		status.meteorology.extTemperature = update.extTemperature;
		status.meteorology.serrurierTrussTemperature = update.serrurierTrussTemperature;
		status.meteorology.oilTemperature = update.oilTemperature;
		status.meteorology.primMirrorTemperature = update.primMirrorTemperature;
		status.meteorology.secMirrorTemperature = update.secMirrorTemperature;
		status.meteorology.dewPointTemperature = update.dewPointTemperature;

		status.meteorology.windSpeed = update.windSpeed;
		status.meteorology.windDirn = update.windDirn;
		status.meteorology.agBoxTemperature = update.agBoxTemperature;

		status.meteorology.pressure = update.pressure;
		status.meteorology.humidity = update.humidity;
		status.meteorology.lightLevel = update.lightLevel;

	}

	private static void insertSource(TCS_Status.SourceBlock update) {
		// System.err.println("StatusPool::Inserting a SOURCE update");
		status.source.timeStamp = update.timeStamp;

		status.source.srcName = update.srcName;
		status.source.srcRa = update.srcRa;
		status.source.srcDec = update.srcDec;
		// System.err.println("StatusPool:: Received source update: RA: "+
		// status.source.srcRa+" rads, Dec: "+
		// status.source.srcDec+" rads");

		status.source.srcEquinox = update.srcEquinox;
		status.source.srcEpoch = update.srcEpoch;
		status.source.srcNsTrackRA = update.srcNsTrackRA;
		status.source.srcNsTrackDec = update.srcNsTrackDec;
		status.source.srcPmRA = update.srcPmRA;
		status.source.srcPmDec = update.srcPmDec;
		status.source.srcParallax = update.srcParallax;
		status.source.srcRadialVelocity = update.srcRadialVelocity;
		status.source.srcActRa = update.srcActRa;
		status.source.srcActDec = update.srcActDec;

	}

	private static void insertServices(TCS_Status.Services update) {
		// System.err.println("StatusPool::Inserting the SERVICES stuff");
		status.services.timeStamp = update.timeStamp;

		status.services.powerState = update.powerState;
	}

	private static void insertState(TCS_Status.State update) {
		// System.err.println("StatusPool::Inserting the STATE stuff");
		status.state.timeStamp = update.timeStamp;

		status.state.networkControlState = update.networkControlState;
		status.state.engineeringOverrideState = update.engineeringOverrideState;
		status.state.telescopeState = update.telescopeState;
		status.state.tcsState = update.tcsState;
		status.state.systemRestartFlag = update.systemRestartFlag;
		status.state.systemShutdownFlag = update.systemShutdownFlag;
	}

	private static void insertTime(TCS_Status.Time update) {
		// System.err.println("StatusPool::Inserting the TIME stuff");
		status.time.timeStamp = update.timeStamp;

		status.time.mjd = update.mjd;
		status.time.ut1 = update.ut1;
		status.time.lst = update.lst;
	}

	private static void insertVersion(TCS_Status.Version update) {
		// System.err.println("StatusPool::Inserting the VERSION stuff");
		status.version.timeStamp = update.timeStamp;

		status.version.tcsVersion = update.tcsVersion;
	}

	private static void insertNetwork(TCS_Status.Network update) {
		// System.err.println("StatusPool::Inserting the NETWORK info:"+
		// "\n"+update.toString());
		status.network.timeStamp = update.timeStamp;

		status.network.networkState = update.networkState;
	}

	/** get an individual status segment. */
	private static TCS_Status.Segment getStatus(String cat) {
		if (cats.containsKey(cat))
			return (TCS_Status.Segment) cats.get(cat);
		return null;
	}

	/** get a status segment entry. */
	public static Object getStatusEntry(String cat, String id) {
		TCS_Status.Segment seg = getStatus(cat);
		if (seg == null)
			return null;
		return seg.getEntry(id);
	}

	private static void log(String text) {
		System.err.println("StatusPool::" + text);
	}

	
	
	
	public void telescopeStatusUpdate(TelescopeStatus telstatus) throws RemoteException {
		//System.err.println("StatusPool::telescopeStatusUpdate: " + telstatus);
		
		try {

		if (telstatus instanceof PrimaryAxisStatus) {
			PrimaryAxisStatus axis = (PrimaryAxisStatus) telstatus;
			String axisName = axis.getMechanismName();
			if (axisName.equals("AZM")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.azDemand = axis.getDemandPosition();
				StatusPool.latest().mechanisms.azPos = axis.getCurrentPosition();
				StatusPool.latest().mechanisms.azStatus = axis.getMechanismState();
			} else if (axisName.equals("ALT")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.altDemand = axis.getDemandPosition();
				StatusPool.latest().mechanisms.altPos = axis.getCurrentPosition();
				StatusPool.latest().mechanisms.altStatus = axis.getMechanismState();
			}
			instance.notifyObservers(MECHANISMS_UPDATE_EVENT, this.status.mechanisms);
		}

		if (telstatus instanceof RotatorAxisStatus) {
			RotatorAxisStatus rotator = (RotatorAxisStatus) telstatus;
			StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
			StatusPool.latest().mechanisms.rotDemand = rotator.getDemandPosition();
			StatusPool.latest().mechanisms.rotPos = rotator.getCurrentPosition();
			StatusPool.latest().mechanisms.rotStatus = rotator.getMechanismState();
			StatusPool.latest().mechanisms.rotSkyAngle = rotator.getSkyAngle();
			StatusPool.latest().mechanisms.rotMode = rotator.getRotatorMode();
			instance.notifyObservers(MECHANISMS_UPDATE_EVENT, this.status.mechanisms);
		}
		if (telstatus instanceof FocusStatus) {
			FocusStatus focusStatus = (FocusStatus) telstatus;
			String focusName = focusStatus.getMechanismName();
			
			if (focusName == null)
				return;
			
			if (focusName.equals("SMF")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.secMirrorDemand = focusStatus.getDemandPosition();
				StatusPool.latest().mechanisms.secMirrorPos = focusStatus.getCurrentPosition();
				System.err.println("StatusPool::telescopeStatusUpdate:SMF:sec mirror pos: "+focusStatus.getCurrentPosition());
				StatusPool.latest().mechanisms.secMirrorStatus = focusStatus.getMechanismState();
				StatusPool.latest().mechanisms.focusOffset = focusStatus.getFocusOffset();
				System.err.println("StatusPool::telescopeStatusUpdate:SMF:focus offset: "+focusStatus.getFocusOffset());
				instance.notifyObservers(MECHANISMS_UPDATE_EVENT, this.status.mechanisms);
			} else if
				(focusName.equals("AGF")) {
				StatusPool.latest().autoguider.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().autoguider.agFocusDemand = focusStatus.getDemandPosition();
				StatusPool.latest().autoguider.agFocusPos = focusStatus.getCurrentPosition();
				StatusPool.latest().autoguider.agFocusStatus = focusStatus.getMechanismState();
				instance.notifyObservers(FOCAL_STATION_UPDATE_EVENT, this.status.focalStation);
			}
			
		}
		
		
		if (telstatus instanceof AuxilliaryMechanismStatus) {
			
			AuxilliaryMechanismStatus aux = (AuxilliaryMechanismStatus)telstatus;
		
			String auxMechName = aux.getMechanismName();
			if (auxMechName == null) 
				return;
			if (auxMechName.equals("AGD")) {
				StatusPool.latest().autoguider.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().autoguider.agMirrorDemand = aux.getDemandPosition();
				StatusPool.latest().autoguider.agMirrorPos = aux.getCurrentPosition();
				StatusPool.latest().autoguider.agMirrorStatus = aux.getMechanismState();
			} else if
				(auxMechName.equals("AGI")) {
				StatusPool.latest().autoguider.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().autoguider.agFilterDemand = aux.getDemandPosition();
				StatusPool.latest().autoguider.agFilterPos = aux.getCurrentPosition();
				StatusPool.latest().autoguider.agFilterStatus = aux.getMechanismState();
			} else if
				(auxMechName.equals("PMC")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.primMirrorCoverDemand = aux.getDemandPosition();
				StatusPool.latest().mechanisms.primMirrorCoverPos = aux.getCurrentPosition();
				StatusPool.latest().mechanisms.primMirrorCoverStatus = aux.getMechanismState();
			} else if
				(auxMechName.equals("PMS")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.primMirrorSysStatus = aux.getMechanismState();
				
			} else if 
				(auxMechName.equals("EN1")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.encShutter1Demand = aux.getDemandPosition();
				StatusPool.latest().mechanisms.encShutter1Pos = aux.getCurrentPosition();
				StatusPool.latest().mechanisms.encShutter1Status = aux.getMechanismState();
			} else if 
				(auxMechName.equals("EN2")) {
				StatusPool.latest().mechanisms.timeStamp = telstatus.getStatusTimeStamp();
				StatusPool.latest().mechanisms.encShutter2Demand = aux.getDemandPosition();
				StatusPool.latest().mechanisms.encShutter2Pos = aux.getCurrentPosition();
				StatusPool.latest().mechanisms.encShutter2Status = aux.getMechanismState();
			}
			instance.notifyObservers(MECHANISMS_UPDATE_EVENT, this.status.mechanisms);
		}
		
		if (telstatus instanceof AutoguiderStatus) {
			
			AutoguiderStatus autoguider = (AutoguiderStatus)telstatus;
			StatusPool.latest().autoguider.timeStamp = telstatus.getStatusTimeStamp();
			StatusPool.latest().autoguider.agMode = autoguider.getGuideMode();
			StatusPool.latest().autoguider.agStatus = autoguider.getGuideState();
			StatusPool.latest().autoguider.agSwState = autoguider.getSoftwareState();
			StatusPool.latest().autoguider.fwhm = autoguider.getGuideFwhm();
			StatusPool.latest().autoguider.guideStarMagnitude = autoguider.getGuideStarMagnitude();
			instance.notifyObservers(AUTOGUIDER_UPDATE_EVENT, this.status.autoguider);
		}
		
		if (telstatus instanceof TelescopeControlSystemStatus) {
			TelescopeControlSystemStatus telstate = (TelescopeControlSystemStatus)telstatus;
			StatusPool.latest().state.timeStamp = telstatus.getStatusTimeStamp();
			StatusPool.latest().state.telescopeState = telstate.getTelescopeSystemState();
			StatusPool.latest().state.tcsState = telstate.getTelescopeControlSystemState();
			StatusPool.latest().state.engineeringOverrideState = telstate.getTelescopeEngineeringControlState();
			StatusPool.latest().state.networkControlState = telstate.getTelescopeNetworkControlState();
			instance.notifyObservers(STATE_UPDATE_EVENT, this.status.state);		
		}
		
		if (telstatus instanceof TelescopeEnvironmentStatus) {
			TelescopeEnvironmentStatus env = (TelescopeEnvironmentStatus)telstatus;
			StatusPool.latest().meteorology.timeStamp = telstatus.getStatusTimeStamp();
			StatusPool.latest().meteorology.agBoxTemperature = env.getAgBoxTemperature();
			StatusPool.latest().meteorology.primMirrorTemperature = env.getPrimaryMirrorTemperature();
			StatusPool.latest().meteorology.secMirrorTemperature = env.getSecondaryMirrorTemperature();
			StatusPool.latest().meteorology.serrurierTrussTemperature = env.getTrussTemperature();
			StatusPool.latest().meteorology.oilTemperature = env.getOilTemperature();
			instance.notifyObservers(MECHANISMS_UPDATE_EVENT, this.status.meteorology);
		}

		if (telstatus instanceof SourceData) {
		    SourceData srcData = (SourceData)telstatus;
		    StatusPool.latest().source.timeStamp = telstatus.getStatusTimeStamp();
		    //System.err.println("StatusPool: telstatus:SourceData: "+srcData);
		    StatusPool.latest().source.srcName = srcData.srcName;
		    StatusPool.latest().source.srcRa = srcData.srcRa;
		    StatusPool.latest().source.srcDec = srcData.srcDec;
		    StatusPool.latest().source.srcEquinox = srcData.srcEquinox;
		    StatusPool.latest().source.srcEpoch = srcData.srcEpoch;
		    StatusPool.latest().source.srcPmRA = srcData.srcPmRA;
		    StatusPool.latest().source.srcPmDec = srcData.srcPmDec;
		    StatusPool.latest().source.srcNsTrackRA = srcData.srcNsTrackRA;
		    StatusPool.latest().source.srcNsTrackDec = srcData.srcNsTrackDec;
		    StatusPool.latest().source.srcParallax = srcData.srcParallax;
		    StatusPool.latest().source.srcRadialVelocity = srcData.srcRadialVelocity;
		    StatusPool.latest().source.srcActRa = srcData.srcActRa;
		    StatusPool.latest().source.srcActDec = srcData.srcActDec;
		    instance.notifyObservers(SOURCE_UPDATE_EVENT, this.status.source);

		}

		if (telstatus instanceof TimeData) {
                    TimeData timeData = (TimeData)telstatus;
                    StatusPool.latest().time.timeStamp = telstatus.getStatusTimeStamp();
                    //System.err.println("StatusPool: telstatus:TimeData: "+timeData);
                    StatusPool.latest().time.mjd = timeData.mjd;
		    StatusPool.latest().time.lst = timeData.lst;
		    StatusPool.latest().time.ut1 = timeData.ut1;
		    instance.notifyObservers(TIME_UPDATE_EVENT, this.status.time);

		}
		
		if (telstatus instanceof AstrometryData) {
			
			AstrometryData ad = (AstrometryData) telstatus;
			//System.err.println("StatusPool: telstatus:Astro: "+ad.airmass);
			StatusPool.latest().astrometry.timeStamp = telstatus.getStatusTimeStamp();
			StatusPool.latest().astrometry.airmass = ad.airmass;
			StatusPool.latest().astrometry.refractionHumidity = ad.refractionHumidity;
			StatusPool.latest().astrometry.refractionPressure = ad.refractionPressure;
			StatusPool.latest().astrometry.refractionTemperature = ad.refractionTemperature;
			StatusPool.latest().astrometry.refractionWavelength = ad.refractionWavelength;
			StatusPool.latest().astrometry.agwavelength = ad.agwavelength;
			
			instance.notifyObservers(ASTROMETRY_UPDATE_EVENT, this.status.astrometry);
			
		}
		
		} catch (Exception e) {
		    e.printStackTrace();
		    // lets not allow this to fail and get knoecked off listener list..
		}
	}

	public void meteorologyStatusUpdate(MeteorologyStatus mstatus) throws RemoteException {
		// TODO Auto-generated method stub
	    try {
		//System.err.println("StatusPool::meteoStatusUpdate: " + mstatus.getClass().getName());
		if (mstatus instanceof WmsStatus) {
			WmsStatus wms = (WmsStatus) mstatus;
			StatusPool.latest().meteorology.timeStamp = mstatus.getStatusTimeStamp();
			StatusPool.latest().meteorology.dewPointTemperature = wms.getDewPointTemperature();
			StatusPool.latest().meteorology.extTemperature = wms.getExtTemperature();
			StatusPool.latest().meteorology.humidity = wms.getHumidity();
			StatusPool.latest().meteorology.lightLevel = wms.getLightLevel();
			StatusPool.latest().meteorology.moistureFraction = wms.getMoistureFraction();
			StatusPool.latest().meteorology.pressure = wms.getPressure();
			StatusPool.latest().meteorology.rainState = wms.getRainState();
			StatusPool.latest().meteorology.windDirn = wms.getWindDirn();
			StatusPool.latest().meteorology.windSpeed = wms.getWindSpeed();
			StatusPool.latest().meteorology.wmsStatus = wms.getWmsStatus();
			System.err.println("StatusPool::Updated all meteo parameters, forwarding to observers...");
			instance.notifyObservers(METEOROLOGY_UPDATE_EVENT, this.status.meteorology);		
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	public void telescopeNetworkFailure(long time, String message) throws RemoteException {	
		System.err.println("StatusPool::telescopeNetworkFailure: " + message);
	}
}

/**
 * $Log: StatusPool.java,v $ /** Revision 1.3 2007/10/18 07:40:48 snf /** added
 * actra and actdec to source update /** /** Revision 1.2 2007/07/02 10:07:47
 * snf /** added agBoxTemp to status for meteo /** /** Revision 1.1 2006/12/12
 * 08:30:52 snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:34:57
 * snf /** Initial revision /** /** Revision 1.7 2002/09/16 09:38:28 snf /** ***
 * empty log message *** /** /** Revision 1.6 2001/06/08 16:27:27 snf /** Added
 * telfocus trapping info. /** /** Revision 1.5 2001/04/27 17:14:32 snf /**
 * backup /** /** Revision 1.4 2001/02/16 17:44:27 snf /** *** empty log message
 * *** /** /** Revision 1.3 2000/12/22 14:40:37 snf /** Backup. /** /** Revision
 * 1.2 2000/12/20 10:38:53 snf /** Changed TCS_Status reference. /** /**
 * Revision 1.1 2000/12/14 11:53:56 snf /** Initial revision /**
 */

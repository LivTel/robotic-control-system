package ngat.rcs.calib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ngat.astrometry.Astrometry;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.Catalog;
import ngat.astrometry.Coordinates;
import ngat.astrometry.Position;
import ngat.astrometry.Site;
import ngat.astrometry.SolarCalculator;
import ngat.icm.InstrumentCalibration;
import ngat.icm.InstrumentCalibrationHistory;
import ngat.icm.InstrumentCalibrationProvider;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.XBeamSteeringConfig;
import ngat.phase2.XDetectorConfig;
import ngat.phase2.XFilterDef;
import ngat.phase2.XFilterSpec;
import ngat.phase2.XImagerInstrumentConfig;
import ngat.rcs.RCS_Controller;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.manager.DefaultModalTask;
import ngat.rcs.tms.manager.ModalTask;
import ngat.tcm.TelescopeCalibration;
import ngat.tcm.TelescopeCalibrationHistory;
import ngat.util.ConfigurationProperties;

// use findAvailJobs to implement nextJob(), nextWantCtrl(), wantCtrl()
//
// wantsCtrl = findJobs() != null
//
// getNextJob() = findJobs() != null && rankJobs() and selectBestJob()
//
// nextWantCtrl() = findJobs() != null at some t in future.
//
//
//
//
//

/** Manages calibration operating mode. */
public class CalibrationControlAgent extends DefaultModalTask {

	/** No TELFOCUS until 1H after sunset. */
	protected static final long TELFOCUS_SUNSET_OFFSET = 3600 * 1000L;

	/** No TELFOCUS after 1H before sunrise. */
	protected static final long TELFOCUS_SUNRISE_OFFSET = 3600 * 1000L;

	protected static final long EVENING_SKYFLAT_MIN_DURATION = 8 * 60 * 1000L;

	/** The sun level at which we stop evening flats. */
	protected static final double EVENING_SKYFLAT_MIN_SOLAR_ELEV = -10.0;

	/** The sun level at which we start evening flats. */
	protected static final double EVENING_SKYFLAT_MAX_SOLAR_ELEV = -2.5;

	protected static final long MORNING_SKYFLAT_MIN_DURATION = 8 * 60 * 1000L;

	/** The sun level at which we start morning flats. */
	protected static final double MORNING_SKYFLAT_MIN_SOLAR_ELEV = -10.0;

	/** The sun level at which we stop morning flats. */
	protected static final double MORNING_SKYFLAT_MAX_SOLAR_ELEV = -2.5;

	/** Agent ID. */
	protected static final String CLASS = "CCA";

	private static final double ASTRO_TWILIGHT_LIMIT = Math.toRadians(-18.0);
	private static final double HORIZON_LIMIT = Math.toRadians(-0.5); // sun
	// dipped

	/** The single instance. */
	protected static CalibrationControlAgent instance;

	/** The CA. */
	//RCS_ControlTask controlAgent;

	// SkyFlatsCalibrationTask skyflatsTask;

	// TelFocusCalibrationTask telfocusTask;

	TelescopeCalibration telCalib;

	TelescopeCalibrationHistory telCalibHist;

	protected Site obsSite;

	protected AstrometrySiteCalculator astro;

	protected InstrumentRegistry ireg;

	/** Focus star catalog. */
	protected Catalog focusStarCatalog;

	/** Focus instrument. */
	private String focusInstrumentName;

	private InstrumentDescriptor fid;

	/** Config used for telfocus. */
	protected IInstrumentConfig focusInstrumentConfig;

	/** Latest time after sunset to start evening flats. */
	// protected long eveningSkyflatsLatestTimeAfterSunsetLimit;

	// protected long morningSkyflatsEarliestTimeBeforeSunriseLimit;
	// protected long morningSkyflatsLatestTimeBeforeSunriseLimit;
	protected long morningSkyFlatsMinimumDuration;
	protected double morningSkyflatsMinimumSolarElevation;
	protected double morningSkyflatsMaximumSolarElevation;

	protected long eveningSkyFlatsMinimumDuration;
	protected double eveningSkyflatsMinimumSolarElevation;
	protected double eveningSkyflatsMaximumSolarElevation;

	boolean morning = false;
	boolean evening = false;

	private boolean logsearch = false;

	private double calibWeightSameFlats;
	private double calibWeightOtherFlats;
	private double calibWeightRandom;
	private double calibWeightPriority;

	/** Create an instance. */
	public CalibrationControlAgent(String name, TaskManager manager) {
		super(name, manager);
	}

	/**
	 * Creates the initial instance of the ModalTask.
	 */
	@Override
	public void initialize(ModalTask mt) {
		instance = (CalibrationControlAgent) mt;
		//controlAgent = RCS_ControlTask.getInstance();

		telCalib = RCS_Controller.controller.getTelescopeCalibration();
		telCalibHist = RCS_Controller.controller.getTelescopeCalibrationHistory();

		ireg = RCS_Controller.controller.getInstrumentRegistry();

		obsSite = RCS_Controller.controller.getObservatorySite();
		astro = RCS_Controller.controller.getSiteCalculator();
	}

	/** Returns a reference to the singleton instance. */
	public static ModalTask getInstance() {
		return instance;
	}

	/**
	 * Configure from File.
	 * 
	 * @param file
	 *            Configuration file.
	 * @exception IOException
	 *                If any problem occurs reading the file or does not exist.
	 * @exception IllegalArgumentException
	 *                If any config information is dodgy.
	 */
	@Override
	public void configure(File file) throws IOException, IllegalArgumentException {
		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));

		try {

			// we need to access the Focus star catalog
			if (config.getProperty("focus.star.catalog") != null) {

				File catfile = new File(config.getProperty("focus.star.catalog", "config/focus_std.cat"));

				focusStarCatalog = Astrometry.loadCatalog("FOCUS_STD", catfile);
				taskLog.log(1, CLASS, name, "Config", "CCA loaded " + focusStarCatalog.size() + " targets from "
						+ focusStarCatalog.getCatalogName());

			}

			// work out a config for the focus instrument

			focusInstrumentName = telCalib.getTelfocusInstrument();
			if (focusInstrumentName != null) {

				fid = new InstrumentDescriptor(focusInstrumentName);

				File focusInstrumentConfigFile = new File(config.getProperty("focus.instrument.config.file",
						"config/telfocus_inst.properties"));

				ConfigurationProperties telcfg = new ConfigurationProperties();
				telcfg.load(new FileInputStream(focusInstrumentConfigFile));

				if (focusInstrumentName.equalsIgnoreCase("RATCAM")) {
					String lf = telcfg.getProperty("lower.filter", "clear");
					String uf = telcfg.getProperty("upper.filter", "SDSS-R");

					XImagerInstrumentConfig xim = new XImagerInstrumentConfig("TelFocusCfg");
					xim.setInstrumentName(focusInstrumentName);
					XFilterSpec filters = new XFilterSpec();

					filters.addFilter(new XFilterDef(lf));
					filters.addFilter(new XFilterDef(uf));
					xim.setFilterSpec(filters);

					int xbin = telcfg.getIntValue("x.bin", 2);
					int ybin = telcfg.getIntValue("y.bin", 2);
					XDetectorConfig detector = new XDetectorConfig();
					detector.setXBin(xbin);
					detector.setYBin(ybin);
					xim.setDetectorConfig(detector);
					focusInstrumentConfig = xim;
				} else if (focusInstrumentName.equalsIgnoreCase("IO:O")) {
					String mf = telcfg.getProperty("primary.filter", "clear");
					String fs1 = telcfg.getProperty("slide.filter.1", "clear");
					String fs2 = telcfg.getProperty("slide.filter.2", "clear");

					XImagerInstrumentConfig xim = new XImagerInstrumentConfig("TelFocusCfg");
					xim.setInstrumentName(focusInstrumentName);
					XFilterSpec filters = new XFilterSpec();

					filters.addFilter(new XFilterDef(mf));
					// TODO enable these 2 fields
					filters.addFilter(new XFilterDef(fs1));
					filters.addFilter(new XFilterDef(fs2));
					xim.setFilterSpec(filters);

					int xbin = telcfg.getIntValue("x.bin", 2);
					int ybin = telcfg.getIntValue("y.bin", 2);
					XDetectorConfig detector = new XDetectorConfig();
					detector.setXBin(xbin);
					detector.setYBin(ybin);
					xim.setDetectorConfig(detector);
					focusInstrumentConfig = xim;

				}
				// null instrument
			}
			/** The sun level at which we stop morning flats. */
			// TODO Check instrument blank catalogs here
			// Iterator it = Instruments.findInstrumentSet();
			List ilist = ireg.listInstruments();
			Iterator it = ilist.iterator();
			while (it.hasNext()) {
				InstrumentDescriptor instId = (InstrumentDescriptor) it.next();

				String ilcname = instId.getInstrumentName().toLowerCase();
				if (config.getProperty(ilcname + ".blank.catalog") != null) {

					File catfile = new File(config.getProperty(ilcname + ".blank.catalog", "config/" + ilcname
							+ "_blank.cat"));

					// will throw a wobbly if this fails to load...
					Catalog instBlankCatalog = Astrometry.loadCatalog(instId.getInstrumentName() + "_BLANK", catfile);
					taskLog.log(1, CLASS, name, "Config", "CCA loaded " + instBlankCatalog.size() + " targets from "
							+ instBlankCatalog.getCatalogName());

				}

			}

			morningSkyFlatsMinimumDuration = config.getLongValue("morning.skyflats.min.duration",
					MORNING_SKYFLAT_MIN_DURATION);

			morningSkyflatsMinimumSolarElevation = Math.toRadians(config.getDoubleValue(
					"morning.skyflats.min.solar.elevation", MORNING_SKYFLAT_MIN_SOLAR_ELEV));
			morningSkyflatsMaximumSolarElevation = Math.toRadians(config.getDoubleValue(
					"morning.skyflats.max.solar.elevation", MORNING_SKYFLAT_MAX_SOLAR_ELEV));

			eveningSkyFlatsMinimumDuration = config.getLongValue("evening.skyflats.min.duration",
					MORNING_SKYFLAT_MIN_DURATION);

			eveningSkyflatsMinimumSolarElevation = Math.toRadians(config.getDoubleValue(
					"evening.skyflats.min.solar.elevation", EVENING_SKYFLAT_MIN_SOLAR_ELEV));
			eveningSkyflatsMaximumSolarElevation = Math.toRadians(config.getDoubleValue(
					"evening.skyflats.max.solar.elevation", EVENING_SKYFLAT_MAX_SOLAR_ELEV));

			taskLog.log(1, CLASS, name, "Config", "ESF Min duration: " + (eveningSkyFlatsMinimumDuration / 1000L) + "s");
			taskLog.log(1, CLASS, name, "Config",
					"ESF Max Solar: Start at: " + Math.toDegrees(eveningSkyflatsMaximumSolarElevation) + "deg");
			taskLog.log(1, CLASS, name, "Config",
					"ESF Min Solar: Stops at: " + Math.toDegrees(eveningSkyflatsMinimumSolarElevation) + "deg");

			taskLog.log(1, CLASS, name, "Config", "MSF Min duration: " + (morningSkyFlatsMinimumDuration / 1000L) + "s");
			taskLog.log(1, CLASS, name, "Config",
					"MSF Min Solar: Start at: " + Math.toDegrees(morningSkyflatsMinimumSolarElevation) + "deg");
			taskLog.log(1, CLASS, name, "Config",
					"MSF Max Solar: Stops at: " + Math.toDegrees(morningSkyflatsMaximumSolarElevation) + "deg");

			long now = System.currentTimeMillis();

			calibWeightSameFlats = config.getDoubleValue("calib.weight.same", 0.5);
			calibWeightOtherFlats = config.getDoubleValue("calib.weight.other", 0.5);
			calibWeightRandom = config.getDoubleValue("calib.weight.random", 0.5);
			calibWeightPriority = config.getDoubleValue("calib.weight.priority", 0.0);

			taskLog.log(1, CLASS, name, "Config", "Calib weights: Cs=" + calibWeightSameFlats + ", Co="
					+ calibWeightOtherFlats + ", Cr=" + calibWeightRandom + ", Cp=" + calibWeightPriority);

		} catch (Exception e) {
			throw new IllegalArgumentException("Error parsing calibration-config: " + e);
		}
		taskLog.log(1, CLASS, name, "Config", "CCA was configured ok");

	}

	/** How long till this controller will definitely want control from time. */
	@Override
	public long nextWantsControl(long time) throws RemoteException {

		// step forward for rest of night till sunrise and check ...

		taskLog.log(1, "CAL::Checking ahead to see if wanting control...");
		long t = time;
		Position sun = Astrometry.getSolarPosition(time);
		double sunElev = sun.getAltitude(time, obsSite);
		while (t < time + 24 * 3600 * 1000L && sunElev < 0.0) {
			logsearch = false;
			if (wantsControl(t))
				return t;

			t += 2 * 60 * 1000L; // 2 minute steps
			sun = Astrometry.getSolarPosition(t);
			sunElev = sun.getAltitude(t, obsSite);
		}
		logsearch = true;
		return time + 24 * 3600 * 1000L;

	}

	@Override
	public boolean wantsControl(long time) throws RemoteException {
		List<CalibrationOperation> list = null;
		try {
			list = findAvailableJobs(time);
			if (logsearch)
				System.err.printf("At %tF %tT Found these jobs: %s", time, time, list.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (list == null || list.size() == 0)
			return false;
		return true;
	}

	/** Return true if wants control at time. */
	/*
	 * public boolean wantsControl2(long time) throws RemoteException {
	 * 
	 * // START NEWCODE // List jlist = findAvailableJobs(time); // return (!
	 * jlist.isEmpty()); // END NEWCODE
	 * 
	 * long sunset = RCS_Controller.controller.getObsDate().getSunset(); //
	 * check // this // is // valid // ? long sunrise =
	 * RCS_Controller.controller.getObsDate().getSunrise(); // check // this //
	 * is // valid // ? if (sunrise < time) sunrise += 24 * 3600 * 1000L;
	 * 
	 * // morning or evening ? long later = time + 30 * 60 * 1000L; Position
	 * sunnow = Astrometry.getSolarPosition(time); double selnow =
	 * sunnow.getAltitude(time, obsSite); Position sun30 =
	 * Astrometry.getSolarPosition(later); double sel30 =
	 * sun30.getAltitude(later, obsSite); // taskLog.log(1,
	 * "CAL:: SunElev now: " + Position.toDegrees(selnow, 2) // + " after 30M: "
	 * // + Position.toDegrees(sel30, 2));
	 * 
	 * morning = false; evening = false;
	 * 
	 * long timeSinceStartAstroTwilight = 0L; long timeTillSunrise = 0L; long
	 * timeSinceSunset = 0L; long timeTillEndAstroTwilight = 0L;
	 * 
	 * if (selnow > Math.toRadians(-18.0)) { // it is twilight of some sort if
	 * (sel30 > selnow) { // morning morning = true; evening = false;
	 * 
	 * // compute BACK startMorningTwilight timeSinceStartAstroTwilight =
	 * computeStartAstroTwilight(time); // compute FWD sunrise timeTillSunrise =
	 * computeNextSunrise(time); // taskLog.log(1,
	 * "CAL:: PERIOD:Morning: Time Since SAT =" + //
	 * (timeSinceStartAstroTwilight / 60000) + "m"); // taskLog.log(1,
	 * "CAL:: PERIOD:Morning: Time Till   SR =" + // (timeTillSunrise / 60000) +
	 * "m");
	 * 
	 * // Check all known instruments (flats and standards etc, etc)
	 * 
	 * // Iterator it = Instruments.findInstrumentSet(); List ilist =
	 * ireg.listInstruments(); Iterator it = ilist.iterator(); while
	 * (it.hasNext()) { InstrumentDescriptor instId = (InstrumentDescriptor)
	 * it.next();
	 * 
	 * try {
	 * 
	 * InstrumentCalibrationProvider icalp =
	 * ireg.getCalibrationProvider(instId); InstrumentCalibration ical =
	 * icalp.getCalibrationRequirements();
	 * 
	 * InstrumentCalibrationHistory ich = icalp.getCalibrationHistory(); //
	 * taskLog.log(1, // "CAL:: Check calibration history for: " + instId + //
	 * " " + ich);
	 * 
	 * // TODO new icm system after Instruments is trashed... //
	 * RCS_Controller.controller.getInstrumentRegistry() //
	 * .getStatusProvider(new InstrumentDescriptor("a")) // .getStatus() //
	 * .isOnline();
	 * 
	 * InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
	 * InstrumentStatus istat = isp.getStatus();
	 * 
	 * if ((!istat.isOnline()) && (!istat.isFunctional())) continue;
	 * 
	 * // MSF if (morning && ical.doMorningSkyflatCalibration() && (time -
	 * ich.getLastMorningSkyflatCalibration() > ical
	 * .getMorningSkyflatCalibrationInterval()) && timeTillSunrise <
	 * morningSkyflatsEarliestTimeBeforeSunriseLimit && timeTillSunrise >
	 * morningSkyFlatsMinimumDuration) {
	 * 
	 * return true;
	 * 
	 * } else {
	 * 
	 * }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); taskLog.log(1,
	 * "CAL:: Error locating calibration/history for " + instId); }
	 * 
	 * } // next Instrument
	 * 
	 * } else { morning = false; evening = true;
	 * 
	 * // compute BACK sunset timeSinceSunset = computeTimeSinceSunset(time); //
	 * compute FWD endEveningTwilight timeTillEndAstroTwilight =
	 * computeEndAstroTwilight(time);
	 * 
	 * // Check all known instruments (flats and standards etc, etc)
	 * 
	 * List ilist = ireg.listInstruments(); Iterator it = ilist.iterator();
	 * while (it.hasNext()) { InstrumentDescriptor instId =
	 * (InstrumentDescriptor) it.next(); try {
	 * 
	 * InstrumentCalibrationProvider icalp =
	 * ireg.getCalibrationProvider(instId); InstrumentCalibration ical =
	 * icalp.getCalibrationRequirements(); InstrumentCalibrationHistory ich =
	 * icalp.getCalibrationHistory();
	 * 
	 * // see if this instrument is either offline or // impaired. int netStat =
	 * inst.getStatus(); int opStat // = inst.getOperationalStatus(); if
	 * (netStat ==
	 * 
	 * InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
	 * InstrumentStatus istat = isp.getStatus();
	 * 
	 * if ((!istat.isOnline()) && (!istat.isFunctional())) continue;
	 * 
	 * // ESF if (evening && ical.doEveningSkyflatCalibration() && (time -
	 * ich.getLastEveningSkyflatCalibration() > ical
	 * .getEveningSkyflatCalibrationInterval()) && (time - sunset <
	 * eveningSkyflatsLatestTimeAfterSunsetLimit)) {
	 * 
	 * return true; // this one will go now so do it... } else {
	 * 
	 * }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); taskLog.log(1,
	 * "CAL:: Error locating calibration/history for " + instId); }
	 * 
	 * } // next Instrument
	 * 
	 * }
	 * 
	 * } else {
	 * 
	 * // taskLog.log(1, "CAL:: PERIOD:Nighttime");
	 * 
	 * Calendar cal = Calendar.getInstance(); cal.setTime(new Date(time)); int h
	 * = cal.get(Calendar.HOUR_OF_DAY); int m = cal.get(Calendar.MINUTE);
	 * boolean hour_div_3_min_le_10 = ((h % 3 == 0) && (m < 10 || m > 50)); //
	 * within
	 * 
	 * boolean round_midnight_and_m_le_10 = ((h >= 23 || h <= 1) && (m < 10 || m
	 * > 50)); // within
	 * 
	 * // Check TELFOCUS NOTE SUNSET_OFFSET etc should be part of TalCalib //
	 * parameters. if (telCalib.doTelfocusCalibration() && (time -
	 * telCalibHist.getLastTelfocusCalibration() >
	 * telCalib.getTelfocusCalibrationInterval()) && (time - sunset >
	 * TELFOCUS_SUNSET_OFFSET) && (sunrise - time > TELFOCUS_SUNRISE_OFFSET) &&
	 * round_midnight_and_m_le_10) {
	 * 
	 * // check we have a focus instrument if (fid == null) return false;
	 * 
	 * try { InstrumentStatusProvider fsp = ireg.getStatusProvider(fid);
	 * InstrumentStatus fstat = fsp.getStatus(); if ((!fstat.isOnline()) &&
	 * (!fstat.isFunctional())) return false; } catch (Exception e) {
	 * e.printStackTrace(); return false; }
	 * 
	 * return true; }
	 * 
	 * }
	 * 
	 * // use TCAL and TCH from RCS Controller return false;
	 * 
	 * }
	 */

	/** @return A list of available tasks which can run at the specified time. */
	private List<CalibrationOperation> findAvailableJobs(long time) throws Exception {

		// store the list of jobs
		List<CalibrationOperation> list = new Vector<CalibrationOperation>();

		SolarCalculator sunTrack = new SolarCalculator();
		Coordinates sun = sunTrack.getCoordinates(time);

		double sunlev = astro.getAltitude(sun, time);
		double sunlevplus = astro.getAltitude(sun, time + 30 * 60 * 1000L);
		if (logsearch)
			System.err.printf("faj::Sunlev is now: %4.2f ->  %4.2f \n", Math.toDegrees(sunlev),
					Math.toDegrees(sunlevplus));

		long timeSinceStartAstroTwilight = 0L;
		long timeTillSunrise = 0L;
		long timeSinceSunset = 0L;
		long timeTillEndAstroTwilight = 0L;
		long timeTillSunMax = 0L;
		long timeTillSunMin = 0L;

		// twilight and rising
		boolean morningTwilight = ((sunlev > ASTRO_TWILIGHT_LIMIT) && (sunlev < HORIZON_LIMIT) && (sunlevplus > sunlev));

		boolean eveningTwilight = ((sunlev > ASTRO_TWILIGHT_LIMIT) && (sunlev < HORIZON_LIMIT) && (sunlevplus < sunlev));

		if (morningTwilight) {

			// check if sun is in right range, this will stop any nasties with
			// astro lib calls...
			if (sunlev > morningSkyflatsMinimumSolarElevation && sunlev < morningSkyflatsMaximumSolarElevation) {
				if (logsearch)
					System.err.println("faj::Test for MSF...");
				// how long since SAT and until TSR
				timeSinceStartAstroTwilight = astro.getTimeSinceLastRise(sun, ASTRO_TWILIGHT_LIMIT, time);
				timeTillSunrise = astro.getTimeUntilNextRise(sun, 0.0, time);
				timeTillSunMax = astro.getTimeUntilNextRise(sun, morningSkyflatsMaximumSolarElevation, time);

				List<InstrumentDescriptor> ilist = ireg.listInstruments();
				Iterator<InstrumentDescriptor> ii = ilist.iterator();
				while (ii.hasNext()) {

					InstrumentDescriptor id = ii.next();

					InstrumentStatusProvider isp = ireg.getStatusProvider(id);
					InstrumentStatus inst = isp.getStatus();
					if (logsearch)
						System.err.println("faj::check instrument: " + isp);

					// skip if the instrument is shagged !
					if (!inst.isFunctional() || !inst.isOnline() || !inst.isEnabled())
						continue;

					InstrumentCalibrationProvider cp = ireg.getCalibrationProvider(id);
					InstrumentCalibration ical = cp.getCalibrationRequirements();
					InstrumentCalibrationHistory ich = cp.getCalibrationHistory();
					// if (logsearch)
					System.err.println("faj::check calib: " + id.getInstrumentName() + " " + ical + " history: " + ich);

					if (ical == null)
						continue;
					boolean doMsf = ical.doMorningSkyflatCalibration();
					long msfInt = ical.getMorningSkyflatCalibrationInterval();
					// long msfSt = ical.getMorningSkyflatStartTimeOffset();
					// long msfEt = ical.getMorningSkyflatEndTimeOffset();
					// long msfWs = ical.getMorningSkyflatPreferredWindowSize();
					long msfLast = time - ich.getLastMorningSkyflatCalibration();

					// TODO: isREQUIRED & (ttmax > minduration) & (SUNLEV > min)
					// & (tslast > minint)

					// check if MSF is feasible...
					boolean mfeas = (doMsf) && (timeTillSunMax > morningSkyFlatsMinimumDuration)
							&& sunlev > morningSkyflatsMinimumSolarElevation && (msfLast > msfInt);

					// if (logsearch)
					// System.err.println("faj::tssat=" +
					// timeSinceStartAstroTwilight + ", msf-ST-Off=" + msfSt
					// + ", msf-ET-Off=" + msfEt + ", feasible: " + mfeas);

					// System.err.printf("Find available: (MSF): Sunlev: %4.2f,
					// Min: %4.2f, Max: %4.2f

					double score = calibWeightSameFlats
							* Math.floor((double) (time - ich.getLastMorningSkyflatCalibration())
									/ (double) (ical.getMorningSkyflatCalibrationInterval()))
							+ calibWeightOtherFlats
							* Math.floor((double) (time - ich.getLastEveningSkyflatCalibration())
									/ (double) (ical.getEveningSkyflatCalibrationInterval())) + calibWeightRandom
							* Math.random() + calibWeightPriority * (ical.getCalibrationPriority());

					// save this as a valid operation - not a TASK !
					if (mfeas)
						list.add(new MorningSkyFlatCalibration(id, score));

				}
			}
		} else if (eveningTwilight) {

			if (sunlev > eveningSkyflatsMinimumSolarElevation && sunlev < eveningSkyflatsMaximumSolarElevation) {
				if (logsearch)
					System.err.println("faj::Test for ESF...");

				// how long since SAT and until TSR
				timeSinceSunset = astro.getTimeSinceLastSet(sun, 0.0, time);
				// timeTillEndAstroTwilight = astro.getTimeUntilNextSet(sun,
				// ASTRO_TWILIGHT_LIMIT, time);
				// note we have a lower set limit
				timeTillEndAstroTwilight = astro.getTimeUntilNextSet(sun, eveningSkyflatsMinimumSolarElevation, time);
				timeTillSunMin = astro.getTimeUntilNextSet(sun, eveningSkyflatsMinimumSolarElevation, time);

				List<InstrumentDescriptor> ilist = ireg.listInstruments();
				Iterator<InstrumentDescriptor> ii = ilist.iterator();
				while (ii.hasNext()) {

					InstrumentDescriptor id = ii.next();

					InstrumentStatusProvider isp = ireg.getStatusProvider(id);
					InstrumentStatus inst = isp.getStatus();
					// skip if the instrument is shagged !
					if (!inst.isFunctional() || !inst.isOnline() || !inst.isEnabled())
						continue;

					InstrumentCalibrationProvider cp = ireg.getCalibrationProvider(id);
					InstrumentCalibration ical = cp.getCalibrationRequirements();
					InstrumentCalibrationHistory ich = cp.getCalibrationHistory();
					if (logsearch)
						System.err.println("faj::check calib: " + id.getInstrumentName() + " " + ical + " history: "
								+ ich);

					if (ical == null)
						continue;
					boolean doEsf = ical.doEveningSkyflatCalibration();
					long esfInt = ical.getEveningSkyflatCalibrationInterval();
					// long esfSt = ical.getEveningSkyflatStartTimeOffset();
					// long esfEt = ical.getEveningSkyflatEndTimeOffset();
					// long esfWs = ical.getEveningSkyflatPreferredWindowSize();
					long esfLast = time - ich.getLastEveningSkyflatCalibration();

					// check if ESF is feasible...

					boolean efeas = (doEsf) && (timeTillSunMin > eveningSkyFlatsMinimumDuration)
							&& sunlev < eveningSkyflatsMaximumSolarElevation && (esfLast > esfInt);

					// if (logsearch)
					// System.err.println("faj::tssset=" + timeSinceSunset +
					// ", esf-ST-Off=" + esfSt + ", esf-ET-Off="
					// + esfEt + ", feasible: " + efeas);

					double score = calibWeightSameFlats
							* Math.floor((double) (time - ich.getLastEveningSkyflatCalibration())
									/ (double) (ical.getEveningSkyflatCalibrationInterval()))
							+ calibWeightOtherFlats
							* Math.floor((double) (time - ich.getLastMorningSkyflatCalibration())
									/ (double) (ical.getMorningSkyflatCalibrationInterval())) + calibWeightRandom
							* Math.random() + calibWeightPriority * (ical.getCalibrationPriority());

					if (efeas)
						list.add(new EveningSkyFlatCalibration(id, score));

				}
			}
		} else {
			// proper night

			// work out telescope calibs here
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(time));
			int h = cal.get(Calendar.HOUR_OF_DAY);
			int m = cal.get(Calendar.MINUTE);

			// TEMP change to 5 am
			boolean round_midnight_and_m_le_10 = (   ((h == 23) && (m >= 50)) || 
													 ((h == 0) && ((m <= 10) || (m >= 50))) ||
						                             ((h == 1) && (m < 10)));
			
			
			// 0450-0510 and 0550-0610
			//boolean round_midnight_and_m_le_10 = ((h = 4 && m >= 50) || ((h >= 5 || h <= 6) && (m < 10 || m > 50)));

			timeTillSunrise = astro.getTimeUntilNextRise(sun, 0.0, time);
			timeSinceSunset = astro.getTimeSinceLastSet(sun, 0.0, time);
			if (logsearch)
				System.err.println("faj:checking for telfocus: ttSR = " + timeTillSunrise + " tsSS = "
						+ timeSinceSunset);
			if (telCalib.doTelfocusCalibration()
					&& (time - telCalibHist.getLastTelfocusCalibration() > telCalib.getTelfocusCalibrationInterval())
					&& (timeSinceSunset > TELFOCUS_SUNSET_OFFSET) && (timeTillSunrise > TELFOCUS_SUNRISE_OFFSET)
					&& round_midnight_and_m_le_10) {

				// find a focus instrument ...
				List<InstrumentDescriptor> ilist = ireg.listInstruments();
				Iterator<InstrumentDescriptor> ii = ilist.iterator();
				while (ii.hasNext()) {

					InstrumentDescriptor id = ii.next();

					InstrumentCapabilitiesProvider icp = ireg.getCapabilitiesProvider(id);
					InstrumentCapabilities icap = icp.getCapabilities();

					// is this a focus instrument
					if (!icap.isFocusInstrument())
						continue;

					InstrumentStatusProvider isp = ireg.getStatusProvider(id);
					InstrumentStatus inst = isp.getStatus();
					// skip if the instrument is shagged !
					if (!inst.isFunctional() || !inst.isOnline() || !inst.isEnabled())
						continue;

					list.add(new TelFocusCalibration(id, 0.0));
					// TODO required info: focusStarCatalog, telCalib,
					// focusInstr, focusInstrumentConfig;

				}
			}
		}

		return list;

	}

	@Override
	public Task getNextJob() {

		CalibrationOperation bestjob = null;
		double bestscore = -999.9;

		try {
			long now = System.currentTimeMillis();
			logsearch = true;
			List<CalibrationOperation> joblist = findAvailableJobs(now);

			Iterator<CalibrationOperation> il = joblist.iterator();
			while (il.hasNext()) {

				CalibrationOperation job = il.next();
				taskLog.log(1, CLASS, name, "getNextJob", "Checking job: " + job);
				if (job.getScore() > bestscore) {
					bestscore = job.getScore();
					bestjob = job;
				}
			}
		} catch (Exception e) {
			return null;
		}

		if (bestjob == null) {
			return null;
		} else if (bestjob instanceof MorningSkyFlatCalibration) {
			MorningSkyFlatCalibration msf = (MorningSkyFlatCalibration) bestjob;
			try {
				return createMorningSkyFlatsTask(msf);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else if (bestjob instanceof EveningSkyFlatCalibration) {
			EveningSkyFlatCalibration esf = (EveningSkyFlatCalibration) bestjob;
			try {
				return createEveningSkyFlatsTask(esf);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else if (bestjob instanceof TelFocusCalibration) {
			try {
				return createTelfocusTask();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return null;

	}

	private TelFocusCalibrationTask createTelfocusTask() throws Exception {

		// TODO fix this for O as telfocus instrument along with the
		// configuraiton

		return new TelFocusCalibrationTask(name + "/TELFOCUS", this, focusStarCatalog, telCalib, focusInstrumentConfig);
	}

	private SkyFlatCalibrationTask createMorningSkyFlatsTask(MorningSkyFlatCalibration msf) throws Exception {
		long now = System.currentTimeMillis();
		// long timeSinceStartAstroTwilight = computeStartAstroTwilight(now);
		// long timeTillSunrise = computeNextSunrise(now);

		SolarCalculator sunTrack = new SolarCalculator();
		Coordinates sun = sunTrack.getCoordinates(now);
		double sunlev = astro.getAltitude(sun, now);
		long timeTillSunMax = astro.getTimeUntilNextRise(sun, morningSkyflatsMaximumSolarElevation, now);

		InstrumentDescriptor instId = msf.getInstrumentDescriptor();
		InstrumentCalibrationProvider cp = ireg.getCalibrationProvider(instId);
		InstrumentCalibration ical = cp.getCalibrationRequirements();
		InstrumentCalibrationHistory ich = cp.getCalibrationHistory();

		Catalog blanks = Astrometry.getCatalog(instId.getInstrumentName() + "_BLANK");

		taskLog.log(1, "CAL:: Check calibration history for: " + instId + " " + ich);

		// see if this instrument is either offline or
		// impaired.

		InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
		InstrumentStatus inst = isp.getStatus();
		// skip if the instrument is shagged !
		if (!inst.isFunctional() || !inst.isOnline() || !inst.isEnabled())
			throw new Exception("Instrument not available for MSF");

		long timeAvailable = timeTillSunMax;

		taskLog.log(
				1,
				"CAL:: create Msf task: sunat: " + sunlev + ", ttmax("
						+ Math.toDegrees(morningSkyflatsMaximumSolarElevation) + ")deg " + (timeTillSunMax / 1000)
						+ "s");

		XBeamSteeringConfig beam = null;
		return new SkyFlatCalibrationTask(name + "/" + instId.getInstrumentName() + "_MORN_SKYFLAT", this, blanks,
				ical, ich, instId.getInstrumentName(), beam, timeAvailable, true);

	}

	private SkyFlatCalibrationTask createEveningSkyFlatsTask(EveningSkyFlatCalibration esf) throws Exception {

		// or TIME !!!
		long now = System.currentTimeMillis();

		long sunset = RCS_Controller.getObsDate().getSunset(); // check

		SolarCalculator sunTrack = new SolarCalculator();
		Coordinates sun = sunTrack.getCoordinates(now);
		double sunlev = astro.getAltitude(sun, now);
		// long timeSinceSunset = computeTimeSinceSunset(now);
		// long timeTillEndAstroTwilight = computeEndAstroTwilight(now);

		long timeTillEndAstroTwilight = astro.getTimeUntilNextSet(sun, eveningSkyflatsMinimumSolarElevation, now);
		long timeTillSunMin = astro.getTimeUntilNextSet(sun, eveningSkyflatsMinimumSolarElevation, now);
		InstrumentDescriptor instId = esf.getInstrumentDescriptor();
		InstrumentCalibrationProvider cp = ireg.getCalibrationProvider(instId);
		InstrumentCalibration ical = cp.getCalibrationRequirements();
		InstrumentCalibrationHistory ich = cp.getCalibrationHistory();

		Catalog blanks = Astrometry.getCatalog(instId.getInstrumentName() + "_BLANK");

		taskLog.log(1, "CAL:: Check calibration history for: " + instId + " " + ich);

		// see if this instrument is either offline or
		// impaired or disabled

		InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
		InstrumentStatus inst = isp.getStatus();
		// skip if the instrument is shagged !
		// TODO TODO TDO disabled
		if (!inst.isFunctional() || !inst.isOnline() || !inst.isEnabled())
			throw new Exception("Instrument not available for ESF");

		long timeAvailable = timeTillSunMin;

		taskLog.log(
				1,
				"CAL:: create Esf task: sunat: " + sunlev + ", ttmax("
						+ Math.toDegrees(eveningSkyflatsMinimumSolarElevation) + ")deg " + (timeTillSunMin / 1000)
						+ "s");

		// TODO we need different types of skyflat for diff insts eg IOO needs
		// bemsteeering
		XBeamSteeringConfig beam = null;
		return new SkyFlatCalibrationTask(name + "/" + instId.getInstrumentName() + "_EVEN_SKYFLAT", this, blanks,
				ical, ich, instId.getInstrumentName(), beam, timeAvailable, false);
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		opsLog.log(1, "Starting Calibration-Operations Mode.");
		FITS_HeaderInfo.current_USRDEFOC.setValue(new Double(0.0));
		FITS_HeaderInfo.clearAcquisitionHeaders();

	}

	/** Overridden to carry out specific work after completion. */
	@Override
	public void onCompletion() {
		super.onCompletion();
		opsLog.log(1, "Completed Calibration-Operations Mode.");
	}

	/**
	 * Overridden to handle completion of a subTask.
	 * 
	 * @param task
	 *            The subTask which has done.
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		long now = System.currentTimeMillis();

		// TODO let the tftask do this itself later
		if (task instanceof TelFocusCalibrationTask) {
			// update calib history
			// TODO This belongs in the telfocus task after onCompeltion(true)
			// and then we can also
			// do stuff for individual instruments, ie need to make sure the
			// relevant info is passed to these tasks
			telCalibHist.setLastTelfocusCalibration(now);
			opsLog.log(1, "Completed TELFOCUS calibration, updated calibration history");
		}

	}

	/**
	 * Overridden to handle failure of a subTask.
	 * 
	 * @param task
	 *            The subTask which has done.
	 */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
	}

	/** Compute backwards till SAT. */
	private long computeStartAstroTwilight(long time) {

		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		// look back till sun below -18, called in MT
		long t = time;
		while (t > time - 24 * 3600 * 1000L && sel > Math.toRadians(-18.0)) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t -= 30000L;
		}
		return time - t;

	}

	/** Compute backwards till SS. */
	private long computeTimeSinceSunset(long time) {
		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		// look back till sun below 0.0, called in ET
		long t = time;
		while (t > time - 24 * 3600 * 1000L && sel < Math.toRadians(0.0)) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t -= 30000L;
		}
		return time - t;
	}

	/** Compute forward till SR. */
	private long computeNextSunrise(long time) {
		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		// look back till sun above 0.0, called in MT
		long t = time;
		while (t < time + 24 * 3600 * 1000L && sel < Math.toRadians(0.0)) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t += 30000L;
		}
		return t - time;
	}

	/** Compute forward till EAT. */
	private long computeEndAstroTwilight(long time) {
		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		// look back till sun above -18.0, called in ET
		long t = time;
		while (t < time + 24 * 3600 * 1000L && sel > Math.toRadians(-18.0)) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t += 30000L;
		}
		return t - time;
	}

	/**
	 * Compute forward to next sunrise then backwards from next sunrise to
	 * specified sun elev angle (-ve).
	 */
	private long computeTimeOfNextRiseAboveElevation(long time, double elevation) {
		// first compute sunrise time.
		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		long sunrise = 0L;

		if (sel < 0.0) {
			// sun is down, compute forward
			long t = time;
			while (t < time + 24 * 3600 * 1000L && sel < Math.toRadians(0.0)) {
				sun = Astrometry.getSolarPosition(t);
				sel = sun.getAltitude(t, obsSite);
				t += 30000L;
			}
			sunrise = t;
		} else {
			// sun is up, jump one day then compute backwards
			long t = time + 24 * 3600 * 1000L;
			while (t > time && sel > Math.toRadians(0.0)) {
				sun = Astrometry.getSolarPosition(t);
				sel = sun.getAltitude(t, obsSite);
				t -= 30000L;
			}
			sunrise = t;
		}

		sun = Astrometry.getSolarPosition(sunrise);
		sel = sun.getAltitude(sunrise, obsSite);

		// look back till sun below elev
		long t = sunrise;
		while (t > time && sel > elevation) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t -= 30000L;
		}
		return sunrise - t;

	}

}

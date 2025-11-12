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
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.telemetry.DefaultGroupOperationsMonitor;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.sciops.DisplaySeq;
import ngat.rcs.sciops.GroupExecutionTask;
import ngat.rcs.calib.*;
import ngat.sms.GroupItem;
import ngat.net.*;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.instrument.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

import java.lang.reflect.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.rmi.*;

/**
 * This Task works in the background when no other MCA can operate.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: BackgroundControlAgent.java,v 1.3 2008/10/24 10:46:24 eng Exp eng $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/BackgroundControlAgent.java,v
 * $
 * </dl>
 * 
 * @author $Author: eng $
 * @version $Revision: 1.3 $
 */
public class BackgroundControlAgent extends DefaultModalTask implements EventSubscriber, Logging {

	protected static BackgroundControlAgent instance;

	/** Default latest start before sunrise (ms). */
	public static final long DEFAULT_LATEST_START_BEFORE_SUNRISE = 45 * 60 * 1000L;

	/** Default minimum lunar distance (degs). */
	public static final double DEFAULT_MIN_LUNAR_DISTANCE = 20.0;

	/** Default highest sun angle to stop ops in morning (degs). */
	public static final double DEFAULT_SUNRISE_STOP_ANGLE = -10.0;

	/** Default highest sun angle to start ops in evening (degs). */
	public static final double DEFAULT_SUNSET_START_ANGLE = -5.0;

	/** Standard date format. */
	static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	//protected boolean alertInProgress;

	/** Implements the lockfile associated with the group instantiation counter. */
	protected PersistentUniqueInteger puid;

	/**
	 * Counts the number of groups so far since start of observing. This number
	 * is maintained over reboots during the night.
	 */
	protected int groupCounter;

	/** True if we are using GSM otherwise we do nothing. */
	protected boolean useGreySuit;

	/** Minimum lunar distance from target. */
	protected double minLunarDistance;

	protected boolean badZone = false;
	protected double badNegLimit;
	protected double badPosLimit;

	/**
	 * Highest angle of sun (below horizon) in morning after which BG ops should
	 * stop (a negative angle).
	 */
	private double sunriseStopAngle;
	private double sunsetStartAngle;

	/** Star catalog. */
	protected Catalog primaryCatalog;
	protected Catalog secondaryCatalog;

	/** List of configs. */
	// protected Vector configs;

	private List primaryConfigs;
	private List secondaryConfigs;

	/** List of groups. */
	// protected Vector groups;

	private List primaryGroups;
	private List secondaryGroups;

	private boolean primaryActive;
	private boolean secondaryActive;

	/** Set to 1 or 2 to indicate if primary or secondary is to be preferred. */
	private int groupPreference;

	private Map groupTargetMap;

	/** Primary instrument. */
	private String primaryInstrumentName;

	/** Second instrument. */
	private String secondaryInstrumentName;

	private InstrumentDescriptor primaryInstId;

	private InstrumentDescriptor secondaryInstId;

	private String secondaryUpperBeamElementName;

	private String secondaryLowerBeamElementName;

	private XProgram useProgram;

	private XUser usePI;

	private XProposal useProposal;

	private XTag useTag;

	/** Counts groups executed. */
	private int ecount;

	/** Monitors group operations. */
	private DefaultGroupOperationsMonitor gom;

	private ISite site;
	private AstrometryCalculator ast;
	private AstrometrySiteCalculator astro;

	private SolarCalculator sunTrack;

	private LunarCalculator moonTrack;

	/**
	 * Create a BackgroundControlAgent using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public BackgroundControlAgent(String name, TaskManager manager) {
		super(name, manager);

		// configs = new Vector();
		// groups = new Vector();
		primaryGroups = new Vector();
		secondaryGroups = new Vector();
		primaryConfigs = new Vector();
		secondaryConfigs = new Vector();
		groupTargetMap = new HashMap();
		site = RCS_Controller.controller.getSite();
		astro = new BasicAstrometrySiteCalculator(site);
		ast = new BasicAstrometryCalculator();
		sunTrack = new SolarCalculator();
		moonTrack = new LunarCalculator(site);
		// by default we prefer primary groups
		groupPreference = 1;
	}

	/**
	 * Creates the initial instance of the BackgroundControlAgent
	 * 
	 */
	@Override
	public void initialize(ModalTask tm) {
		instance = (BackgroundControlAgent) tm;
	}

	/** Returns a reference to the singleton instance. */
	public static ModalTask getInstance() {
		return instance;
	}

	/**
	 * Configure from File. Does nothing at present will be used to:- Set
	 * timelimits etc.
	 * 
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

		// identity stuff for new phase2 usage
		// / scope_getAgentId() / tag = scope_RCS / user= Background / prop =
		// Standards");

		useTag = new XTag();
		useTag.setName("LTOps");

		usePI = new XUser("LT_RCS");

		useProgram = new XProgram("Background");

		useProposal = new XProposal("Standards");
		useProposal.setPriority(IProposal.PRIORITY_Z);
		useProposal.setPriorityOffset(0.0);

		useGreySuit = (config.getProperty("grey.suit") != null);

		/*
		 * int agUsage = TelescopeConfig.AGMODE_NEVER; if
		 * (config.getProperty("ag.optional") != null) agUsage =
		 * TelescopeConfig.AGMODE_OPTIONAL;
		 * 
		 * if (config.getProperty("ag.mandatory") != null) agUsage =
		 * TelescopeConfig.AGMODE_MANDATORY;
		 */

		int agUsage = IAutoguiderConfig.OFF;
		if (config.getProperty("ag.optional") != null)
			agUsage = IAutoguiderConfig.ON_IF_AVAILABLE;

		if (config.getProperty("ag.mandatory") != null)
			agUsage = IAutoguiderConfig.ON;

		// what instruments do we use...
		primaryActive = (config.getProperty("primary.active") != null);
		secondaryActive = (config.getProperty("secondary.active") != null);

		primaryInstrumentName = config.getProperty("primary.instrument.name", "LOCI");
		primaryInstId = ireg.getDescriptor(primaryInstrumentName);

		secondaryInstrumentName = config.getProperty("secondary.instrument.name", "IO:O");
		secondaryInstId = ireg.getDescriptor(secondaryInstrumentName);
		secondaryUpperBeamElementName = config.getProperty("secondary.upper.beam.element", "Clear");
		secondaryLowerBeamElementName = config.getProperty("secondary.lower.beam.element", "AlMirror");

		groupPreference = config.getIntValue("instrument.preference", 1);

		int bin = config.getIntValue("binning", 2);

		minLunarDistance = Math.toRadians(config.getDoubleValue("min.lunar.distance", DEFAULT_MIN_LUNAR_DISTANCE));

		// Is there an azimuth bad zone ?
		badZone = (config.getProperty("bad.zone") != null);
		badNegLimit = Math.toRadians(config.getDoubleValue("bad.zone.min", 160.0));
		badPosLimit = Math.toRadians(config.getDoubleValue("bad.zone.max", 180.0));

		// how late (before sunrise) can we still start a group?
		// latestStartBeforeSunrise =
		// config.getLongValue("latest.start.sunrise.offset",
		// DEFAULT_LATEST_START_BEFORE_SUNRISE);

		// how high can sun get before stopping groups.
		sunriseStopAngle = Math.toRadians(config.getDoubleValue("sunrise.stop.angle", DEFAULT_SUNRISE_STOP_ANGLE));
		sunsetStartAngle = Math.toRadians(config.getDoubleValue("sunset.start.angle", DEFAULT_SUNSET_START_ANGLE));
		try {

			File primaryCatfile = new File(config.getProperty("primary.catalog", "config/loci_std.cat"));

			primaryCatalog = Astrometry.loadCatalog("LOCI_STD", primaryCatfile);
			taskLog.log(1, CLASS, name, "Config", "BGCA loaded " + primaryCatalog.size() + " targets from "
					+ primaryCatalog.getCatalogName());

			File secondaryCatfile = new File(config.getProperty("secondary.catalog", "config/io:o_std.cat"));

			secondaryCatalog = Astrometry.loadCatalog("IO:O_STD", secondaryCatfile);
			taskLog.log(1, CLASS, name, "Config", "BGCA loaded " + secondaryCatalog.size() + " targets from "
					+ secondaryCatalog.getCatalogName());

			// Primary configs.
			int nconfigs = config.getIntValue("count.primary.configs", 0);
			for (int ic = 0; ic < nconfigs; ic++) {

				String cid = config.getProperty("primary.config." + ic + ".ID");
				String filter = config.getProperty("primary.config." + ic + ".filter");

				XImagerInstrumentConfig ccdConfig = new XImagerInstrumentConfig(cid);

				if (ccdConfig == null) {
					throw new IllegalArgumentException("BackgroundControlAgent: InstrumentConfig " + ic
							+ " was not generated: ");
				}

				// new stuff
				XDetectorConfig xdet = new XDetectorConfig();
				xdet.setName("bg" + bin + "x" + bin);
				xdet.setXBin(bin);
				xdet.setYBin(bin);
				XFilterSpec fspec = new XFilterSpec();
				XFilterDef filterDef = new XFilterDef(filter);
				fspec.addFilter(filterDef);
				XImagerInstrumentConfig xcfg = new XImagerInstrumentConfig(cid);
				xcfg.setFilterSpec(fspec);
				xcfg.setInstrumentName(primaryInstrumentName);
				xcfg.setDetectorConfig(xdet);
				primaryConfigs.add(xcfg);
				// end new

			}

			// Secondary configs.
			nconfigs = config.getIntValue("count.secondary.configs", 0);
			for (int ic = 0; ic < nconfigs; ic++) {

				String cid = config.getProperty("secondary.config." + ic + ".ID");
				String filter = config.getProperty("secondary.config." + ic + ".filter");

				XImagerInstrumentConfig ccdConfig = new XImagerInstrumentConfig(cid);

				if (ccdConfig == null) {
					throw new IllegalArgumentException("BackgroundControlAgent: InstrumentConfig " + ic
							+ " was not generated: ");
				}

				// new stuff
				XDetectorConfig xdet = new XDetectorConfig();
				xdet.setName("bg" + bin + "x" + bin);
				xdet.setXBin(bin);
				xdet.setYBin(bin);
				XFilterSpec fspec = new XFilterSpec();
				XFilterDef lf = new XFilterDef(filter);

				fspec.addFilter(lf);
				fspec.addFilter(new XFilterDef("clear"));
				fspec.addFilter(new XFilterDef("clear"));

				XImagerInstrumentConfig xcfg = new XImagerInstrumentConfig(cid);
				xcfg.setFilterSpec(fspec);
				xcfg.setInstrumentName(secondaryInstrumentName);
				xcfg.setDetectorConfig(xdet);
				secondaryConfigs.add(xcfg);
				// end new

			}

			// Make up the groups per target and config pair.(n*m).
			List targets = primaryCatalog.listTargets();
			Iterator it = targets.iterator();
			while (it.hasNext()) {
				ExtraSolarSource src = (ExtraSolarSource) it.next();

				String scope = RCS_Controller.controller.getTelescopeId();

				XGroup xgroup = new XGroup();
				xgroup.setName("BG:(" + primaryInstrumentName + "_" + src.getName() + ")");
				XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
				// add slew and rot and init aperture
				XExtraSolarTarget target = new XExtraSolarTarget(src.getName());
				target.setRa(src.getRA());
				target.setDec(src.getDec());
				target.setEpoch(2000.0);
				target.setFrame(ReferenceFrame.FK5);

				XRotatorConfig rotator = new XRotatorConfig(IRotatorConfig.CARDINAL, 0.0, primaryInstrumentName);
				XSlew slew = new XSlew(target, rotator, false);
				root.addElement(new XExecutiveComponent("slew", slew));
				
				XAcquisitionConfig aperture = new XAcquisitionConfig(
						IAcquisitionConfig.INSTRUMENT_CHANGE,
						primaryInstrumentName, 
						primaryInstrumentName, 
						false,IAcquisitionConfig.PRECISION_NOT_SET);
				root.addElement(new XExecutiveComponent("focal-plane", aperture));

				// autoguide if required
				if (agUsage == IAutoguiderConfig.ON)
					root.addElement(new XExecutiveComponent("ag-on", new XAutoguiderConfig(IAutoguiderConfig.ON,
							"CASSEGRAIN")));
				else if (agUsage == IAutoguiderConfig.ON_IF_AVAILABLE)
					root.addElement(new XExecutiveComponent("ag-on", new XAutoguiderConfig(
							IAutoguiderConfig.ON_IF_AVAILABLE, "CASSEGRAIN")));

				// add primary configs
				for (int ic = 0; ic < primaryConfigs.size(); ic++) {

					double expose = config.getDoubleValue("primary.config." + ic + ".expose");
					int repeat = config.getIntValue("primary.config." + ic + ".repeat");

					IInstrumentConfig xconfig = (IInstrumentConfig) primaryConfigs.get(ic);
					root.addElement(new XExecutiveComponent("config", new XInstrumentConfigSelector(xconfig)));
					XMultipleExposure multrun = new XMultipleExposure(expose, repeat, true);
					multrun.setStandard(true);
					root.addElement(new XExecutiveComponent("multrun", multrun));

				}

				// AUTO OFF
				root.addElement(new XExecutiveComponent("ag-off", new XAutoguiderConfig(IAutoguiderConfig.OFF,
						"CASSEGRAIN")));

				xgroup.setTimingConstraint(new XFlexibleTimingConstraint());
				GroupItem groupItem = new GroupItem(xgroup, root);
				groupItem.setTag(useTag);
				groupItem.setUser(usePI);
				groupItem.setProgram(useProgram);
				groupItem.setProposal(useProposal);
				primaryGroups.add(groupItem);
				groupTargetMap.put(xgroup.getName(), target);

				System.err.println(DisplaySeq.display(0, root));

			} // next primary group

			// Make up the groups per target and config pair.(n*m).
			XOpticalSlideConfig secondaryUpper = new XOpticalSlideConfig(IOpticalSlideConfig.SLIDE_UPPER);
			secondaryUpper.setElementName(secondaryUpperBeamElementName);
			XOpticalSlideConfig secondaryLower = new XOpticalSlideConfig(IOpticalSlideConfig.SLIDE_LOWER);
			secondaryLower.setElementName(secondaryLowerBeamElementName);

			XBeamSteeringConfig beam = new XBeamSteeringConfig(secondaryUpper, secondaryLower);

			targets = secondaryCatalog.listTargets();
			it = targets.iterator();
			while (it.hasNext()) {
				ExtraSolarSource src = (ExtraSolarSource) it.next();

				String scope = RCS_Controller.controller.getTelescopeId();

				XGroup xgroup = new XGroup();
				xgroup.setName("BG:(" + secondaryInstrumentName + "_" + src.getName() + ")");
				XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
				// add slew and rot and init aperture
				XExtraSolarTarget target = new XExtraSolarTarget(src.getName());
				target.setRa(src.getRA());
				target.setDec(src.getDec());
				target.setEpoch(2000.0);
				target.setFrame(ReferenceFrame.FK5);

				XRotatorConfig rotator = new XRotatorConfig(IRotatorConfig.CARDINAL, 0.0, secondaryInstrumentName);
				XSlew slew = new XSlew(target, rotator, false);
				root.addElement(new XExecutiveComponent("slew", slew));

				XAcquisitionConfig aperture = new XAcquisitionConfig(IAcquisitionConfig.INSTRUMENT_CHANGE,
						secondaryInstrumentName, secondaryInstrumentName, false, IAcquisitionConfig.PRECISION_NOT_SET);
				root.addElement(new XExecutiveComponent("focal-plane", aperture));

				// TODO remove beam steering for now....
				// root.addElement(new XExecutiveComponent("beam-steer", beam));

				// autoguide if required
				if (agUsage == IAutoguiderConfig.ON)
					root.addElement(new XExecutiveComponent("ag-on", new XAutoguiderConfig(IAutoguiderConfig.ON,
							"CASSEGRAIN")));
				else if (agUsage == IAutoguiderConfig.ON_IF_AVAILABLE)
					root.addElement(new XExecutiveComponent("ag-on", new XAutoguiderConfig(
							IAutoguiderConfig.ON_IF_AVAILABLE, "CASSEGRAIN")));

				// add secondary configs
				for (int ic = 0; ic < secondaryConfigs.size(); ic++) {

					double expose = config.getDoubleValue("secondary.config." + ic + ".expose");
					int repeat = config.getIntValue("secondary.config." + ic + ".repeat");

					IInstrumentConfig xconfig = (IInstrumentConfig) secondaryConfigs.get(ic);
					root.addElement(new XExecutiveComponent("config", new XInstrumentConfigSelector(xconfig)));
					XMultipleExposure multrun = new XMultipleExposure(expose, repeat, true);
					multrun.setStandard(true);
					root.addElement(new XExecutiveComponent("multrun", multrun));

				}

				// AUTO OFF
				root.addElement(new XExecutiveComponent("ag-off", new XAutoguiderConfig(IAutoguiderConfig.OFF,
						"CASSEGRAIN")));

				xgroup.setTimingConstraint(new XFlexibleTimingConstraint());
				GroupItem groupItem = new GroupItem(xgroup, root);
				groupItem.setTag(useTag);
				groupItem.setUser(usePI);
				groupItem.setProgram(useProgram);
				groupItem.setProposal(useProposal);
				secondaryGroups.add(groupItem);
				groupTargetMap.put(xgroup.getName(), target);

				System.err.println(DisplaySeq.display(0, root));

			} // next secondary group

		} catch (Exception e) {
			throw new IllegalArgumentException("Error parsing bg-config: " + e);
		}
		taskLog.log(1, CLASS, name, "Config", "BGCA was configured ok");
	}

	/**
	 * Overridden to carry out specific work after the init() method is called.
	 * Sets a number of FITS headers and subscribes to any required events.
	 */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(1, CLASS, name, "onInit", "\n********************************************************"
				+ "\n** Background Operations Control Agent is initialized **"
				+ "\n********************************************************\n");
		opsLog.log(1, "Starting Background-Operations Mode.");
		FITS_HeaderInfo.current_TELMODE.setValue("BACKGROUND");
		// FITS_HeaderInfo.current_COMPRESS.setValue("NONE");

		FITS_HeaderInfo.clearAcquisitionHeaders();

		// Always override until we decide not to.
		ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);
		gom = RCS_Controller.controller.getGroupOperationsMonitor();
		
		puid = new PersistentUniqueInteger("%%group");

		try {
			groupCounter = puid.get();
		} catch (Exception e) {
			System.err.println("** WARNING - Unable to read initial group counter: " + e);
			taskLog.log(1, CLASS, name, "onInit", "Error reading initial group counter: " + e);
			taskLog.dumpStack(1, e);
			groupCounter = 0;
		}
		
		
	}

	/** Deal with failed subtask. */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
	}

	/**
	 * Overridden to carry out specific work when a subtask fails.
	 */
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
	}

	@Override
	public void onAborting() {
		super.onAborting();
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		opsLog.log(1, "Completed Background-Operations Mode.");
	}

	/** Return true if wants control at time. */
	@Override
	public boolean wantsControl(long time) throws RemoteException {
		Site site = RCS_Controller.controller.getObservatorySite();

		Position sun = Astrometry.getSolarPosition(time);

		double sunElev = sun.getAltitude(time, site);
		boolean sunup = (sunElev > 0.0);
		// WHY did we bother to evaluate these sun things???
		return true;
	}

	/** How long till this controller will definitely want control from time. */
	@Override
	public long nextWantsControl(long time) throws RemoteException {
		return time + 24 * 3600 * 1000L;
	}

	/**
	 * Overriden to return the time at which this ModalControlAgent will next
	 * request control. ##### CURRENTLY FAKED TO RETURN NOW ########
	 * 
	 * @return Time when this MCA will next want/be able to take control (millis
	 *         1970).
	 */
	@Override
	public long demandControlAt() {
		ObsDate obsDate = RCS_Controller.getObsDate();
		long now = System.currentTimeMillis();
		return now + 24 * 3600 * 1000L;
	}

	/**
	 * Override to return <i>true</i> at ALL times of night.
	 */
	@Override
	public boolean acceptControl() {
		return true;
	}

	/**
	 * Returns the next available job:- If GSM is set then always a GSM with 5
	 * sec delay for now, otherwise we do nothing.
	 */
	@Override
	public Task getNextJob() {

		long now = System.currentTimeMillis();

		Coordinates sun = null;
		double selnow = 0.0;
		double sel30 = 0.0;
		try {
			sun = sunTrack.getCoordinates(now);
			selnow = astro.getAltitude(sun, now);
			sel30 = astro.getAltitude(sun, now + 30 * 60 * 1000L);

			// Position sun30 = Astrometry.getSolarPosition(now + 30 * 60 *
			// 1000L);
			// double sel30 = sun30.getAltitude(now + 30 * 60 * 1000L, obsSite);

			if (selnow < sel30) {
				// its morning so check the sun angle against the limit value,
				if (selnow >= sunriseStopAngle) {
					taskLog.log(1, CLASS, name, "getNextJob", "Sun elevation " + Position.toDegrees(selnow, 2)
							+ " is already past sunrise limit angle: " + Position.toDegrees(sunriseStopAngle, 2));
					return null;
				}
				long timeToSunriseLimit = computeNextSunrise(now, sunriseStopAngle);

				if (timeToSunriseLimit < 5 * 60 * 1000L) {
					taskLog.log(1, CLASS, name, "getNextJob",
							"Too close to sunrise (" + Position.toDegrees(sunriseStopAngle, 2) + " degs), only "
									+ (timeToSunriseLimit / 60000) + "m remaining, need 5m");
					return null;
				}

			} else {
				// its evening so check the sun angle against the limit value,
				if (selnow >= sunsetStartAngle) {
					taskLog.log(1, CLASS, name, "getNextJob", "Sun elevation " + Position.toDegrees(selnow, 2)
							+ " has not reached sunset limit angle: " + Position.toDegrees(sunsetStartAngle, 2));
					return null;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			taskLog.log(1, CLASS, name, "getNextJob", "Unable to determine sun data");
			return null;
		}

		double maxScore = -999.9;
		int index = -1;
		double domeLimit = RCS_Controller.getDomelimit();
		// Position moon = Astrometry.getLunarPosition();

		// check the primary instrument is online
		boolean primaryAvailable = true;
		taskLog.log(1, CLASS, name, "getNextJob", "Checking status: Inst#1: " + primaryInstId);
		try {
			
			InstrumentStatusProvider isp = ireg.getStatusProvider(primaryInstId);
			InstrumentStatus status = isp.getStatus();
			//taskLog.log(1, CLASS, name, "getNextJob", "Status: "+ primaryInstId + " is: " + status);
			if (!status.isOnline()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + primaryInstrumentName
						+ " is offline");
				primaryAvailable = false;
			}
			if (!status.isEnabled()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + primaryInstrumentName
						+ " is disabled");
				primaryAvailable = false;
			}
			if (!status.isFunctional()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + primaryInstrumentName
						+ " is impaired");
				primaryAvailable = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			taskLog.log(1, CLASS, name, "getNextJob", "Cannot determine status of primary background instrument: "
					+ primaryInstrumentName);
			primaryAvailable = false;
		}

		boolean secondaryAvailable = true;
		taskLog.log(1, CLASS, name, "getNextJob", "Checking status: Inst#2: " + secondaryInstId);
		try {
			InstrumentStatusProvider isp = ireg.getStatusProvider(secondaryInstId);
			InstrumentStatus status = isp.getStatus();
			//taskLog.log(1, CLASS, name, "getNextJob", "Status : " + secondaryInstId + " is: " + status);
			if (!status.isOnline()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + secondaryInstrumentName
						+ " is offline");
				secondaryAvailable = false;
			}
			if (!status.isEnabled()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + secondaryInstrumentName
						+ " is disabled");
				secondaryAvailable = false;
			}
			if (!status.isFunctional()) {
				taskLog.log(1, CLASS, name, "getNextJob", "Background Instrument: " + secondaryInstrumentName
						+ " is impaired");
				secondaryAvailable = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			taskLog.log(1, CLASS, name, "getNextJob", "Cannot determine status of background instrument: "
					+ secondaryInstrumentName);
			secondaryAvailable = false;
		}

		// if we prefer primary then check its ok and select a group
		List useGroups = null;
		Catalog catalog = null;
		if (groupPreference == 1) {
			if (primaryAvailable && primaryActive) {
				useGroups = primaryGroups;
				catalog = primaryCatalog;
			} else if (secondaryAvailable && secondaryActive) {
				useGroups = secondaryGroups;
				catalog = secondaryCatalog;
			} else {
				useGroups = null;
			}
		} else {
			if (secondaryAvailable && secondaryActive) {
				useGroups = secondaryGroups;
				catalog = secondaryCatalog;
			} else if (primaryAvailable && primaryActive) {
				useGroups = primaryGroups;
				catalog = primaryCatalog;
			} else {
				useGroups = null;
			}
		}

		if (useGroups == null) {
			taskLog.log(1, CLASS, name, "getNextJob", "Primary and secondary instruments are both unavailable");
			return null;
		}
		// Check for most suitable target.
		taskLog.log(1, CLASS, name, "getNextJob",
				"Checking " + catalog.size() + " targets in " + catalog.getCatalogName()
						+ " for suitability, with dome limit " + Position.toDegrees(domeLimit, 3)
						+ ", minimum lunar distance " + Position.toDegrees(minLunarDistance, 3) + " degs");

		GroupItem selectedGroup = null;

		Iterator ig = useGroups.iterator();
		while (ig.hasNext()) {

			GroupItem group = (GroupItem) ig.next();
			XExtraSolarTarget target = (XExtraSolarTarget) groupTargetMap.get(group.getName());
			Coordinates c = new Coordinates(target.getRa(), target.getDec());
			TargetTrackCalculator targetTrack = new BasicTargetCalculator(target, site);

			try {
				double el = astro.getAltitude(c, now);
				double ttset = astro.getTimeUntilNextSet(c, domeLimit, now);

				// how far from moon in next 30 minutes ?
				double lunsep = ast.getClosestPointOfApproach(moonTrack, targetTrack, now, now + 30 * 60 * 1000L);

				// boolean r1 = ((((azStart < badNegLimit) && (azEnd <
				// badNegLimit))));
				// boolean r2 = ((((azStart > badPosLimit) && (azEnd >
				// badPosLimit))));

				// boolean willRemainInLimits = (!badZone || (badZone && (r1 ||
				// r2)));

				taskLog.log(1, CLASS, name, "getNextJob", "Trying target: " + target.getName() + ", Elevation="
						+ Position.toDegrees(el, 3) + " degs" + ", Lunar distance=" + Position.toDegrees(lunsep, 3)
						+ " degs" + ", TTS(dome)=" + (ttset / 1000.0) + " sec");

				double score = 1.0 * (el / Math.toRadians(90.0)) + 0.5 * (ttset / (12.0 * 3600000.0)) + 0.25
						* (lunsep / Math.toRadians(180.0));

				taskLog.log(1, CLASS, name, "getNextJob", "Score " + score);

				if (ttset > 30 * 60 * 1000L && el > domeLimit && lunsep > Math.toRadians(20.0)) {
					taskLog.log(1, CLASS, name, "getNextJob", "Target " + target.getName() + " OK, Score " + score);
					if (score > maxScore) {
						maxScore = score;
						selectedGroup = group;
					}
				}
			} catch (Exception e) {
				taskLog.log(1, CLASS, name, "getNextJob",
						"Unable to determine target information for: " + target.getName() + " due to: " + e);
			}
		}

		// if (index == -1) {
		if (selectedGroup == null) {
			taskLog.log(1, CLASS, name, "getNextJob", "There are no feasible background observations");
			return null;
		}

		taskLog.log(1, CLASS, name, "onInit", "The group to execute is: " + selectedGroup);

		/*
		 * Iterator it = selectedGroup.listAllObservations(); while
		 * (it.hasNext()) { Observation obs = (Observation) it.next();
		 * taskLog.log(1, CLASS, name, "onInit", "BG:: Do observation: " + obs);
		 * }
		 */

		// GroupItem groupItem =
		// (GroupItem)groupNamesMap.get(selectGroup.getName());
		opsLog.log(1, "Starting Background Group-Sequence: " + groupCounter);

		// FITS set GROUP_UNIQUE_ID groupCounter
		FITS_HeaderInfo.current_GRPUID.setValue(new Integer(groupCounter));
		
		GroupExecutionTask gt = new GroupExecutionTask(name + "/BGExec:" + (ecount++), this, selectedGroup);

		// Group_Task gt = new Group_Task("BGExec:" + (ecount++), this,
		// selectGroup, groupItem);

		// we would like to pass the ecount into the group name for info but no
		// way to access the contained group !
		// ALREADY taken care of by GXT
		// ...gom.notifyListenersGroupSelected(selectedGroup);

		return gt;

	}

	@Override
	protected TaskList createTaskList() {
		return taskList;
	}

	/**
	 * EventSubscriber method. <br>
	 */
	@Override
	public void notifyEvent(String eventId, Object data) {
	}

	/**
	 * EventSubscriber method.
	 */
	@Override
	public String getSubscriberId() {
		return name;
	}

	/** Compute forward till SR above elevation. */
	private long computeNextSunrise(long time, double elevation) {
		Site obsSite = RCS_Controller.controller.getObservatorySite();
		Position sun = Astrometry.getSolarPosition(time);
		double sel = sun.getAltitude(time, obsSite);

		// look fwd till sun above 0.0
		long t = time;
		while (t < time + 24 * 3600 * 1000L && sel < elevation) {
			sun = Astrometry.getSolarPosition(t);
			sel = sun.getAltitude(t, obsSite);
			t += 30000L;
		}
		return t - time;
	}
}

/**
 * $Log: BackgroundControlAgent.java,v $ /** Revision 1.3 2008/10/24 10:46:24
 * eng /** added wantsControl /** /** Revision 1.2 2007/07/05 11:30:03 snf /**
 * checkin /** /** Revision 1.1 2006/12/12 08:28:54 snf /** Initial revision /**
 * /** Revision 1.1 2006/05/17 06:33:38 snf /** Initial revision /** /**
 * Revision 1.1 2002/09/16 09:38:28 snf /** Initial revision /**
 */

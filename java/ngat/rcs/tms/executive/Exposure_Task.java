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
package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.newstatemodel.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.sciops.ChangeTracker;
import ngat.rcs.sciops.ConfigTranslator;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.phase2.*;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatusProvider;
import ngat.icm.Wavelength;
import ngat.instrument.*;
import ngat.tcm.DefaultAutoguiderMonitor;
import ngat.tcm.DefaultTrackingMonitor;
import ngat.tcm.SciencePayload;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.GUI_RCS.*;
// experimental skymodel stuff 
import ngat.ems.*;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * A leaf Task for performing an Instrument exposure. The observation passed in
 * is checked and an appropriate EXPOSE command subclass is generated and sent
 * to the relevant instrument control system. In future expect the
 * ngat.phase2.Observation parameter to be swapped for an ngat.phase2.Exposure
 * or telescope-specific subclass which will then indicate the Instrument(s),
 * any offsets and potentially multi-arm or instrument exposure lengths etc.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Exposure_Task.java,v 1.6 2007/09/27 08:23:19 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Exposure_Task.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.6 $
 */
public class Exposure_Task extends Default_TaskImpl implements AutoguiderAdjustmentListener {

	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, EXPOSE = 100
	
	/** Maximum time we can go unguided (if mandatory guiding). */
	public static final double DEFAULT_AG_LOST_TIME = 10000.0;

	/**
	 * Minimum total time of an observation for which we would bother to switch
	 * on the AG monitor.
	 */
	public static final double DEFAULT_MIN_OBS_GUIDE_TIME = 4 * 60 * 1000.0;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss z");

	public static final String CLASS = "Exposure_Task";

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 10000L;

	/** Signal category indicating that an exposure has completed. */
	public static final int EXPOSURE_COMPLETE = 602101;

	/**
	 * Signal category indicating that a Multrun single exposure (DPd) has
	 * completed.
	 */
	public static final int EXPOSURE_ELEMENT = 602102;

	/**
	 * Signal category indicating that a Multrun single exposure MAX_COUNTS is
	 * available.
	 */
	public static final int EXPOSURE_COUNTS = 602103;

	/**
	 * Signal category indicating that a Multrun single exposure SEEING is
	 * available.
	 */
	public static final int EXPOSURE_SEEING = 602104;

	/**
	 * Signal category indicating that a Multrun single exposure X is available.
	 */
	public static final int EXPOSURE_X = 602105;

	/**
	 * Signal category indicating that a Multrun single exposure Y is available.
	 */
	public static final int EXPOSURE_Y = 602106;

	/** Signal category indicating that a Multrun single exposure has completed. */
	public static final int EXPOSURE_FILE = 602107;

	public static final int EXPOSURE_DATA = 602108;

	/** ### TEMP Default maximum seeing to use for predictor update. */
	public static final double DEFAULT_MAX_SEEING = 3.0;

	/** Stores the times when the exposure DP reduction data was received. */
	protected long[] exposureTimes;

	/** Stores the az at times when the exposure DP reduction data was received. */
	protected double[] storeAz;

	/**
	 * Stores the alt at times when the exposure DP reduction data was received.
	 */
	protected double[] storeAlt;

	/** Number of exposure. */
	protected int numExposures;

	/** Stores the seeing data from each reduced image. */
	protected double[] seeing;

	/**
	 * ### TEMP to determine the max seeing we will use to update the CCDSeeing.
	 */
	protected double max_seeing;

	/** Counts number of images completed so far. */
	protected int countCompletedExposures;

	/** Counts number of images reduced so far. */
	protected int countReducedExposures;

	/** The Observation exposure which is to be performed. */
	// protected Observation observation;

	protected IExposure exposure;

	/** The Instrument to be used. */
	protected String instrumentName;

	private InstrumentDescriptor instId;

	protected InstrumentCapabilities icap;

	protected IInstrumentConfig instConfig;

	/** Ref to StateModel as EnvironmentChangeListener. */
	protected ngat.rcs.newstatemodel.StandardStateModel tsm;

	protected SciencePayload payload;
	
	private String targetName;

	protected InstrumentRegistry ireg;

	protected DefaultTrackingMonitor tm;
	
	protected DefaultAutoguiderMonitor am;

	/** True if this exposure represents a standard exposure. */
	// protected boolean standard;

	/** True if we only want to glance at target. */
	// protected boolean glance;

	/** True if this exposure refers to a single FIXED TIME exposure. */
	// protected boolean fixed;

	/** Holds the time for a FIXED exposure if applicable. */
	// protected long fixedTime;

	// /private boolean durationLimit;

	// /private double exposureDuration;

	/** Indicates that DP(RT) should be called by ICS on raw image. */
	protected boolean dprt = true;
	
	/** Track status of execution.*/
	private ChangeTracker collator;
	
	/**
	 * Used for telemtry notification - this is quite crap really based on old
	 * p2.path notation.
	 */
	protected String obsPathName;

	/** Used in stats logging - effectively the total time of exposing. */
	private long exposureTotal;

	/**
	 * Create an Exposure_Task using the supplied Observation and settings. 
	 * */
	
	
	
	/** Create an Exposure_Task using the supplied settings.
	 * @param name The name of this task.
	 * @param manager The manager.
	 * @param exposure An instance of IExposure.
	 * @param instrumentName The name of the instrument to be used.
	 * @param obsPathName FITS header and observation update identification information.
	 */
	public Exposure_Task(String name, TaskManager manager, IExposure exposure, String instrumentName, String obsPathName, ChangeTracker collator) {
		
		super(name, manager, instrumentName);
		this.exposure = exposure;
		this.obsPathName = obsPathName;
		this.instrumentName = instrumentName;
		this.collator = collator;
		
		if (exposure instanceof XMultipleExposure)
			numExposures = ((XMultipleExposure) exposure).getRepeatCount();

		exposureTimes = new long[numExposures];
		seeing = new double[numExposures];
		storeAz = new double[numExposures];
		storeAlt = new double[numExposures];

		countCompletedExposures = 0;
		countReducedExposures = 0;

		// ----------------------------------------------------
		// Decide on the Instrument. Set the connection to use.
		// ----------------------------------------------------
		// InstrumentConfig instconfig = observation.getInstrumentConfig();

		// instrument = Instruments.findInstrumentFor(instconfig);
		// get science pyaload ref

		instId = new InstrumentDescriptor(instrumentName);

		try {
			payload = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload();
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640102, "Unable to locate science payload", e);
			return;
		}

		try {
			ireg = RCS_Controller.controller.getInstrumentRegistry();
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640104, "Unable to locate instrument registry", e);
			return;
		}

		// Inst should not be null or the Mgr would have failed probably?
		InstrumentCapabilitiesProvider icp = null;
		try {
			icp = ireg.getCapabilitiesProvider(instId);
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640103, "Unable to locate instrument registry", e);
			return;
		}
		if (icp == null) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640105, "Cannot select instrument for Config: "
					+ config.getClass().getName(), null);
			return;
			// FATAL
		} else {
			try {
				icap = icp.getCapabilities();
				// instConfig = icp.getCurrentConfig();
				// createConnection(instconfig.getClass());
				// createConnection(instrumentName);
				logger.log(1, "Exposure_Task", name, "Constructor", "Obtained capabilities for: " + instrumentName
						+ ", " + icap);
			} catch (Exception e) {
				logger.log(1, "Exposure_Task", name, "Constructor", "Unable to obtain capabilities for: "
						+ instrumentName + " : " + e);
				failed = true;
				errorIndicator = new BasicErrorIndicator(640101, "Capabilities unobtainable for : " + instrumentName, e);
				return;
				// FATAL
			}
		}

		try {

			InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
			instConfig = isp.getCurrentConfig();
			logger.log(1, "Exposure_Task", name, "Constructor", "Obtained status info for: " + instrumentName
					+ ", current config: " + instConfig);

		} catch (Exception e) {
			logger.log(1, "Exposure_Task", name, "Constructor", "Unable to obtain status for: " + instrumentName
					+ " : " + e);
			failed = true;
			errorIndicator = new BasicErrorIndicator(640106, "Status unobtainable for : " + instrumentName, e);
			return;
			// FATAL
		}

		// get a ref to the TMS which we will use to switch on and off the
		// stabilityMonitoring

		tsm = RCS_Controller.controller.getTestStateModel();

		tm = RCS_Controller.controller.getTrackingMonitor();
		am = RCS_Controller.controller.getAutoguiderMonitor();

	}

	@Override
	public void reset() {
		super.reset();
		countCompletedExposures = 0;
		countReducedExposures = 0;
	}

	/**
	 * @return the fixedTime
	 */
	// public long getFixedTime() {
	// return fixedTime;
	// }

	/**
	 * @param fixedTime
	 *            the fixedTime to set
	 */
	// public void setFixedTime(long fixedTime) {
	// this.fixedTime = fixedTime;
	// }

	/**
	 * @param fixed
	 *            the fixed to set
	 */
	// public void setFixed(boolean fixed) {
	// this.fixed = fixed;
	// }

	/** Sets whetehr to DP(RT) images. */
	public void setDprt(boolean dprt) {
		this.dprt = dprt;
	}

	/** Set true if we only want to glance at target. */
	// public void setGlance(boolean glance) {
	// this.glance = glance;
	// }

	/**
	 * @param durationLimit
	 *            the durationLimit to set
	 */
	// /public void setDurationLimit(boolean durationLimit) {
	// this.durationLimit = durationLimit;
	// /}

	/**
	 * @param exposureDuration
	 *            the exposureDuration to set
	 */
	// public void setExposureDuration(double exposureDuration) {
	// this.exposureDuration = exposureDuration;
	// }

	/**
	 * Overridden. When a MULTRUN_ACK is received the time is recorded. Later
	 * when the MULTRUN_DP_ACK is received the seeing data is paired up with the
	 * exposure-time and an update sent to CCDSeeing to allow the atmospheric
	 * seeing and other data to be upgraded.arg0
	 */
	@Override
	public void handleAck(ACK ack) {
		super.handleAck(ack);

		logger.log(3, CLASS, name, "handleAck", "Ack timeToComplete: " + ack.getTimeToComplete() + " ms");

		if (ack instanceof MULTRUN_DP_ACK) {
			// This is where we push data to the SeeingSensor.
			// and to the Planetarium over the net.
			MULTRUN_DP_ACK mack = (MULTRUN_DP_ACK) ack;

			String fileName = mack.getFilename();
			if (fileName == null)
				fileName = "NO_DP_FILENAME_AVAILABLE";

			long time = exposureTimes[countReducedExposures];

			logger.log(3, CLASS, name, "handleAck", "Multrun DP(RT) completed:" + "\n Sample time   "
					+ sdf.format(new Date(time)) + "\n File:         " + fileName + "\n Counts:       "
					+ mack.getCounts() + "\n Seeing:       " + mack.getSeeing() + " (arcsec)" + "\n Bright X-pix: "
					+ mack.getXpix() + "\n Bright Y-pix: " + mack.getYpix() + "\n Photometric:  "
					+ mack.getPhotometricity() + " (mags extinction)" + "\n Sky Bright:   " + mack.getSkyBrightness()
					+ " (mag/arcsec)" + "\n Saturation:   " + (mack.getSaturation() ? "SATURATED" : "OK"));

			manager.sigMessage(this, EXPOSURE_ELEMENT, payload.getMountPointForInstrument(instId) + "/"
					+ (new File(fileName)).getName());
			manager.sigMessage(this, EXPOSURE_COUNTS, "" + mack.getCounts());
			manager.sigMessage(this, EXPOSURE_SEEING, "" + mack.getSeeing());
			manager.sigMessage(this, EXPOSURE_X, "" + mack.getXpix());
			manager.sigMessage(this, EXPOSURE_Y, "" + mack.getYpix());
			// seeing[countReducedExposures] = mack.getSeeing(); /// ?? ARCSECS
			// ??

			// NOTE: We need a way to decide if the data is allowed to go into
			// the seeing monitor - some instruments should not be allowed
			// to contribute as they have 'bizarre' concepts of seeing.

			double seeing = mack.getSeeing();

			// add the corrections for lambda and z
			// double lambda = instrument.getCurrentWavelength();

			double lambda = 0.0;
			try {
				InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
				instConfig = isp.getCurrentConfig();
				lambda = icap.getWavelength(instConfig).getValueNm();
			} catch (Exception e) {
				System.err.println("ExposureTask:: Unable to determine wavelength: "+e);
			}

			// - use the info for the time the exposure occurred.
			double elev = storeAlt[countReducedExposures];
			double az = storeAz[countReducedExposures];

			// new model
			double cosz = Math.cos(0.5 * Math.PI - elev);
			lambda = (lambda < 750.0 ? lambda : 750.0); // truncate wavelength
			// value
			double correction = Math.pow(lambda / 700.0, 0.45) * Math.pow(cosz, 0.5);
			double corrsee = seeing * correction;

			if (icap.isSkyModelProvider()) {
				// only update skymodel if this instrument is a valid
				// SkyModelProvider, otherwise skip the CCDSeeing update.

				logger.log(2, CLASS, name, "handleAck", "NOtify sky model seeing update: using wavelength: "+lambda);
				
				MutableSkyModel skyModel = (MutableSkyModel) RCS_Controller.controller.getSkyModel();
				try {
					skyModel.updateSeeing(seeing, lambda, elev, az, time, exposure.isStandard(), instId.getInstrumentName(), targetName);
								
				// TODO add the filename on the end of the param list
				/*	skyModel.updateSeeing(
							seeing, 
							lambda, 
							elev, 
							az, 
							time,
							exposure.isStandard(),
							instId.getInstrumentName(), 
							targetName,
							fileName);*/
					
				
				} catch (Exception e) {
					e.printStackTrace();
				}

			} // check for SkyModelProvider

			countReducedExposures++;

			// Telemetry.
			ReductionInfo info = new ReductionInfo(System.currentTimeMillis());
			// String obsPathName = observation.getFullPath();
			info.setObsPathName(obsPathName);
			info.setFileName(payload.getMountPointForInstrument(instId) + "/" + (new File(fileName)).getName());
			info.setCountReducedExposures(countReducedExposures);
			info.setSeeing(mack.getSeeing());
			info.setPhotometricity(mack.getPhotometricity());
			info.setSkyBrightness(mack.getSkyBrightness());
			info.setCounts((int) mack.getCounts());
			info.setXpix((int) mack.getXpix());
			info.setYpix((int) mack.getYpix());

			Telemetry.getInstance().publish("DPRT", info);

			manager.sigMessage(this, EXPOSURE_DATA, info);

		} else if (ack instanceof MULTRUN_ACK) {
			MULTRUN_ACK mack = (MULTRUN_ACK) ack;

			String fileName = mack.getFilename();
			if (fileName == null)
				fileName = "NO_FILENAME_AVAILABLE";

			logger.log(2, CLASS, name, "handleAck", "Multrun Exposure completed:" + " File: " + fileName);

			exposureTimes[countCompletedExposures] = System.currentTimeMillis();
			// alt and az at that time also...
			double alt = Math.toRadians(StatusPool.latest().mechanisms.altPos);
			double az  = Math.toRadians(StatusPool.latest().mechanisms.azPos);
			storeAlt[countCompletedExposures] = alt;
			storeAz[countCompletedExposures] = az;

			countCompletedExposures++;
			// NOTE: We are setting this to the correct mount-relative location
			// here!
			if (manager != null) {
				manager.sigMessage(this, EXPOSURE_FILE, payload.getMountPointForInstrument(instId) + "/"
						+ (new File(fileName)).getName());
			}

			// Telemetry.
			ExposureInfo info = new ExposureInfo(System.currentTimeMillis());
			// String obsPathName = observation.getFullPath();
			info.setObsPathName(obsPathName);
			info.setFileName(payload.getMountPointForInstrument(instId) + "/" + (new File(fileName)).getName());
			info.setCountCompletedExposures(countCompletedExposures);

			Telemetry.getInstance().publish("EXP", info);

			manager.sigMessage(this, EXPOSURE_DATA, info);

			// logger.log(1, CLASS, name, "onDisposal",
			// "Exposure Tracking summary: Integration: "+tm.getIntegrationTime()+
			// " ms, RMS(asec)/Max(asec)/Std(asec?):"+
			// "   Az: "+tm.getAzRms()+
			// "/"+(tm.getAzMax()*3600.0)+"/"+(tm.getAzStd()*3600.0)+
			// ", Alt: "+tm.getAltRms()+"/"+(tm.getAltMax()*3600.0)+"/"+(tm.getAltStd()*3600.0)+
			// ", Rot: "+tm.getRotRms()+"/"+(tm.getRotMax()*3600.0)+"/"+(tm.getRotStd()*3600.0));
			// tm.reset();
			// am.reset();

		} else {
			logger.log(4, CLASS, name, "handleAck", ack.getClass().getName() + " received:");
		}
	}

	/** This task can NOT be aborted when it is running - let it fail. */
	@Override
	public boolean canAbort() {
		return false;
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();

		long expectedTotalExposure = 0L;
		
		// determine how the instrument is configured
		try {
			InstrumentStatusProvider isp = ireg.getStatusProvider(instId);
			instConfig = isp.getCurrentConfig();
		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
			errorIndicator = new BasicErrorIndicator(640107, "Unable to determine current instrument configuration", e);
			return;
		}

		if (exposure instanceof XPeriodRunAtExposure) {

			XPeriodRunAtExposure xperat = (XPeriodRunAtExposure) exposure;

			TIMED_MULTRUNAT timedmultrunat = new TIMED_MULTRUNAT(name);
			timedmultrunat.setTotalDuration((int) xperat.getTotalExposureDuration());
			timedmultrunat.setExposureTime((int) xperat.getExposureLength());
			timedmultrunat.setPipelineProcess(true);
			timedmultrunat.setStartTime(new Date(xperat.getRunAtTime()));
			timedmultrunat.setStandard(exposure.isStandard());
			exposureTotal = (long) xperat.getTotalExposureDuration();
			command = timedmultrunat;
			
		} else if (exposure instanceof XMultipleExposure) {

			XMultipleExposure xmult = (XMultipleExposure) exposure;

			if (instConfig instanceof XDualBeamSpectrographInstrumentConfig) {
				XDualBeamSpectrographInstrumentConfig frodoConfig = (XDualBeamSpectrographInstrumentConfig) instConfig;
				FRODOSPEC_MULTRUN frodomultrun = new FRODOSPEC_MULTRUN(name);
				try {
					FrodoSpecConfig frodo = (FrodoSpecConfig) ConfigTranslator.translateToOldStyleConfig(instConfig);
					
					String instName = collator.getLastConfig().getInstrumentName();
					if (instName.equals("FRODO_RED"))
					    frodomultrun.setArm(FrodoSpecConfig.RED_ARM);
					else if (instName.equals("FRODO_BLUE"))
                                            frodomultrun.setArm(FrodoSpecConfig.BLUE_ARM);
					
				} catch (Exception e) {
					e.printStackTrace();
					failed = true;
					errorIndicator = new BasicErrorIndicator(640108, "Unable to determine FRODO arm from config", e);
					return;
				}
				frodomultrun.setNumberExposures(xmult.getRepeatCount());
				frodomultrun.setExposureTime((int) xmult.getExposureTime());
				frodomultrun.setPipelineProcess(true);
				frodomultrun.setStandard(xmult.isStandard());
				command = frodomultrun;
			
			} else {

				MULTRUN multrun = new MULTRUN(name);
				multrun.setNumberExposures(xmult.getRepeatCount());
				multrun.setExposureTime((int) xmult.getExposureTime());
				multrun.setPipelineProcess(true);
				multrun.setStandard(xmult.isStandard());			
				command = multrun;
				
			}
			exposureTotal = (long) xmult.getExposureTime();
		} else if (exposure instanceof XPeriodExposure) {

			XPeriodExposure xper = (XPeriodExposure) exposure;

			MULTRUN multrun = new MULTRUN(name);
			multrun.setNumberExposures(0);
			multrun.setExposureTime((int) xper.getExposureTime());
		
			multrun.setPipelineProcess(true);
			
			multrun.setStandard(xper.isStandard());
			command = multrun;
			exposureTotal = (long) xper.getExposureTime();

		}

		// Determine the target name
		if (collator != null) {
			
			ITarget target = collator.getLastTarget();
			if (target != null)
				targetName = target.getName();
			else 
				targetName = "UNKNOWN";
		}
		
		// TODO Does this just override all the previous settings ?
		((EXPOSE) command).setPipelineProcess(dprt);

		max_seeing = config.getDoubleValue("max.usable.seeing", DEFAULT_MAX_SEEING);

		logger.log(1, CLASS, name, "onInit", "Starting "
				+ (exposure.isStandard() ? "photometric standards" : "observational") + " exposure sequence: "
				+ numExposures + " exposures inc. offsets, "+(dprt ? "using dprt":"no dprt"));

		// Set the seeing estimate for start of observation - uncorrected !
		// double lambda = instrument.getCurrentWavelength();
		logger.log(1, CLASS, name, "onInit", "Determine wavelength using config: " + instConfig);
		Wavelength w = icap.getWavelength(instConfig);
		logger.log(1, CLASS, name, "onInit", "Found wavelength: " + w);
		double lambda = w.getValueNm();
		double elev = Math.toRadians(StatusPool.latest().mechanisms.altPos);
		double zz = 0.5 * Math.PI - elev;
		double cosz = Math.cos(zz);

		// new model
		lambda = (lambda < 750.0 ? lambda : 750.0);
		double correction = Math.pow(lambda / 700.0, 0.45) * Math.pow(cosz, 0.5);

		double corrSeeing = 0.0;
		double uncorrSeeing = 0.0;
		boolean valid = true;
		try {
			corrSeeing = RCS_Controller.controller.getSkyModel().getSeeing(700.0, 0.5 * Math.PI, Math.PI,
					System.currentTimeMillis());
			uncorrSeeing = corrSeeing / correction;
			if (Double.isNaN(uncorrSeeing) || Double.isInfinite(uncorrSeeing))
				valid = false;
		} catch (Exception e) {
			valid = false;
		}
		
		logger.log(1, CLASS, name, "onInit", "De-correct seeing estimate using instrument wavelength= " + lambda
				+ "nm, at Z= " + Position.toDegrees(zz, 3) + " degs, Valid= " + valid);

		if (valid)
			FITS_HeaderInfo.current_ESTSEE.setValue(new Double(uncorrSeeing));
		else
			FITS_HeaderInfo.current_ESTSEE.setValue(new Double(99.9));

		// Sky brightness
		double azm = Math.toRadians(StatusPool.latest().mechanisms.azPos);
		
		try {
		    ISite site = RCS_Controller.controller.getSite(); 
		    SkyBrightnessCalculator skycalc = new SkyBrightnessCalculator(site);
		    int skycat = skycalc.getSkyBrightnessCriterion(elev, azm, System.currentTimeMillis());
		    double skyb = SkyBrightnessCalculator.getSkyBrightness(skycat);		
		    
		    if (skyb < 50.0)
			FITS_HeaderInfo.current_SCHEDSKY.setValue(new Double(skyb));
		    else
			FITS_HeaderInfo.current_SCHEDSKY.setValue(new Double(99.99));
		} catch (Exception e) {
		    FITS_HeaderInfo.current_SCHEDSKY.setValue(new Double(-99.99));
		}

		// switch on stability monitoring - check if we need the AG?
		// NOTE The AG flag is ignored by TSM at the moment...
		// boolean needAutoguider = (observation.getAutoGuiderUsageMode() ==
		// TelescopeConfig.AGMODE_MANDATORY);
		// TODOtsm.startStabilityMonitoring(needAutoguider);

		// enable tracking and guide monitors if applicable...
		logger.log(1, CLASS, name, "onInit", "Enabling Tracking-monitor");
		tm.reset();

		// TODO IMPORTANT SOON
		/*
		 * if (observation.getAutoGuiderUsageMode() ==
		 * TelescopeConfig.AGMODE_MANDATORY) { // &&
		 * observation.getNumRuns()*observation.getExposeTime() < //
		 * DEFAULT_MIN_OBS_GUIDE_TIME) {
		 * 
		 * // if we lose for 0.5 of the average exposure time then we reset ?
		 * double maxGuideLostTime = Math.max(DEFAULT_AG_LOST_TIME, 0.5 *
		 * (double) observation.getExposeTime());
		 * am.setMaxGuidingLostTime((long) maxGuideLostTime); am.reset();
		 * 
		 * logger.log(1, CLASS, name, "onInit",
		 * "Enabling Autoguider-monitor for mandatory guided observation using max guide lost time: "
		 * + maxGuideLostTime + "ms");
		 * 
		 * }
		 */
		// TODO switch on AGMON if we are expecting agmandatory
		if (collator != null) {
			IAutoguiderConfig autoConfig = collator.getAutoguide();
			if (autoConfig.getAutoguiderCommand() == IAutoguiderConfig.ON) {
				long maxGuideLostTime =  (long)(0.05*exposureTotal);
				am.setMaxGuidingLostTime(maxGuideLostTime);
				am.setEnableAlerts(true);
				logger.log(1, CLASS, name, "onInit",
						  "Enabling Autoguider-monitor for mandatory guided observation using max guide lost time: "
						  + (maxGuideLostTime/1000) + "s");
				tsm.startStabilityMonitoring(true);
				logger.log(1, CLASS, name, "onInit",
						  "Enabling stability-monitor for mandatory guided observation using max guide lost time: "
						  + (maxGuideLostTime/1000) + "s");
			}
		}
		
		// register for IssAutoguider offsets here.
		IssAutoguiderAdjustmentController agcon = ISS.getInstance().getAdjustmentController();
		agcon.addAutoguiderAdjustmentListener(this);

	}

	/**
	 * Carry out subclass specific completion work. Pass the filename up the
	 * task hierarchy. Update the Seeing sensor readings.
	 */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		// The MULTRUN_DONE (or otherwise) contains the last image filename.
		// via. MULTRUN_DONE.getFilename(). This is not terribly important
		// most of the time BUT in Planetarium mode the top level caller
		// e.g. POS_CCDOBSERVE_Task will want this info so it can retreive
		// the image via the TIT-Server on the ICS. We use the sigMessage()
		// method to propagate up the management hierarchy and hope it
		// will be of use to some manager or other.
		if (response instanceof MULTRUN_DONE) {
			if (manager != null) {
				manager.sigMessage(this, EXPOSURE_COMPLETE, ((MULTRUN_DONE) response).getFilename());
			}
		}

		logger.log(1, CLASS, name, "onCompletion", "Completed exposure sequence: Total " + countCompletedExposures
				+ " exposures, reduced " + countReducedExposures);

		// Completed exposure sequence: Total 15 exposures, reduced 12.

	}

	@Override
	protected void logExecutionStatistics() {
		// int bx = observation.getInstrumentConfig().getDetector(0).getXBin();
		// int by = observation.getInstrumentConfig().getDetector(0).getYBin();

		int bx = instConfig.getDetectorConfig().getXBin();
		int by = instConfig.getDetectorConfig().getYBin();

		// TODO needs special handling for diff types of
		// exposures...per/mult/trig etc

		logger.log(3, CLASS, name, "onCompletion", "EXEC_TIME for " + CLASS + " : " + instConfig.getInstrumentName()
				+ " " + countCompletedExposures + " " + bx + "x" + by + " " + exposureTotal + " : "
				+ (System.currentTimeMillis() - startTime));
	}

	@Override
	protected void onDisposal() {
		super.onDisposal();

		// switch off stability monitoring
		tsm.stopStabilityMonitoring();

		// old stability mon - switch off alerts to OST
		tm.setEnableAlerts(false);
		am.setEnableAlerts(false);

		IssAutoguiderAdjustmentController agcon = ISS.getInstance().getAdjustmentController();
		agcon.removeAutoguiderAdjustmentListener(this);

		// these may not make sense as the resets are done only once per multrun
		// NOT per exposure as before
		logger.log(1, CLASS, name, "onDisposal", "Exposure Tracking summary (full multrun): Integration: "
				+ tm.getIntegrationTime() + " ms, RMS(asec)/Max(asec):" + "   Az: " + tm.getAzRms() + "/"
				+ (tm.getAzMax() * 3600.0) + ", Alt: " + tm.getAltRms() + "/" + (tm.getAltMax() * 3600.0) + ", Rot: "
				+ tm.getRotRms() + "/" + (tm.getRotMax() * 3600.0));
	}

	/** Returns a reference to the Instrument in use. */
	// public Instrument getInstrument() {
	// return instrument;
	// }

	public void startingOffset() {
		logger.log(1, CLASS, name, "ExposureTask: " + name + " - [START_OFFSET] Iss is starting an offset");
	}

	public void endingOffset() {
		logger.log(1, CLASS, name, "ExposureTask: " + name + " - [DONE_OFFSET] Iss has completed an offset");
	}

	public void guideReAcquired() {
		logger.log(1, CLASS, name, "ExposureTask: " + name + " - [AG_LOCK_REACQUIRED] Iss has reacquired guide lock");
	}

	public void guideNotReAcquired(String message) {
		logger.log(1, CLASS, name, "ExposureTask: " + name
				+ " - [AG_NOT_REACQUIRED] Iss has failed to reacquired guide lock: " + message);
	}

}

/**
 * $Log: Exposure_Task.java,v $ /** Revision 1.6 2007/09/27 08:23:19 snf /**
 * added notes about use of obs.pipelineconfig /** /** Revision 1.5 2007/09/11
 * 11:32:42 snf /** added ag adjustment listener methods /** /** Revision 1.4
 * 2007/08/10 21:30:53 snf /** added skymodel stuff /** /** Revision 1.3
 * 2007/07/09 10:00:11 snf /** added test for SkyModelProvider before updating
 * seeing estimate to SkyModel (CCDSeeing) /** /** Revision 1.2 2007/03/07
 * 11:17:53 snf /** added check for SMP - commented out for now as insts are not
 * set up to reveal this info yet. /** /** Revision 1.1 2006/12/12 08:28:27 snf
 * /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:16 snf /** Initial
 * revision /** /** Revision 1.4 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.3 2001/06/08 16:27:27 snf /** Added telfocus
 * trapping info. /** /** Revision 1.2 2001/04/27 17:14:32 snf /** backup /**
 * /** Revision 1.1 2001/02/16 17:44:27 snf /** Initial revision /**
 */

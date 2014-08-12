/**
 * 
 */
package ngat.rcs.sciops;

import java.util.List;

import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicNonSiderealTrackingCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.NonSiderealTrackingCalculator;
import ngat.astrometry.Position;
import ngat.astrometry.TargetTrackCalculator;
import ngat.astrometry.TrackingRates;
import ngat.icm.DetectorArrayPosition;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.ITarget;
import ngat.phase2.XEphemerisTarget;
import ngat.phase2.TelescopeConfig;
import ngat.phase2.XPositionOffset;
import ngat.rcs.RCS_Controller;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.iss.ISS;
import ngat.rcs.scm.collation.StatusPool;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.ApertureOffsetTask;
import ngat.rcs.tms.executive.InstrumentAcquireTask;
import ngat.rcs.tms.executive.TweakTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.tcm.BasicTelescopeAlignmentAdjuster;
import ngat.tcm.BasicTelescopeSystem;

/**
 * @author eng
 * 
 */
public class AcquisitionTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, ACQ = 100

	/**
	 * Tracking rate for non-sidereal targets which will need to be treated as
	 * moving acquisition (rad/sec). // using 10 as/s = 30 binned pix per sec 
	 */
        private static final double MOVING_TARGET_RATE = Math.toRadians((10.0/3600.0));

        // here we work out 5as in 3 minutes as half the bundle width 
        // in the time taken to do an acquire or 0.025 as/s
        // private static final double MOVING_TARGET_RATE = Math.toRadians((5.0/(3600.0*200.0));
        
	/** The acquisition config. */
	private IAcquisitionConfig acquisitionConfig;

	/**
	 * The instrument we will actually use for acquisition - may not be the one
	 * requested.
	 */
	private String useAcquireInstrument;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/** Acquisition X offset (pix). */
	private int offsetX;

	/** Acquisition Y offset (pix). */
	private int offsetY;

	/** Tweak X offset (asec). */
	private double tweakOffsetX;

	/** Tweak Y offset (asec). */
	private double tweakOffsetY;

	/** Description of this task for logging entry. */
	private String taskOperationName;

    /** Actual threshold for moving target acquisition.*/
    private double movingTargetThreshold = MOVING_TARGET_RATE;

	// private SciencePayload payload;

	// private InstrumentRegistry ireg;

	/**
	 * @param name
	 * @param manager
	 */
	public AcquisitionTask(String name, TaskManager manager, IAcquisitionConfig acquisitionConfig,
			ChangeTracker collator) {
		super(name, manager);
		this.acquisitionConfig = acquisitionConfig;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {

		// locate the target instrument

		String targetInstrumentName = acquisitionConfig.getTargetInstrumentName();
		if (targetInstrumentName == null || targetInstrumentName.equals("")) {
			failed(660101, "No target instrument specified for acquisition");
			return null;
		}

		InstrumentDescriptor tid = null;
		try {
			tid = ireg.getDescriptor(targetInstrumentName);
		} catch (Exception e) {
			failed(660102, "Failed to locate descriptor for target instrument (" + targetInstrumentName
					+ ") for acquisition");
			return null;
		}

		InstrumentStatusProvider tsp = null;
		try {
			tsp = ireg.getStatusProvider(tid);
			if (tsp == null) {
				failed(660103, "Unknown target instrument (" + targetInstrumentName + ") for acquisition");
				return null;
			}
		} catch (Exception e) {
			failed(660104, "Unknown target instrument (" + targetInstrumentName + ") for acquisition");
			return null;
		}

		// TODO InstrumentStatus tistat = tsp.getStatus();

		// acquire mode
		int useAcquireMode = TelescopeConfig.ACQUIRE_MODE_NONE;

		int acqMode = acquisitionConfig.getMode();

		// this is the one the user asked for in the first place
		String primaryAcquisitionInstrument = acquisitionConfig.getAcquisitionInstrumentName();

		// this is the one we are using - may be same
		InstrumentDescriptor aid = null;
		try {
			aid = ireg.getDescriptor(useAcquireInstrument);
		} catch (Exception e) {
			failed(660106, "Failed to locate descriptor for actual acquisition instrument (" + useAcquireInstrument
					+ ") for acquisition");
			return null;
		}

		switch (acqMode) {
		case IAcquisitionConfig.INSTRUMENT_CHANGE:

			int apNumber = payload.getApertureNumberForInstrument(tid);

			ApertureOffsetTask apertureTask = new ApertureOffsetTask(name + "/AP_" + targetInstrumentName + "("
					+ apNumber + ")", this, apNumber);
			taskList.addTask(apertureTask);

			// convert the offsets to rads first
			TweakTask tweakTask = new TweakTask(name + "/TWEAK(" + tweakOffsetX + "," + tweakOffsetY + ")", this,
					Math.toRadians(tweakOffsetX / 3600.0), Math.toRadians(tweakOffsetY / 3600.0));

			// they will be converted back to arcsecs by the command sender !
			taskList.addTask(tweakTask);

			try {
				taskList.sequence(apertureTask, tweakTask);
			} catch (TaskSequenceException tse) {
				failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error: " + tse);
				return null;
			}

			break;

		case IAcquisitionConfig.WCS_FIT:
		case IAcquisitionConfig.BRIGHTEST:

			if (acqMode == IAcquisitionConfig.WCS_FIT)
				useAcquireMode = TelescopeConfig.ACQUIRE_MODE_WCS;
			else
				useAcquireMode = TelescopeConfig.ACQUIRE_MODE_BRIGHTEST;

			// locate the actual acquisition instrument

			if (useAcquireInstrument == null || useAcquireInstrument.equals("")) {
				failed(660105, "No acquire instrument specified for acquisition");
				return null;
			}

			InstrumentStatusProvider asp = null;
			try {
				asp = ireg.getStatusProvider(aid);
				if (asp == null) {
					failed(660107, "Unknown acquisition instrument (" + useAcquireInstrument + ") for acquisition");
					return null;
				}
			} catch (Exception e) {
				failed(660108, "Unknown acquisition instrument (" + useAcquireInstrument + ") for acquisition");
				return null;
			}

			try {
				// TODO break this down into bits...
				InstrumentCapabilities acap = ireg.getCapabilitiesProvider(aid).getCapabilities();
				taskLog.log(1, "Located instrument caps for acquisition instrument: " + aid.getInstrumentName());
				DetectorArrayPosition dap = acap.getAcquisitionTargetPosition(tid);
				taskLog.log(1, "Located DAP for target instrument: " + tid.getInstrumentName());

				offsetX = (int) dap.getDetectorArrayPositionX();
				offsetY = (int) dap.getDetectorArrayPositionY();
			} catch (Exception e) {
				failed(660109, "Unable to obtain acquisition offsets for: " + targetInstrumentName
						+ ", using actual acquire instrument: " + useAcquireInstrument);
				e.printStackTrace();
				return null;
			}
			// locate the target, only if we are acquiring...
			ITarget target = collator.getLastTarget();

			double ra = 0.0;
			double dec = 0.0;
			try {
				long now = System.currentTimeMillis();
				AstrometrySiteCalculator astro = RCS_Controller.controller.getSiteCalculator();
				ISite site = RCS_Controller.controller.getSite();
				TargetTrackCalculator track = new BasicTargetCalculator(target, site);
				Coordinates c = track.getCoordinates(now);
				ra = c.getRa();
				dec = c.getDec();

			} catch (Exception e) {
				failed(660110, "Unable to determine target coordinates: " + e);
				return null;
			}

			// TODO MOVING TARGET Work out the tracking rate for NS targets

			double raRate = 0.0;
			double decRate = 0.0;
			long rateTime = System.currentTimeMillis();
			boolean moving = false;

			// decide if we even need to do this calculation
			if (target instanceof XEphemerisTarget) {
				taskLog.log(1, "Ephemeris target, determine tracking rates...");

				try {
					ISite site = RCS_Controller.controller.getSite();
					TargetTrackCalculator track = new BasicTargetCalculator(target, site);
					NonSiderealTrackingCalculator nsc = new BasicNonSiderealTrackingCalculator();
					TrackingRates rates = nsc.getNonSiderealTrackingRates(track, rateTime);
					raRate = rates.getNsTrackingRateRA();
					decRate = rates.getNsTrackingRateDec();
					taskLog.log(1,
						    "Tracking rates: RA: " + Math.toDegrees(raRate) * 3600.0 + " a/s, Dec"
						    + Math.toDegrees(decRate) * 3600.0 + " a/s");
					
					movingTargetThreshold = MOVING_TARGET_RATE;
					try {
					    String strMtr = System.getProperty("moving.target.threshold.rate", "10.0"); // in as/sec  
					    double mtrasec = Double.parseDouble(strMtr);
					    movingTargetThreshold = Math.toRadians(mtrasec/3600.0);
					} catch (Exception e) {
					    e.printStackTrace();
					    taskLog.log(1, "Unable to determine moving target threshold from sysconfig, using default");
					}

					taskLog.log(1, "Moving target threshold rate: "+Math.toDegrees(movingTargetThreshold)*3600.0+" a/s");

					if (raRate > movingTargetThreshold || decRate > movingTargetThreshold) {
						taskLog.log(1, "Target is moving fast, use moving-acquire");
						moving = true;
					}
				} catch (Exception e) {
					failed(660116, "Unable to determine target tracking rate: " + e);
					return null;
				}
			}

			// acqInstName here is useAcqInstName which may not be what we asked
			// for
			InstrumentAcquireTask acquireTask = new InstrumentAcquireTask(name + "/ACQ(" + useAcquireInstrument + "->"
					+ targetInstrumentName + ")", this, useAcquireInstrument, ra, dec, moving, raRate, decRate,
					rateTime, offsetX, offsetY, useAcquireMode);

			// ACQ(RATCAM->FRODO)
			taskList.addTask(acquireTask);

			if (!(useAcquireInstrument.equals(primaryAcquisitionInstrument))) {

				// not using primary inst - need to re-aperture offset
				ApertureOffsetTask aperture2Task = null;

				taskLog.log(1, "Setting up aperture for secondary acquisition instrument");
				int ap2Number = payload.getApertureNumberForInstrument(aid);
				aperture2Task = new ApertureOffsetTask(name + "/AP2_" + primaryAcquisitionInstrument + "(" + ap2Number
						+ ")", this, ap2Number);

				taskList.addTask(aperture2Task);

				// convert the offsets to rads first
				TweakTask tweak2Task = new TweakTask(name + "/TWEAK(" + tweakOffsetX + "," + tweakOffsetY + ")", this,
						Math.toRadians(tweakOffsetX / 3600.0), Math.toRadians(tweakOffsetY / 3600.0));

				// they will be converted back to arcsecs by the command sender
				// !
				taskList.addTask(tweak2Task);

				try {
					taskList.sequence(aperture2Task, tweak2Task);
					taskList.sequence(tweak2Task, acquireTask);
				} catch (TaskSequenceException tse) {
					failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error", tse);
					return null;
				}
			}

			break;
		}

		return taskList;

	}

	@Override
	public void preInit() {
		super.preInit();
		String acqInstName = acquisitionConfig.getAcquisitionInstrumentName();
		String targetInstName = acquisitionConfig.getTargetInstrumentName();
		String acqModeStr = null;

		InstrumentCapabilities acap = null;
		BasicTelescopeSystem bts = null;
		BasicTelescopeAlignmentAdjuster bta;
		double rotatorPosition = 0.0;
		XPositionOffset offset = null;
		
		int mode = acquisitionConfig.getMode();
		switch (mode) {
		case IAcquisitionConfig.INSTRUMENT_CHANGE:
			taskLog.log(1, "Starting focal plane offset, applying offsets for: " + targetInstName);
			taskOperationName = "Focal plane offset for: " + targetInstName;

			// obtain the current rotator position and add a tweak here
			rotatorPosition = StatusPool.latest().mechanisms.rotPos;

		
			try {
				bts = (BasicTelescopeSystem) RCS_Controller.controller.getTelescope().getTelescopeSystem();
			} catch (Exception e) {
				failed = true;

				failed(660113, "Unable to locate telescope system", e);
				return;
			}

			try {
				bta = bts.getAdjuster();
			} catch (Exception e) {
				failed(660114, "Unable to locate telescope alignment adjuster", e);
				return;
			}

			try {
				// rotator is in degrees
				offset = bta.interpolate(rotatorPosition);
				// these are in arcsecs
				tweakOffsetX = offset.getRAOffset();
				tweakOffsetY = offset.getDecOffset();
				taskLog.log(1, CLASS, name, "onInit", "Tweak offsets for rotator: " + rotatorPosition + "degs are: x: "
						+ tweakOffsetX + " asec, y: " + tweakOffsetY + " asec");
				// these are still in arcsecs here
			} catch (Exception e) {
				failed(660115, "Unable to determine valid alignment solution", e);
				return;
			}

			break;
		case IAcquisitionConfig.WCS_FIT:
		case IAcquisitionConfig.BRIGHTEST:

			if (mode == IAcquisitionConfig.WCS_FIT) {
				acqModeStr = "WCS_FIT";
				taskOperationName = "Wcsfit acquisition";
			} else {
				acqModeStr = "BRIGHTEST";
				taskOperationName = "Brightest target acquisition";
			}

			taskLog.log(1, "Starting fine-tune acquisition for " + targetInstName + ", using " + acqInstName
					+ " via mode:" + acqModeStr);

			InstrumentDescriptor aid = null;
			InstrumentStatusProvider asp = null;
			InstrumentCapabilitiesProvider acp = null;

			try {
				aid = ireg.getDescriptor(acqInstName);
				asp = ireg.getStatusProvider(aid);
				acp = ireg.getCapabilitiesProvider(aid);
				acap = acp.getCapabilities();
				if (asp == null) {
					failed(660111, "Unknown acquisition instrument (" + acqInstName + ") for acquisition");
					return;
				}
			} catch (Exception e) {
				failed(660112, "Unknown acquisition instrument (" + acqInstName + ") for acquisition");
				return;
			}

			// TODO if the ACQ inst is stuffed and we allow altenative, test
			// these in order and
			// and set actualAcquisitionIntrument to that
			// ETCETCif (!asp.getStatus().isEnabled() &&
			// asp.getStatus().isFunctional() && online etc

			taskLog.log(1, "Checking status of primary acquisition instrument: " + acqInstName);
			InstrumentStatus astat = null;
			boolean primaryAcqInstOk = true;
			boolean altAcqInstOk = true;
			String altInstName = null;
			try {
				astat = asp.getStatus();
				if (!astat.isEnabled()) {
					primaryAcqInstOk = false;
					taskLog.log(1, "Primary acqusition instrument: " + acqInstName + " is disabled");
				}
				if (!astat.isFunctional()) {
					primaryAcqInstOk = false;
					taskLog.log(1, "Primary acqusition instrument: " + acqInstName + " is not functional");
				}
				if (!astat.isOnline()) {
					primaryAcqInstOk = false;
					taskLog.log(1, "Primary acqusition instrument: " + acqInstName + " is offline");
				}
			} catch (Exception e) {
				failed(660117, "Unable to determine status of primary acqusition instrument: " + acqInstName);
				return;
			}

			if (primaryAcqInstOk) {
				useAcquireInstrument = acqInstName;
				taskLog.log(1, "Using requested acquisition instrument: " + acqInstName);
			} else {
				// we cant use the primary instrument
				if (acquisitionConfig.getAllowAlternative()) {

					// we can try an alternative
					InstrumentDescriptor qid = null;
					InstrumentStatusProvider qsp = null;

					List<InstrumentDescriptor> alist = null;
					try {
						alist = ireg.listAcquisitionInstruments();
					} catch (Exception e) {
						failed(660120, "Unable to obtain list of alternative acquisition instruments");
						return;
					}

					// TODO not very efficient we should drop out as soon as we find a 
					// (the highest priority) alternative acq instrument.
					
					for (int ia = 0; ia < alist.size(); ia++) {
						qid = alist.get(ia);
						altInstName = qid.getInstrumentName();
						taskLog.log(1, "Checking status of alternative acquisition instrument [" + ia + "] : "
								+ altInstName);
						try {
							qsp = ireg.getStatusProvider(qid);
							InstrumentCapabilitiesProvider qcp = ireg.getCapabilitiesProvider(qid);
							acap = qcp.getCapabilities();

						} catch (Exception e) {
							failed(660120, "Unable to determine status of primary acqusition instrument: "
									+ acqInstName);
							return;
						}

						altAcqInstOk = true;
						InstrumentStatus qstat = null;
						try {
							qstat = qsp.getStatus();
							if (!qstat.isEnabled()) {
								altAcqInstOk = false;
								taskLog.log(1, "Alternative acqusition instrument: " + altInstName + " is disabled");
							}
							if (!qstat.isFunctional()) {
								altAcqInstOk = false;
								taskLog.log(1, "Alternative acqusition instrument: " + altInstName
										+ " is not functional");
							}
							if (!qstat.isOnline()) {
								altAcqInstOk = false;
								taskLog.log(1, "Alternative acqusition instrument: " + altInstName + " is offline");
							}
						} catch (Exception e) {
							taskLog.log(1, "Unable to determine status of alternative acqusition instrument: "
									+ altInstName);
							// never mind there may be others to try...
						}					

					}
					
					if (!altAcqInstOk) {
						
						failed(660121, "No alternative acquisition instruments were available");
						return;
						
					} else {
				
						useAcquireInstrument = altInstName;
						taskLog.log(1, "Using alternative acquisition instrument: " + altInstName + " instead of: "
								+ acqInstName);
						
						// now we need to calculate tweaks as we will need to re-aperture offset
						
						taskLog.log(1, "Computing tweak to follow re-aperture offset");
					
						// obtain the current rotator position and add a tweak here
						rotatorPosition = StatusPool.latest().mechanisms.rotPos;


						try {
							bts = (BasicTelescopeSystem) RCS_Controller.controller.getTelescope().getTelescopeSystem();
						} catch (Exception e) {
							failed = true;

							failed(660113, "Unable to locate telescope system", e);
							return;
						}

						try {
							bta = bts.getAdjuster();
						} catch (Exception e) {
							failed(660114, "Unable to locate telescope alignment adjuster", e);
							return;
						}

						try {
							// rotator is in degrees
							offset = bta.interpolate(rotatorPosition);
							// these are in arcsecs
							tweakOffsetX = offset.getRAOffset();
							tweakOffsetY = offset.getDecOffset();
							taskLog.log(1, CLASS, name, "onInit", "Tweak offsets for rotator: " + rotatorPosition + "degs are: x: "
									+ tweakOffsetX + " asec, y: " + tweakOffsetY + " asec");
							// these are still in arcsecs here
						} catch (Exception e) {
							failed(660115, "Unable to determine valid alignment solution", e);
							return;
						}
						
					}
				} else {
					// we cannot use alternative
					failed(660118, "Primary acquisition instrument " + acqInstName
							+ " is not available and alternatives are not allowed");
					return;
				}

			}

			// let ISS know who might be requesting offsets
			ISS.setCurrentAcquisitionInstrumentName(useAcquireInstrument);
			taskLog.log(1, "Notified ISS of expected acquire instrument name: " + useAcquireInstrument);

			double rotcorr = acap.getRotatorOffset();
			taskLog.log(1, "Resetting instrument rotator alignment correction for: " + useAcquireInstrument + " to "
					+ Position.toDegrees(rotcorr, 2));

			FITS_HeaderInfo.setRotatorSkyCorrection(rotcorr);

			// set ACQ headers
			FITS_HeaderInfo.current_ACQINST.setValue(useAcquireInstrument);
			FITS_HeaderInfo.current_ACQIMG.setValue("NONE");
			FITS_HeaderInfo.current_ACQMODE.setValue(acqModeStr);
			FITS_HeaderInfo.current_ACQXPIX.setValue(new String("NONE"));
			FITS_HeaderInfo.current_ACQYPIX.setValue(new String("NONE"));
			break;
		}

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		String acqInstrumentName = acquisitionConfig.getAcquisitionInstrumentName();
		String targetInstrumentName = acquisitionConfig.getTargetInstrumentName();
		if (task instanceof InstrumentAcquireTask) {

			InstrumentAcquireTask iaTask = (InstrumentAcquireTask) task;
			String acfile = iaTask.getLastAcquireImageFileName();
			collator.setAcquired(true);
			collator.setAcquireImage(acfile);
			collator.setAcqConfig(acquisitionConfig);
			collator.setAcquireOffset(new DetectorArrayPosition(offsetX, offsetY));
			collator.setAcqInstrument(acqInstrumentName);

		} // else if (task instanceof ApertureOffsetTask) {
			// collator.setApertureInstrument(acqInstrumentName);
			// }
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		failed(err.getErrorCode(), taskOperationName + " failure");
	}

}

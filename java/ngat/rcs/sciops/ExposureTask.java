/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.IAutoguiderConfig;
import ngat.phase2.IExposure;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.XMultipleExposure;
import ngat.phase2.XPeriodExposure;
import ngat.phase2.XPeriodRunAtExposure;
import ngat.rcs.RCS_Controller;
import ngat.rcs.control.Telemetry;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Abort_Task;
import ngat.rcs.tms.executive.Exposure_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.sms.GroupItem;
import ngat.tcm.BasicTelescope;
import ngat.icm.DetectorArrayPosition;
import ngat.icm.InstrumentCalibration;
import ngat.icm.InstrumentCalibrationProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.message.GUI_RCS.ObservationStatusInfo;

/**
 * @author eng
 * @param <XPeriodRunatExposure>
 * 
 */
public class ExposureTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, EXPOSE = 700

	/** The exposure. */
	private IExposure exposure;

	/** The Group for which this exposure is being performed. */
	private GroupItem group;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/**
	 * @param name
	 *            The name of this task.
	 * @param manager
	 *            The task's manager.
	 * @param exposure
	 *            The exposure to perform.
	 * @param collator
	 *            Keeps track of changes.
	 */
	public ExposureTask(String name, TaskManager manager, GroupItem group, IExposure exposure, ChangeTracker collator) {
		super(name, manager);
		this.group = group;
		this.exposure = exposure;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {
		System.err.println("Create exposure tasklist: using: " + collator);
		System.err.println("Create exposure tasklist: last config was: " + collator.getLastConfig());
		System.err.println("Create exposure tasklist: last instrument name was: " + collator.getInstrumentName());

		// are we autoguiding ?
		IAutoguiderConfig autoguide = collator.getAutoguide();
		System.err.println("Create exposure tasklist: autoguider config was: " + autoguide);

		double exposureTime = 0.0;
		int repeat = 0;
		boolean standard = false;
		double exposureDuration = 0.0;
		long exposureStart = 0L;

		String expTaskName = null;
		// Observation obs = null;
		String obsid = null;

		// Instrument = collator.getLastInstrumentSelection();
		String instrumentName = collator.getInstrumentName();

		IInstrumentConfig config = collator.getLastConfig();

		if (config == null) {
			failed(660701, "No config specified - cannot determine exposure instrument");
			return null;
		}
		// re-assign -should be the same !!
		instrumentName = config.getInstrumentName();

		// now turn this into an old style config
		// InstrumentConfig oconfig = null;

		// this will fall out if there was no config
		// try {
		// oconfig = ConfigTranslator.translateToOldStyleConfig(config);
		// } catch (Exception e) {
		// failed(444, "Config translator failed", e);
		// return null;
		// }

		if (exposure instanceof XMultipleExposure) {
			XMultipleExposure xmult = (XMultipleExposure) exposure;

			exposureTime = xmult.getExposureTime();
			repeat = xmult.getRepeatCount();
			standard = xmult.isStandard();

			obsid = xmult.getName();
			if (obsid == null || obsid.equals(""))
				obsid = "0";

			expTaskName = (standard ? "STD_" : "SCIENCE_") + "MULTRUN(" + instrumentName + ":" + repeat + "x"
					+ (int) (exposureTime / 1000.0) + "s)";

			// e.g. SCIENCE_MULTRUN(RATCAM:4x200s)

		} else if (exposure instanceof XPeriodExposure) {
			XPeriodExposure xperex = (XPeriodExposure) exposure;

			// this should really be a duration here !!
			exposureTime = xperex.getExposureTime();
			standard = xperex.isStandard();

			obsid = xperex.getName();
			if (obsid == null || obsid.equals(""))
				obsid = "0";

			expTaskName = (standard ? "STD_" : "SCIENCE_") + "PERIODTRIGEX(" + instrumentName + ":"
					+ (int) (exposureTime / 1000.0) + "s)";

			// e.g. STD_PERIODTRIGEX(RINGO2:60s)

		} else if (exposure instanceof XPeriodRunAtExposure) {
			XPeriodRunAtExposure xperunat = (XPeriodRunAtExposure) exposure;

			exposureTime = xperunat.getExposureLength();
			exposureDuration = xperunat.getTotalExposureDuration();
			exposureStart = xperunat.getRunAtTime();

			standard = xperunat.isStandard();

			obsid = xperunat.getName();
			if (obsid == null || obsid.equals(""))
				obsid = "0";

			expTaskName = (standard ? "STD_" : "SCIENCE_") + "PERIODRUNAT(" + instrumentName + ":"
					+ (int) (exposureDuration / 1000.0) + "s/" + (int) (exposureTime / 1000.0) + "s)";

			// e.g. SCIENCE_PERRUNAT(RATCAM:200s/20s)

		}

		// } else if (exposure instanceof TimedMultrun) {
		// TimedMultrun timex = (TimedMultrun)exposure;
		// } else if (exposure instanceof SigNoiseExposure) {
		// SigNoiseExposure sigexp = (SigNoiseExposure)exposure;
		// }

		// obs = new Observation(obsid);

		String obsPath = "/ODB/" + group.getTag().getName() + "/" + group.getUser().getName() + "/"
				+ group.getProposal().getName() + "/" + group.getName();
		// obs.setPath(obsPath);
		// e.g. /ODB/JMU/Smith.Fred/JL09B23/Gr16

		// obs.setInstrumentConfig(oconfig);
		// obs.setNumRuns(repeat);
		// obs.setExposeTime((float) exposureTime);
		// Mosaic mosaic = new Mosaic("Mosaic");
		// mosaic.setPattern(Mosaic.SINGLE);
		// mosaic.setCellsRA(1);
		// mosaic.setCellsDec(1);
		// obs.setMosaic(mosaic);

		// this will switch on the Agmonitor...
		// if (autoguide.getAutoguiderCommand() == IAutoguiderConfig.ON)
		// obs.setAutoGuiderUsageMode(TelescopeConfig.AGMODE_MANDATORY);
		Exposure_Task expTask = new Exposure_Task(name + "/" + expTaskName, this, exposure, instrumentName, obsPath, collator);

		// Do we need DPRT reduction ?
		try {
			InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
			InstrumentCalibrationProvider icp = ireg.getCalibrationProvider(instId);
			InstrumentCalibration ical = icp.getCalibrationRequirements();
			boolean dprt = ical.requiresRealTimeReduction();
			if (dprt) {
				taskLog.log(3, "Instrument requires DPRT reduction");
				expTask.setDprt(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// all that happens is the instrument will not DPRT the exposure -
			// this may be ok
			taskLog.log(1, "Unable to determine DPRT option for exposure, will not be reduced");
		}

		// if (exposure instanceof XPeriodRunAtExposure) {
		// expTask.setDurationLimit(true);
		// expTask.setExposureDuration(exposureDuration);
		// expTask.setFixed(true);
		// expTask.setFixedTime(exposureStart);
		// }
		taskList.addTask(expTask);

		return taskList;

	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);

		taskLog.log(1, "Failed Exposure:" + exposure.getActionDescription() + " Code: " + errorIndicator.getErrorCode()
				+ " Reason: " + errorIndicator.getErrorString());

		// SPECIAL FUDGE FOR NSO MOON OBSERVATIONS:
		// IF the instrument error codes change then this will NOT work

		// check if this is just a DP(RT) error
		ErrorIndicator err = task.getErrorIndicator();
		if (err.getErrorCode() == 1300600) {
			taskList.skip(task);
			return;
		}

		String obsPathName = "/ODB/" + group.getTag().getName() + "/" + group.getUser().getName() + "/"
				+ group.getProposal().getName() + "/" + group.getName();
		ObservationStatusInfo info = new ObservationStatusInfo(System.currentTimeMillis(), obsPathName,
				ObservationStatusInfo.FAILED, "Failed due to: " + errorIndicator.getErrorString());
		info.setErrorCode(errorIndicator.getErrorCode());

		Telemetry.getInstance().publish("OBS", info);

		IInstrumentConfig config = collator.getLastConfig();
		String instName = config.getInstrumentName();

		failed(err.getErrorCode(), "Exposure failure (" + instName + ")");
	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		// TODO this should be the name.
		taskLog.log(1, CLASS, name, "onInit", "** Completed exposure: " + exposure.getActionDescription());

		// NOTE deprecated but may be used by the TEA to process asynch updates
		// ....
		String obsPathName = "/ODB/" + group.getTag().getName() + "/" + group.getUser().getName() + "/"
				+ group.getProposal().getName() + "/" + group.getName();
		ObservationStatusInfo info = new ObservationStatusInfo(System.currentTimeMillis(), obsPathName,
				ObservationStatusInfo.COMPLETED, "Observation completed");

		Telemetry.getInstance().publish("OBS", info);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		super.preInit();
		// TODO need iexposure to extend IPhase2 to give name.
		// getComponentName()
		String obsid = "UNKNOWN";
		if (exposure instanceof XMultipleExposure)
			obsid = ((XMultipleExposure) exposure).getName();
		else if (exposure instanceof XPeriodExposure)
			obsid = ((XPeriodExposure) exposure).getName();
		else if (exposure instanceof XPeriodRunAtExposure)
			obsid = ((XPeriodRunAtExposure) exposure).getName();

		if (obsid == null || obsid.equals(""))
			obsid = "UNKNOWN";
		FITS_HeaderInfo.current_OBSID.setValue(obsid);

		// TODO here is where we get the predicted seeing and DE-corrrect for
		// inst wav and targ elev.
		// infact tmm.exec.ExposureTk does this anyway !!!

		FITS_HeaderInfo.clearAcquisitionHeaders();

		// acquisition image
		if (collator.hasAcquired()) {
			FITS_HeaderInfo.current_ACQIMG.setValue(collator.getAcquireImage());
			String acqInst = collator.getAcqInstrument();
			if (acqInst != null)
				FITS_HeaderInfo.current_ACQINST.setValue(acqInst);

			if (collator.getAcqConfig() != null) {
				IAcquisitionConfig acqConfig = collator.getAcqConfig();
				String acqModeStr = "NONE";
				switch (acqConfig.getMode()) {
				case IAcquisitionConfig.BRIGHTEST:
					acqModeStr = "BRIGHTEST";
					break;
				case IAcquisitionConfig.WCS_FIT:
					acqModeStr = "WCS_FIT";
					break;
				}
				FITS_HeaderInfo.current_ACQMODE.setValue(acqModeStr);
				DetectorArrayPosition acqOffset = collator.getAcquireOffset();
				if (acqOffset != null) {
					FITS_HeaderInfo.current_ACQXPIX.setValue(new Double(acqOffset.getDetectorArrayPositionX()));
					FITS_HeaderInfo.current_ACQYPIX.setValue(new Double(acqOffset.getDetectorArrayPositionY()));
				}
			}
		}

		// we should be on-target so get the latest fits from the tcs...
		try {
			taskLog.log(1, CLASS, name, "preInit", "Requesting telescope to obtain upto date status via checkServices");
			
			BasicTelescope bt = (BasicTelescope) RCS_Controller.controller.getTelescope();
			bt.checkServices();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ExposureTask: Error calling checkServices() on telescope, ignored");
		}

	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		String obsid = "UNKNOWN";
		if (exposure instanceof XMultipleExposure)
			obsid = ((XMultipleExposure) exposure).getName();
		else if (exposure instanceof XPeriodExposure)
			obsid = ((XPeriodExposure) exposure).getName();
		else if (exposure instanceof XPeriodRunAtExposure)
			obsid = ((XPeriodRunAtExposure) exposure).getName();

		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting execution of exposure: " + obsid);
	}

	@Override
	public void onAborting() {
		super.onAborting();
		// need to abort the exposure in progress
		String instId = collator.getInstrumentName();
		IInstrumentConfig config = collator.getLastConfig();
		String instId2 = config.getInstrumentName().toUpperCase();
		taskLog.log(WARNING, 1, CLASS, name, "onAborting", "Checking instrument details for abort operation: I1=" + instId
				+ ", I2=" + instId2);

		Abort_Task abortTask = new Abort_Task(name + "/ABORT", this, instId2);
		taskList.addTask(abortTask);

		// NOTE this might be an ABORT or a FRODO_ABORT depending on what
		// instrument was involved.

	}

}

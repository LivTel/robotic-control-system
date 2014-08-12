/**
 * 
 */
package ngat.rcs.sciops;

import ngat.icm.DetectorArrayPosition;
import ngat.icm.InstrumentDescriptor;
import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.ICalibration;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.ILampDef;
import ngat.phase2.XArc;
import ngat.phase2.XBias;
import ngat.phase2.XDark;
import ngat.phase2.XLampFlat;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Abort_Task;
import ngat.rcs.tms.executive.Arc_Task;
import ngat.rcs.tms.executive.Bias_Task;
import ngat.rcs.tms.executive.Dark_Task;
import ngat.rcs.tms.executive.LampFlat_Task;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class CalibrationTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, CALIB = 500
	
	/** The exposure. */
	ICalibration calib;

	ChangeTracker collator;

	/**
	 * @param name
	 * @param manager
	 */
	public CalibrationTask(String name, TaskManager manager, ICalibration calib, ChangeTracker collator) {
		super(name, manager);
		this.calib = calib;
		this.collator = collator;
	}

	@Override
	protected TaskList createTaskList() {

		System.err.println("Create Calib tasklist: last config was: " + collator.getLastConfig());

		if (calib instanceof XLampFlat) {

			XLampFlat lampFlat = (XLampFlat) calib;

			ILampDef lamp = lampFlat.getLamp();
			String lampName = lamp.getLampName();

			// Make up an Obs to pass instrument info inwards...
			//Observation obs = new Observation(calib.getActionDescription());
			String instrumentName = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(660501, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			instrumentName = config.getInstrumentName();
			InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
			
			try {			
				ireg.getStatusProvider(instId);
			} catch (Exception e) {
				failed(660502, "Instrument not found: "+instrumentName);
				return null;
			}
			// now turn this into an old style config
			//InstrumentConfig oconfig = null;

			//try {
			//	oconfig = ConfigTranslator.translateToOldStyleConfig(config);
			//} catch (Exception e) {
			//	led(4fai44, "Config translator failed", e);
			//	return null;
			//}

			//obs.setInstrumentConfig(oconfig);

			LampFlat_Task lampTask = new LampFlat_Task(name + "/LAMPFLAT(" + lampName + ")", this, lampName, instrumentName);
			taskList.addTask(lampTask);

		} else if (calib instanceof XArc) {

			XArc arc = (XArc) calib;

			ILampDef lamp = arc.getLamp();
			String lampName = lamp.getLampName();
			
			String instrumentName = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(660503, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			instrumentName = config.getInstrumentName();
			InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
			
			try {			
				ireg.getStatusProvider(instId);
			} catch (Exception e) {
				failed(660504, "Instrument not found: "+instrumentName);
				return null;
			}
			// Make up an Obs to pass instrument info inwards...
			//Observation obs = new Observation(calib.getActionDescription());
			//String instId = collator.getInstrumentName();
			//IInstrumentConfig config = collator.getLastConfig();
			
			//if (config == null) {
			//	failed(444, "No config specified - cannot determine exposure instrument");
			//	return null;
			//}
			
			// now turn this into an old style config
			//InstrumentConfig oconfig = null;

			//try {
			//	oconfig = ConfigTranslator.translateToOldStyleConfig(config);
			//} catch (Exception e) {
			//	failed(444, "Config translator failed", e);
			//	return null;
			//}

			//obs.setInstrumentConfig(oconfig);

			Arc_Task arcTask = new Arc_Task(name + "/ARC(" + lampName + ")", this, lampName, instrumentName);
			taskList.addTask(arcTask);

		} else if (calib instanceof XDark) {

			XDark dark = (XDark) calib;

			double exposureTime = dark.getExposureTime();

			// Make up an Obs to pass instrument info inwards...
			/*Observation obs = new Observation(calib.getActionDescription());
			String instId = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(444, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			// now turn this into an old style config
			InstrumentConfig oconfig = null;

			try {
				oconfig = ConfigTranslator.translateToOldStyleConfig(config);
			} catch (Exception e) {
				failed(444, "Config translator failed", e);
				return null;
			}
			obs.setInstrumentConfig(oconfig);
*/
			String instrumentName = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(660505, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			instrumentName = config.getInstrumentName();
			InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
			
			try {			
				ireg.getStatusProvider(instId);
			} catch (Exception e) {
				failed(660506, "Instrument not found: "+instrumentName);
				return null;
			}
			
			Dark_Task darkTask = new Dark_Task(name + "/DARK(" + exposureTime + ")", this, exposureTime, instrumentName);
			taskList.addTask(darkTask);

		} else if (calib instanceof XBias) {

			XBias bias = (XBias) calib;

			/*// Make up an Obs to pass instrument info inwards...
			Observation obs = new Observation(calib.getActionDescription());
			String instId = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(444, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			// now turn this into an old style config
			InstrumentConfig oconfig = null;

			try {
				oconfig = ConfigTranslator.translateToOldStyleConfig(config);
			} catch (Exception e) {
				failed(444, "Config translator failed", e);
				return null;
			}
			obs.setInstrumentConfig(oconfig);*/
			String instrumentName = collator.getInstrumentName();
			IInstrumentConfig config = collator.getLastConfig();
			
			if (config == null) {
				failed(660507, "No config specified - cannot determine exposure instrument");
				return null;
			}
			
			instrumentName = config.getInstrumentName();
			InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);
			
			try {			
				ireg.getStatusProvider(instId);
			} catch (Exception e) {
				failed(660508, "Instrument not found: "+instrumentName);
				return null;
			}
			Bias_Task biasTask = new Bias_Task(name + "/BIAS", this, instrumentName);
			taskList.addTask(biasTask);

		}

		return taskList;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onDisposal()
	 */
	@Override
	public void onDisposal() {
		super.onDisposal();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		super.preInit();

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

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onInit()
	 */
	@Override
	public void onInit() {
		super.onInit();

	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		failed(err.getErrorCode(), "Calibration failure");
	}

	@Override
	public void onAborting() {
		super.onAborting();
		// need to abort the exposure in progress
		String instId = collator.getInstrumentName();
		IInstrumentConfig config = collator.getLastConfig();
		String instId2 = config.getInstrumentName().toUpperCase();
		taskLog.log(WARNING, 1, CLASS, name, "onAborting", "Checking instrument details for abort operation: I1="
				+ instId + ", I2=" + instId2);

		Abort_Task abortTask = new Abort_Task(name + "/ABORT", this, instId2);
		taskList.addTask(abortTask);

		// NOTE this might be an ABORT or a FRODO_ABOT depending on what
		// instrument was involved.

	}

}

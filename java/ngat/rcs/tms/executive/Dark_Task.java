/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.ISS_INST.DARK;
import ngat.message.ISS_INST.FILENAME_ACK;
import ngat.message.ISS_INST.FRODOSPEC_DARK;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.FrodoSpecConfig;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 *
 */
public class Dark_Task extends Default_TaskImpl {

	private double exposureTime;

	private String instrumentName;
	
	public Dark_Task(String name, TaskManager manager, double exposureTime, String instrumentName) {
		super(name, manager, instrumentName);
		this.exposureTime = exposureTime;
		this.instrumentName = instrumentName;
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();
		if (instrumentName.equalsIgnoreCase("FRODO_RED")) {
			FRODOSPEC_DARK frodoDark = new FRODOSPEC_DARK(name);
			frodoDark.setArm(FrodoSpecConfig.RED_ARM);
			frodoDark.setExposureTime((int)exposureTime);			
			command = frodoDark;
		} else if	
		(instrumentName.equalsIgnoreCase("FRODO_BLUE")) {	
			FRODOSPEC_DARK frodoDark = new FRODOSPEC_DARK(name);
			frodoDark.setArm(FrodoSpecConfig.BLUE_ARM);
			frodoDark.setExposureTime((int)exposureTime);			
			command = frodoDark;			
		} else {
			DARK dark = new DARK(name);
			dark.setExposureTime((int)exposureTime);
			command = dark;
		}

		logger.log(1, CLASS, name, "onInit", "Starting Dark exposure with exposure time: "+exposureTime+"ms");
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "The Dark exposure has completed");
	}

	/** Handle ACKs from Acquisition instrument. */
	@Override
	public void handleAck(ACK ack) {
		super.handleAck(ack);

		logger.log(1, CLASS, name, "handleAck", "Ack timeToComplete: " + ack.getTimeToComplete() + " ms");

		if (ack instanceof FILENAME_ACK) {
			FILENAME_ACK fack = (FILENAME_ACK) ack;

			String fileName = fack.getFilename();
			if (fileName == null)
				fileName = "NO_FILENAME_AVAILABLE";

			logger.log(2, CLASS, name, "handleAck", "Dark exposure completed:" + " File: " + fileName);

		}

	}

	
	
}

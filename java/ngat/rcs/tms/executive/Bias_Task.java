/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.ISS_INST.BIAS;
import ngat.message.ISS_INST.FILENAME_ACK;
import ngat.message.ISS_INST.FRODOSPEC_BIAS;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.FrodoSpecConfig;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 *
 */
public class Bias_Task extends Default_TaskImpl {
	
	/** The Instrument to be used. */
	private String instrumentName;
	
	public Bias_Task(String name, TaskManager manager, String instrumentName) {
		super(name, manager, instrumentName);
		this.instrumentName = instrumentName;		
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();

		if (instrumentName.equalsIgnoreCase("FRODO_RED")) {
			FRODOSPEC_BIAS frodoBias = new FRODOSPEC_BIAS(name);
			frodoBias.setArm(FrodoSpecConfig.RED_ARM);	
			command = frodoBias;
		} else if	
			(instrumentName.equalsIgnoreCase("FRODO_BLUE")) {
			FRODOSPEC_BIAS frodoBias = new FRODOSPEC_BIAS(name);
			frodoBias.setArm(FrodoSpecConfig.BLUE_ARM);	
			command = frodoBias;
		} else {
			BIAS bias = new BIAS(name);	
			command = bias;
		}

		logger.log(1, CLASS, name, "onInit", "Starting Bias exposure");
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "The Bias exposure has completed");
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

			logger.log(2, CLASS, name, "handleAck", "Bias exposure completed:" + " File: " + fileName);

		}

	}

}

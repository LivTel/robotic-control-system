/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.ISS_INST.FILENAME_ACK;
import ngat.message.ISS_INST.FRODOSPEC_LAMPFLAT;
import ngat.message.ISS_INST.LAMPFLAT;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.FrodoSpecConfig;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 * 
 */
public class LampFlat_Task extends Default_TaskImpl {

	private String lamp;

	private String instrumentName;	

	public LampFlat_Task(String name, TaskManager manager, String lamp, String instrumentName) {
		super(name, manager, instrumentName);
		this.lamp = lamp;
		this.instrumentName = instrumentName;
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();

		if (instrumentName.equalsIgnoreCase("FRODO_RED")) {
			FRODOSPEC_LAMPFLAT frodoLampFlat = new FRODOSPEC_LAMPFLAT(name);
			frodoLampFlat.setArm(FrodoSpecConfig.RED_ARM);
			frodoLampFlat.setLamp(lamp);
			command = frodoLampFlat;
		} else if	
			(instrumentName.equalsIgnoreCase("FRODO_BLUE")) {	
			FRODOSPEC_LAMPFLAT frodoLampFlat = new FRODOSPEC_LAMPFLAT(name);
			frodoLampFlat.setArm(FrodoSpecConfig.BLUE_ARM);
			frodoLampFlat.setLamp(lamp);
			command = frodoLampFlat;			
		} else {
			LAMPFLAT lampFlat = new LAMPFLAT(name);
			lampFlat.setLamp(lamp);
			command = lampFlat;
		}

		logger.log(1, CLASS, name, "onInit", "Starting LampFlat using: " + lamp);
	}

    @Override
	protected void logExecutionStatistics() {
	logger.log(3, CLASS, name, "logExecStats", "EXEC_TIME for " + CLASS + " "+instrumentName+" : "+lamp
		   + " : "+ (System.currentTimeMillis() - startTime));
    }

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Lampflat completed");
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

			logger.log(2, CLASS, name, "handleAck", "Flat exposure completed:" + " File: " + fileName);

		}

	}

}

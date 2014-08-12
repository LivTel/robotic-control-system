/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.ISS_INST.ARC;
import ngat.message.ISS_INST.FILENAME_ACK;
import ngat.message.ISS_INST.FRODOSPEC_ARC;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.FrodoSpecConfig;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 * 
 */
public class Arc_Task extends Default_TaskImpl {

	private String lamp;

	/** The Instrument to be used. */
	private String instrumentName;

	public Arc_Task(String name, TaskManager manager, String lamp, String instrumentName) {
		super(name, manager, instrumentName);
		this.lamp = lamp;
		this.instrumentName = instrumentName;
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();

		if (instrumentName.equalsIgnoreCase("FRODO_RED")) {
			FRODOSPEC_ARC frodoArc = new FRODOSPEC_ARC(name);
			frodoArc.setLamp(lamp);
			frodoArc.setArm(FrodoSpecConfig.RED_ARM);
			command = frodoArc;
		} else if (instrumentName.equalsIgnoreCase("FRODO_BLUE")) {
			FRODOSPEC_ARC frodoArc = new FRODOSPEC_ARC(name);
			frodoArc.setLamp(lamp);
			frodoArc.setArm(FrodoSpecConfig.BLUE_ARM);
			command = frodoArc;
		} else {
			ARC arc = new ARC(name);
			arc.setLamp(lamp);
			command = arc;
		}

		logger.log(1, CLASS, name, "onInit", "Starting Arc using: " + lamp);
	}

	@Override
	protected void logExecutionStatistics() {
		
		logger.log(3, CLASS, name, "logExecStats", "EXEC_TIME for " + CLASS + " : " + instrumentName + " : " + lamp
				+ " : " + (System.currentTimeMillis() - startTime));

	// TODO	extra params passed in via Collator
		// EXEC_TIME for ArcTask : FRODO_RED : HI : 1x1 : Xe : 2345670
		
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Arc completed");
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

			logger.log(2, CLASS, name, "handleAck", "Arc exposure completed:" + " File: " + fileName);

		}

	}

}

/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.RCS_BSS.BEAM_STEER;
import ngat.message.base.COMMAND_DONE;
import ngat.phase2.XBeamSteeringConfig;
import ngat.rcs.tms.TaskManager;

/**
 * @author eng
 *
 */
public class BeamSteerExecutiveTask extends Default_TaskImpl {
	
	 /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;
    
    /** The steering configuration.*/
    private XBeamSteeringConfig beamConfig;

	/**
	 * @param name
	 * @param manager
	 * @param beamConfig
	 * @param cid
	 */
	public BeamSteerExecutiveTask(String name, TaskManager manager, XBeamSteeringConfig beamConfig) {
		super(name, manager, "BSS");
		this.beamConfig = beamConfig;
	}
	
	/** Carry out subclass specific initialization. */
	@Override
	public void onInit() {
		super.onInit();
		// Set up the appropriate COMMAND.
		BEAM_STEER steer = new BEAM_STEER(name);
		steer.setBeamConfig(beamConfig);
		
		command = steer;

		logger.log(1, CLASS, name, "onInit", "Starting beam-steerage using: "+beamConfig);

	}
	
	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	public void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);

		logger.log(1, CLASS, name, "onCompletion", "Completed beam steerage");
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	public void onDisposal() {
		super.onDisposal();
	}
    
}

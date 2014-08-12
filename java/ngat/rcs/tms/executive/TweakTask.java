/**
 * 
 */
package ngat.rcs.tms.executive;

import ngat.message.RCS_TCS.TWEAK;
import ngat.message.base.COMMAND_DONE;
import ngat.rcs.tms.TaskManager;

/**
 * Send a tweak based on a rotator position for alignment adjustment
 * 
 * @author eng
 * 
 */
public class TweakTask extends Default_TaskImpl {

	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, TWEAK = 1500

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 30000L;
	
	private TWEAK tweak;
	
	private double tweakOffsetX;
	private double tweakOffsetY;
	
	/**
	 * @param name
	 * @param manager
	 * @param cid
	 */
	public TweakTask(String name, TaskManager manager, double tweakOffsetX, double tweakOffsetY) {
		super(name, manager, "CIL_PROXY");

		this.tweakOffsetX = tweakOffsetX;
		this.tweakOffsetY = tweakOffsetY;
		// -------------------------------
		// Set up the appropriate COMMAND.
		// -------------------------------
		tweak = new TWEAK(name);
		// these must be in rads...
		tweak.setXOffset(tweakOffsetX);
		tweak.setYOffset(tweakOffsetY);
		
		command = tweak;

	}

	public void setTweakOffsets(double tweakOffsetX, double tweakOffsetY) {
		
		// potentially resetting the tweak offsets here !
		this.tweakOffsetX = tweakOffsetX;
		this.tweakOffsetY = tweakOffsetY;
		tweak.setXOffset(tweakOffsetX);
		tweak.setYOffset(tweakOffsetY);
	}
	
	/** Returns the default time for this command to execute. */
	public static long getDefaultTimeToComplete() {

		return DEFAULT_TIMEOUT;
	}

	/**
	 * Compute the estimated completion time.
	 * 
	 * @return The initial estimated completion time in millis.
	 */
	@Override
	protected long calculateTimeToComplete() {
		return getDefaultTimeToComplete();
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();
		logger.log(1, CLASS, name, "onInit", "Starting tweaks");
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Tweaks completed");
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	protected void onDisposal() {
		super.onDisposal();
	}
}

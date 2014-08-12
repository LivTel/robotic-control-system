/**
 * 
 */
package ngat.rcs.iss;

import ngat.eip.EIPHandle;
import ngat.eip.EIPPLC;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/** Temporary plugin to handle science-fold deploy/stow. 
 * This will later be replaced by the ScienceFold mechanism in tcm.
 * @author eng
 *
 */
public class IssScienceFoldPlugin {

	private EIPPLC plc;

	private LogGenerator logger;
	
	public IssScienceFoldPlugin() {
		plc = new EIPPLC();
		Logger alogger = LogManager.getLogger("TASK");
		logger = alogger.generate().system("RCS")
			.subSystem("ISS")
			.srcCompClass("ScienceFold")
			.srcCompId("SFDPlugin");
	}

	protected static final int PLC_TYPE = EIPPLC.PLC_TYPE_MICROLOGIX1100;

	protected static final String HOSTNAME = "ltlampplc";
	/**
	 * The backplane containing the PLC. Part of the Ethernet/IP addressing.
	 */
	protected static final int BACKPLANE = 1;
	/**
	 * The slot containing the PLC. Part of the Ethernet/IP addressing.
	 */
	protected static final int SLOT = 0;

	protected static final String PLC_CONTROL_ADDRESS = "N55:1/0";
	/**
	 * Control address for setting whether to stow/deploy.
	 */
	protected static final String DEPLOY_STATUS_ADDRESS = "N55:2/1";
	/**
	 * Control address for setting whether to stow/deploy.
	 */
	protected static final String STOW_STATUS_ADDRESS = "N55:2/0";

	protected static final int TIMEOUT_COUNT = 60;

	public static final int STOW_POSITION = 0; // or something more complex from
	// TCSTATSTSUS
	// ie public static final int TcsStatus.POSITION_STOWED = 351;

	public static final int DEPLOY_POSITION = 1; // or something more complex

	// from TCSTATSTSUS


	
	public void checkPosition() throws Exception {
		
		EIPHandle handle = null;
		boolean done, stowed, deployed;
		int timeoutIndex = 0;

		try {

			logger.create().info().level(3).msg("Checking position").send();
			handle = plc.createHandle(HOSTNAME, BACKPLANE, SLOT, PLC_TYPE);
			logger.create().info().level(3).msg("Opening handle").send();
			plc.open(handle);
			stowed = plc.getBoolean(handle, STOW_STATUS_ADDRESS);
			deployed = plc.getBoolean(handle, DEPLOY_STATUS_ADDRESS);
			logger.create().info().level(3).msg(
					"StowAddr: ["+STOW_STATUS_ADDRESS+"] = "+ stowed + 
					", DeployAddr: [" + DEPLOY_STATUS_ADDRESS+"] = "+deployed).send();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(this.getClass().getName() + ":checkPosition:Check failed: "+e);
		}
	}
		
	public void stow() throws Exception {
		
		EIPHandle handle = null;
		boolean done, stowed, deployed;
		int timeoutIndex = 0;

		try {

			logger.create().info().level(3).msg("Requesting stow").send();

			handle = plc.createHandle(HOSTNAME, BACKPLANE, SLOT, PLC_TYPE);

			logger.create().info().level(3).msg("Opening handle").send();

			plc.open(handle);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(this.getClass().getName() + ":stow:Stow failed: "+e);
		}
		
		try {
			// see if its already stowed
			stowed = plc.getBoolean(handle, STOW_STATUS_ADDRESS);
			deployed = plc.getBoolean(handle, DEPLOY_STATUS_ADDRESS);
			logger.create().info().level(3).msg(
					"Checked initial status flags: StowAddr: ["+STOW_STATUS_ADDRESS+"] = "+ stowed + 
					", DeployAddr: [" + DEPLOY_STATUS_ADDRESS+"] = "+deployed).send();
			if (stowed && (!deployed)) {
				logger.create().info().level(3).msg("Mechanism is already stowed, no action taken").send();			
				return;
			}

			// set control demand
			logger.create().info().level(3).msg("Setting demand:SetAddr: [" + PLC_CONTROL_ADDRESS + "] to true.").send();

			plc.setBoolean(handle, PLC_CONTROL_ADDRESS, true);
			
			// wait until science fold moved, or timeout
			done = false;		
			timeoutIndex = 0;
			while (!done) {
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:Getting status:Get "+plcStowStatusAddress+".");
				stowed = plc.getBoolean(handle, STOW_STATUS_ADDRESS);
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:"+plcStowStatusAddress+" returned "+statusValue+".");
				if (stowed) {
					// fold is in position
					logger.create().info().level(3).msg("Science fold is now stowed.");
					done = true;
				} else {
					// check timeout and wait a bit
					timeoutIndex++;
					if (timeoutIndex > TIMEOUT_COUNT) {
						// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
						// ":stow:Science fold stow timed out.");
						throw new Exception(this.getClass().getName() + ":stow():Stow failed:timeout after "
								+ timeoutIndex + " seconds.");
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ix) {
					}
				}
			}// end while
		
		} finally {
			// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
			// ":stow:Closing handle.");
			try {
				plc.close(handle);
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:Destroying handle.");
			} catch (Exception e) {
				e.printStackTrace();
				// not a real problem if it did stow
			}
			try {
				plc.destroyHandle(handle);
			} catch (Exception e) {
				e.printStackTrace();
				// not a real problem if it did stow
			}
		}
	
	}

	public void deploy() throws Exception {
		
		EIPHandle handle = null;
		boolean done, stowed, deployed;
		int timeoutIndex = 0;

		try {

			logger.create().info().level(3).msg("Requesting deploy").send();

			handle = plc.createHandle(HOSTNAME, BACKPLANE, SLOT, PLC_TYPE);

			logger.create().info().level(3).msg("Opening handle").send();

			plc.open(handle);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(this.getClass().getName() + ":deploy:Deploy failed: "+e);
		}

		try {
			// see if its already deployed
			stowed = plc.getBoolean(handle, STOW_STATUS_ADDRESS);
			deployed = plc.getBoolean(handle, DEPLOY_STATUS_ADDRESS);
			logger.create().info().level(3).msg(
					"Checked initial status flags: StowAddr: ["+STOW_STATUS_ADDRESS+"] = "+ stowed + 
					", DeployAddr: [" + DEPLOY_STATUS_ADDRESS+"] = "+deployed).send();			
			if ((!stowed) && deployed) {
				logger.create().info().level(3).msg("Mechanism is already deployed, no action taken").send();			
				return;
			}

			// set control demand
			logger.create().info().level(3).msg("Setting demand:SetAddr: [" + PLC_CONTROL_ADDRESS + "] to false.").send();

			plc.setBoolean(handle, PLC_CONTROL_ADDRESS, false);

			// wait until science fold moved, or timeout
			timeoutIndex = 0;
			done = false;
			while (!done) {
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:Getting status:Get "+plcStowStatusAddress+".");
				deployed = plc.getBoolean(handle, DEPLOY_STATUS_ADDRESS);
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:"+plcStowStatusAddress+" returned "+statusValue+".");
				if (deployed) {
					// fold is in position
					logger.create().info().level(3).msg("Science fold is now deployed.");
					done = true;
				} else {
					// check timeout and wait a bit
					timeoutIndex++;
					if (timeoutIndex > TIMEOUT_COUNT) {
						// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
						// ":stow:Science fold stow timed out.");
						throw new Exception(this.getClass().getName() + ":deploy():Deploy failed:timeout after "
								+ timeoutIndex + " seconds.");
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ix) {
					}
				}
			}// end while
		
		} finally {
			// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
			// ":stow:Closing handle.");
			try {
				plc.close(handle);
				// logger.log(Logging.VERBOSITY_VERY_VERBOSE,this.getClass().getName()+
				// ":stow:Destroying handle.");
			} catch (Exception e) {
				e.printStackTrace();
				// not a real problem if it did deploy
			}
			try {
				plc.destroyHandle(handle);
			} catch (Exception e) {
				e.printStackTrace();
				// not a real problem if it did deploy
			}
		}
		
	}

}

/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package ngat.rcs.iss;

import ngat.net.*;
import ngat.phase2.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;

public class ISS_AG_START_CommandImpl extends ISS_CommandImpl {

    public static final String CLASS = "ISS_AG_START_CommandImpl";

    /** Stores the currently-in-use TelescopeConfig - got from ISS_Server.*/
    TelescopeConfig config;

    protected boolean forwarding;

    // by default we will override the AGSTART and fake a reply !
    // this allows manual observations to take place without using AG.
    private static boolean overrideForwarding = true;

    public ISS_AG_START_CommandImpl(JMSMA_ProtocolServerImpl serverImpl,
				    COMMAND receivedCommand) {
	super(serverImpl, receivedCommand);
	config = ISS_Server.getCurrentTelescopeConfig();
    }

    /** Override forwarding for this command.*/
    public static void setOverrideForwarding(boolean ovr) {overrideForwarding = ovr;}

    @Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
	issLog.log(2, CLASS, "-", "processReceivedCommand",
		   "Received command: "+receivedCommand);
    }

    @Override
	public long calculateTimeToComplete() { return 60000L; }

    /** Overridden. If the current TelescopeConfig requires the use of the
     * AutoGuider then this method retruns true and sets a flag <i>forwarding</i>
     * to indicate so to other methods. If the usage mode is <b>OPTIONAL</b> and
     * the AG is available we try to send an <b>AUTOGUIDE ON</b> command to the TCS. 
     * In the event of usage mode <b>NEVER</b> it returns false and sets a flag.
     */
    @Override
	public boolean doesForward() { 

	// 31-aug-07 snf The ISS no longer does any AGSTART forwarding - this will be re-enabled with 
	// standalone ISS when implemented but not in this code.
	return false;


// 	// Check if command forwarding to TCS has been overridden.
// 	if (overrideForwarding)
// 	    return false;

// 	switch (ISS.getInstance().currentAutoguiderUsageMode) {
// 	case TelescopeConfig.AGMODE_NEVER:
// 	    forwarding = false;
// 	    return false;
// 	case TelescopeConfig.AGMODE_OPTIONAL:
// 	    // ### CHECK Netwrk i/f replies for this.
// 	    // ### 11-jul-2006 snf Monitoring of agswstate on FTS yields OKAY. 
// 	    // ### WARN may just mean needs cooling.
// 	    int agSwState = StatusPool.latest().autoguider.agSwState;
// 	    if (agSwState == TCS_Status.STATE_OKAY ||
// 		agSwState == TCS_Status.STATE_WARN) {
// 		forwarding = true;
// 		return true;
// 	    } else {
// 		forwarding = false;
// 		return false;
// 	    }
// 	case TelescopeConfig.AGMODE_MANDATORY:
// 	    forwarding = true;
// 	    return true; 
// 	}
// 	return true;
    }

    @Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) { 
	issLog.log(2, CLASS, "-", "translateCommand",
		   "Translating command");
	AUTOGUIDE ag = new AUTOGUIDE(command.getId());
	ag.setState(AUTOGUIDE.ON);
	
	// Work out the selection mode.
	switch ( config.getAutoGuiderStarSelectionMode()) {
	case TelescopeConfig.STAR_SELECTION_RANK:
	    int rank = config.getAutoGuiderStarSelection1();
	    ag.setMode(AUTOGUIDE.RANK);
	    ag.setRank(1);
	    break;
	case TelescopeConfig.STAR_SELECTION_RANGE:
	    int range1 = config.getAutoGuiderStarSelection1();
	    int range2 = config.getAutoGuiderStarSelection2();
	    ag.setMode(AUTOGUIDE.RANGE);
	    ag.setRange1(range1);
	    ag.setRange2(range2);
	    break;
	case TelescopeConfig.STAR_SELECTION_PIXEL:	   
	    int xPixel = config.getAutoGuiderStarSelection1();
	    int yPixel = config.getAutoGuiderStarSelection2(); 
	    ag.setMode(AUTOGUIDE.PIXEL);
	    ag.setXPix(xPixel);
	    ag.setYPix(yPixel);
	    break;	
	}
	
	return ag;
    }

    @Override
	public void processResponse(COMMAND_DONE response) {
	issLog.log(2, CLASS, "-", "processResponse",
		   "Received response: "+response);
    }
    
    /** If we are <i>forwarding</i> the command then we dont do anything here. If NOT then
     * the command has effectively been ignored and we send a DONE back with the 
     * success flag set.
     */
    @Override
	public COMMAND_DONE makeResponse() {

	AG_START_DONE done = new AG_START_DONE(receivedCommand.getId());
	done.setSuccessful(true);
	done.setErrorNum(0);
	done.setErrorString("Pretending to send AUTOGUIDE ON");
	issLog.log(2, CLASS, "-", "makeResponse",
		   "Pretending to send AUTOGUIDE ON");
	return done;
    
    }

    @Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) { 
	issLog.log(2, CLASS, "-", "translateResponse",
		   "Translating response: "+response);
	AG_START_DONE done = new AG_START_DONE(receivedCommand.getId());
	done.setSuccessful(response.getSuccessful());
	done.setErrorNum(response.getErrorNum());
	done.setErrorString(response.getErrorString());
	return done;
    }
    
}

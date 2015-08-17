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

import java.util.Date;
import ngat.rcs.scm.collation.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;

public class ISS_OFFSET_FOCUS_CommandImpl extends ISS_CommandImpl {

	public static final String CLASS = "ISS_OFFSET_FOCUS_CommandImpl";

	protected double telFocusFilterOffset;

	/** This is set if we have a 0.0 offset so we dont bother to send it. */
	protected boolean overrideNoForward = false;

	public ISS_OFFSET_FOCUS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
	}

	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
		super.processReceivedCommand(receivedCommand);
		// check for zero offset - no send onwards
		if (receivedCommand instanceof OFFSET_FOCUS) {
			double cfo = StatusPool.latest().mechanisms.focusOffset;
			double rfo = ((OFFSET_FOCUS) receivedCommand).getFocusOffset();
			double ifo = ISS.getCurrentFocusOffset();
			Date nowDate = new Date();
			System.err.println(""+nowDate+":Current Focus offset: " + cfo + ", Requested: " + rfo + ", From Obs: " + ifo);
			// basically if the difference between current and requested + obs
			// is > small then send
			if (Math.abs((rfo + ifo) - cfo) < 0.001)
				overrideNoForward = true;
			System.err.println(overrideNoForward ? "No send focus offset" : "Sending offset");
			ISS.setInstrumentFocusOffset(rfo);
		}
	}

	@Override
	public long calculateTimeToComplete() {
		return 20000L;
	}

	@Override
	public boolean doesForward() {

		if (overrideNoForward)
			return false;

		// if (ISS.getInstance().doForward(this.getClass().getName()))
		return true;

		// return false;
	}

	@Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) {

		super.translateCommand(command);

		double receivedOffset = ((OFFSET_FOCUS) command).getFocusOffset();
		double sendOffset = 0.0;

		// ### THIS BLOCK CONCERNS TELFOCUS AND IS NOT FULLY
		// ### UNDERSTOOD - LEAVE THIS COMMENTED OUT FOR NOW
		// // If this is the TelFocus offset we need to save it
		// // and set flag at ISS_Server.
		// if (ISS_Server.getExpectTelFocusOffsetSoon()) {
		// // We are doing a TelFocus.
		// ISS_Server.setExpectTelFocusOffsetSoon(false);
		// // Is this the first one of the night ?
		// if (telFocusFilterOffset > 0.0) {
		// // It isnt. -> Subtract the previous (same?) offset.
		// sendOffset = receivedOffset - telFocusFilterOffset;
		// } else {
		// // It is. -> Just send it on untouched.
		// sendOffset = receivedOffset;
		// }
		// telFocusFilterOffset = receivedOffset;
		// } else {
		// // This isn't a TelFocus -> Subtract the current offset.
		// sendOffset = receivedOffset - telFocusFilterOffset;
		// }

		// Add the offset specified in an observation.
		sendOffset = receivedOffset + ISS.getCurrentFocusOffset();

		// Add on the Focus offset specified (e.g. in Obs.getFocusOffset() )
		// sendOffset = sendOffset + iss.getCurrentFocusOffset();

		issLog.log(2, CLASS, "-", "translateCommand", "Sending focus offset: " + sendOffset);

		DFOCUS df = new DFOCUS(command.getId());
		df.setOffset(sendOffset);
		return df;
	}

	@Override
	public void processResponse(COMMAND_DONE response) {
		super.processResponse(response);
	}

	@Override
	public COMMAND_DONE makeResponse() {
		OFFSET_FOCUS_DONE done = new OFFSET_FOCUS_DONE("FAKED");
		done.setSuccessful(true);
		// done.setErrorString("Pre);
		issLog.log(2, CLASS, "-", "makeResponse", "Faked Focus offset complete");
		return done;
	}

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {

		super.translateResponse(response);

		OFFSET_FOCUS_DONE done = new OFFSET_FOCUS_DONE(receivedCommand.getId());
		done.setSuccessful(response.getSuccessful());
		done.setErrorNum(response.getErrorNum());
		done.setErrorString(response.getErrorString());
		return done;
	}

}

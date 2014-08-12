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
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;

/**
 * Handles the ISS_INST FOCUS_CONTROL command sent by an Instrument to request
 * information about the curent exposure. The data is retrieved from the
 * TCS_StatusPool and the current Observation and TelescopeConfig. <br>
 * <br>
 * $Id: ISS_FOCUS_CONTROL_CommandImpl.java,v 1.2 2008/01/07 10:44:47 snf Exp $
 */
public class ISS_FOCUS_CONTROL_CommandImpl extends ISS_CommandImpl {

	public static final String CLASS = "ISS_FOCUS_CONTROL_CommandImpl";

	/** Timeout (millis) for asynch status requests. */
	public static final long STATUS_TIMEOUT = 5000L;

    private String instrumentName;

	/** Create an ISS_FOCUS_CONTROL_CommandImpl using the specifed parameters. */
	public ISS_FOCUS_CONTROL_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
	}

	/**
	 * Overridden to gather the FITS information for the current exposure. The
	 * data is pushed into a List and sent back to the client via the
	 * serverImpl.
	 */
	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
		super.processReceivedCommand(receivedCommand);

		instrumentName = ((FOCUS_CONTROL)receivedCommand).getInstrumentName();

		ISS.setBeamControlInstrument(instrumentName);		

		FITS_HeaderInfo.current_BFOCCTRL.setValue(instrumentName);
		
	}

	@Override
	public long calculateTimeToComplete() {
		return 10000L;
	}

	@Override
	public boolean doesForward() {
		return false;
	}

	@Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) {
		super.translateCommand(command);
		return null;
	}

	@Override
	public void processResponse(COMMAND_DONE response) {
		super.processResponse(response);
	}

	@Override
	public COMMAND_DONE makeResponse() {

		super.makeResponse();

		FOCUS_CONTROL_DONE done = new FOCUS_CONTROL_DONE(receivedCommand.getId());
		done.setSuccessful(true);
		done.setErrorNum(0);
		done.setErrorString("");
		
		return done;
	}

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {
		super.translateResponse(response);
		return response;
	}

}

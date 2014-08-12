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
 * 
 * 
 * 
 * $Id: ISS_OFFSET_RA_DEC_CommandImpl.java,v 1.3 2007/09/12 07:55:08 snf Exp snf
 * $
 */
public class ISS_OFFSET_RA_DEC_CommandImpl extends ISS_CommandImpl {

	/** RA offset (rads). */
	double dra;

	/** Dec offset (rads). */
	double ddec;

	public ISS_OFFSET_RA_DEC_CommandImpl(JMSMA_ProtocolServerImpl serverImpl,
			COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
	}

	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
		System.out.println("ISS_OFFSET_RA_DEC_Impl: received command: "
				+ receivedCommand.getId());
	}

	@Override
	public long calculateTimeToComplete() {
		return 120000L;
	}

	@Override
	public boolean doesForward() {
		return true;
	}

	@Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) {

		OFFBY off = new OFFBY(command.getId());
		off.setMode(OFFSET.ARC);
		dra = ((OFFSET_RA_DEC) command).getRaOffset();
		ddec = ((OFFSET_RA_DEC) command).getDecOffset();
		dra = Math.toRadians(dra / 3600.0);
		ddec = Math.toRadians(ddec / 3600.0);
		// may need to add in existing mosaic offsets ?
		double mxoff = ISS.getUserOffsetX();
		double myoff = ISS.getUserOffsetY();

		off.setOffsetRA(dra + mxoff);
		off.setOffsetDec(ddec + myoff);

		System.out.println("ISS_OFFSET_RA_DEC_Impl: translating command:");
		return off;
	}

	@Override
	public void processResponse(COMMAND_DONE response) {
		System.out.println("ISS_OFFSET_RA_DEC_Impl: received response: "
				+ response.getId());
	}

	@Override
	public COMMAND_DONE makeResponse() {

		ngat.message.ISS_INST.OFFSET_RA_DEC_DONE done = new ngat.message.ISS_INST.OFFSET_RA_DEC_DONE(
				"Forwarded");
		done.setSuccessful(true);
		issLog.log(2, CLASS, "-", "makeResponse", "Sending onwards...");

		// // see if we need to switch the AG on and OFF or not.
		// IssAutoguiderAdjustmentController agcon =
		// ISS.getAdjustmentController();
		// if (agcon.getControlState()) {
		// System.err.println("ISS_OFFSET_RA_DEC_Impl: Ag control is enabled");

		// AUTOGUIDE agoff = new AUTOGUIDE(receivedCommand.getId());
		// agoff.setState(AUTOGUIDE.OFF);

		// int errno = 0;
		// String errmsg = null;
		// // Send AGOFF - 60 sec timeout
		// try {
		// JMSHandler handler1 = sendCommand(agoff, 60000);
		// if (handler1.isDone()) {

		// COMMAND_DONE mf_done = handler1.getResponse();

		// if (mf_done.getSuccessful()) {

		int errno = 576;
		String errmsg = null;
		OFFBY offby = new OFFBY(receivedCommand.getId());
		offby.setMode(OFFSET.ARC);
		dra = ((OFFSET_RA_DEC) receivedCommand).getRaOffset();
		ddec = ((OFFSET_RA_DEC) receivedCommand).getDecOffset();
		dra = Math.toRadians(dra / 3600.0);
		ddec = Math.toRadians(ddec / 3600.0);

		// may need to add in existing mosaic offsets ?
		double mxoff = ISS.getUserOffsetX();
		double myoff = ISS.getUserOffsetY();

		offby.setOffsetRA(dra + mxoff);
		offby.setOffsetDec(ddec + myoff);

		try {
			JMSHandler handler1 = sendCommand(offby, 60000);
			if (handler1.isDone()) {

				COMMAND_DONE offby_done = handler1.getResponse();

				if (offby_done.getSuccessful()) {
					// success
					return done;
				} else {
					errmsg = offby_done.getErrorString();
					errno = offby_done.getErrorNum();
				}
			} else {
				errmsg = "OFFBY timeout ?";
			}

		} catch (Exception e) {
			errmsg = "Exception in multi-command forwarding OFFBY: " + e;
		}

		// we get here if any of the above fail....
		done.setSuccessful(false);
		done.setErrorString(errmsg);
		done.setErrorNum(errno);
		return done;

	}

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {
		System.out.println("ISS_OFFSET_RA_DEC_Impl: translating response: "
				+ response.getClass().getName());
		OFFSET_RA_DEC_DONE done = new OFFSET_RA_DEC_DONE(
				receivedCommand.getId());

		if (response.getSuccessful()) {
			// Set the temp offsets but only if the offset actually worked...
			// ISS.setInstrumentOffsets(dra, ddec);
		}

		done.setSuccessful(response.getSuccessful());
		done.setErrorNum(response.getErrorNum());
		done.setErrorString(response.getErrorString());
		return done;
	}

}

/**
 * $Log: ISS_OFFSET_RA_DEC_CommandImpl.java,v $ /** Revision 1.3 2007/09/12
 * 07:55:08 snf /** always ignore isnt demands for autoguider /** /** Revision
 * 1.2 2007/09/12 07:53:00 snf /** changed to send OFFBY using makeResponse - no
 * ag control at moment /** /** Revision 1.1 2006/12/12 08:30:20 snf /** Initial
 * revision /** /** Revision 1.1 2006/05/17 06:34:28 snf /** Initial revision
 * /** /** Revision 1.2 2002/09/16 09:38:28 snf /** *** empty log message ***
 * /** /** Revision 1.1 2001/09/04 11:08:14 snf /** Initial revision /**
 */

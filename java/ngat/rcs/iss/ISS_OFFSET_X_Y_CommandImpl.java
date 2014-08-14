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

import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.message.ISS_INST.INST_TO_ISS;
import ngat.message.ISS_INST.OFFSET_X_Y;
import ngat.message.ISS_INST.OFFSET_X_Y_DONE;
import ngat.message.RCS_TCS.RCS_TO_TCS;
import ngat.message.RCS_TCS.TWEAK;
import ngat.message.base.COMMAND;
import ngat.message.base.COMMAND_DONE;
import ngat.net.JMSMA_ProtocolServerImpl;
import ngat.rcs.RCS_Controller;

/**
 * 
 * 
 * 
 * $Id: ISS_OFFSET_X_Y_CommandImpl.java,v 1.1 2007/11/14 11:28:34 snf Exp snf $
 */
public class ISS_OFFSET_X_Y_CommandImpl extends ISS_CommandImpl {

	/** X offset (rads). */
	double dx;

	/** Y offset (rads). */
	double dy;

	public ISS_OFFSET_X_Y_CommandImpl(JMSMA_ProtocolServerImpl serverImpl,
			COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
	}

	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {
		System.out.println("ISS_OFFSET_X_Y_Impl: received command: "
				+ receivedCommand.getId());
	}

	@Override
	public long calculateTimeToComplete() {
		return 120000L;
	}

	// fudged 12-sep07 to return false as ag commends from inst are now ignored
	// always
	// changed back 13-sept-07 to fix Supircam problem
	@Override
	public boolean doesForward() {
		return true;
	}

	@Override
	public RCS_TO_TCS translateCommand(INST_TO_ISS command) {

		System.out.println("ISS_OFFSET_X_Y_Impl: translating command:");

		TWEAK tweak = new TWEAK(command.getId());
		// off.setMode(OFFSET.ARC);
		dx = ((OFFSET_X_Y) command).getXOffset();
		dy = ((OFFSET_X_Y) command).getYOffset();
		// convert to radians
		double dxa = Math.toRadians(dx / 3600.0);
		double dya = Math.toRadians(dy / 3600.0);
		// may need to add in existing mosaic offsets ?
		// double mxoff = FITS_HeaderInfo.getMosaicOffsetX();
		// double myoff = FITS_HeaderInfo.getMosaicOffsetY();

		String acquireInstrumentName = ISS
				.getCurrentAcquisitionInstrumentName();
		InstrumentDescriptor aid = new InstrumentDescriptor(
				acquireInstrumentName);
		double rotatorOffset = 0.0;
		try {
			InstrumentRegistry ireg = RCS_Controller.controller
					.getInstrumentRegistry();
			InstrumentCapabilities icap = ireg.getCapabilitiesProvider(aid)
					.getCapabilities();
			rotatorOffset = icap.getRotatorOffset();
		} catch (Exception e) {
			// TODO We should be able to fail at this point....
			e.printStackTrace();
			System.err
					.println("ISS_OFFSET_X_Y_Impl:: Failed to compute field rotation, using zero");
		}

		// rotate - this may need to be negative
		double dxp = dxa * Math.cos(rotatorOffset) - dya
				* Math.sin(rotatorOffset);
		double dyp = dxa * Math.sin(rotatorOffset) + dya
				* Math.cos(rotatorOffset);

		// convert to arcsec for display
		double dxb = 3600.0 * Math.toDegrees(dxp);
		double dyb = 3600.0 * Math.toDegrees(dyp);

		System.err.println("ISS_OFFSET_X_Y_Impl:: Acquire instr: "
				+ acquireInstrumentName + " field rotation: "
				+ Math.toDegrees(rotatorOffset));
		System.err.println("ISS_OFFSET_X_Y_Impl:: in:  dx = " + dx + ", dy = "
				+ dy);
		System.err.println("ISS_OFFSET_X_Y_Impl:: out: dx = " + dxb + " dy = "
				+ dyb);

		tweak.setXOffset(dxp);// + mxoff);
		tweak.setYOffset(dyp);// + myoff);

		return tweak;
	}

	@Override
	public void processResponse(COMMAND_DONE response) {
		System.out.println("ISS_OFFSET_X_Y_Impl: received response: "
				+ response.getId());
	}

	// public COMMAND_DONE makeResponse() {

	// ngat.message.ISS_INST.OFFSET_X_Y_DONE done = new
	// ngat.message.ISS_INST.OFFSET_X_Y_DONE("Forwarded");
	// done.setSuccessful(true);
	// issLog.log(2, CLASS, "-", "makeResponse",
	// "Sending onwards...");

	// // // see if we need to switch the AG on and OFF or not.
	// // IssAutoguiderAdjustmentController agcon =
	// ISS.getAdjustmentController();
	// // if (agcon.getControlState()) {
	// // System.err.println("ISS_OFFSET_X_Y_Impl: Ag control is enabled");

	// // AUTOGUIDE agoff = new AUTOGUIDE(receivedCommand.getId());
	// // agoff.setState(AUTOGUIDE.OFF);

	// // int errno = 0;
	// // String errmsg = null;
	// // // Send AGOFF - 60 sec timeout
	// // try {
	// // JMSHandler handler1 = sendCommand(agoff, 60000);
	// // if (handler1.isDone()) {

	// // COMMAND_DONE mf_done = handler1.getResponse();

	// // if (mf_done.getSuccessful()) {

	// int errno = 576;
	// String errmsg = null;
	// OFFBY offby = new OFFBY(receivedCommand.getId());
	// offby.setMode(OFFSET.ARC);
	// dra = ((OFFSET_X_Y)receivedCommand).getRaOffset();
	// ddec = ((OFFSET_X_Y)receivedCommand).getDecOffset();
	// dra = Math.toRadians(dra/3600.0);
	// ddec = Math.toRadians(ddec/3600.0);

	// // may need to add in existing mosaic offsets ?
	// double mxoff = FITS_HeaderInfo.getMosaicOffsetX();
	// double myoff = FITS_HeaderInfo.getMosaicOffsetY();

	// offby.setOffsetRA(dra + mxoff);
	// offby.setOffsetDec(ddec + myoff);

	// try {
	// JMSHandler handler1 = sendCommand(offby, 60000);
	// if (handler1.isDone()) {

	// COMMAND_DONE offby_done = handler1.getResponse();

	// if (offby_done.getSuccessful()) {
	// // success
	// return done;
	// } else {
	// errmsg = offby_done.getErrorString();
	// errno = offby_done.getErrorNum();
	// }
	// } else {
	// errmsg = "OFFBY timeout ?";
	// }

	// } catch (Exception e) {
	// errmsg = "Exception in multi-command forwarding OFFBY: "+e;
	// }

	// // we get here if any of the above fail....
	// done.setSuccessful(false);
	// done.setErrorString(errmsg);
	// done.setErrorNum(errno);
	// return done;

	// }

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {
		System.out.println("ISS_OFFSET_X_Y_Impl: translating response: "
				+ response.getClass().getName());
		OFFSET_X_Y_DONE done = new OFFSET_X_Y_DONE(receivedCommand.getId());

		if (response.getSuccessful()) {
			// Set the temp offsets but only if the offset actually worked...
			// this doesnt apply to TWEAK hopefully ?
			// FITS_HeaderInfo.setInstrumentOffsets(dra, ddec);
		}

		done.setSuccessful(response.getSuccessful());
		done.setErrorNum(response.getErrorNum());
		done.setErrorString(response.getErrorString());
		return done;
	}

}

/**
 * $Log: ISS_OFFSET_X_Y_CommandImpl.java,v $ /** Revision 1.1 2007/11/14
 * 11:28:34 snf /** Initial revision /**
 */

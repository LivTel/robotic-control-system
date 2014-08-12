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

import ngat.rcs.RCS_Controller;
import ngat.rcs.scm.collation.*;
import ngat.tcm.BasicTelescope;

import java.util.*;

import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;
import ngat.fits.FitsHeaderCardImage;

/**
 * Handles the ISS_INST GET_FITS command sent by an Instrument to request
 * information about the curent exposure. The data is retrieved from the
 * TCS_StatusPool and the current Observation and TelescopeConfig. <br>
 * <br>
 * $Id: ISS_GET_FITS_CommandImpl.java,v 1.2 2008/01/07 10:44:47 snf Exp $
 */
public class ISS_GET_FITS_CommandImpl extends ISS_CommandImpl {

	public static final String CLASS = "ISS_GET_FITS_CommandImpl";

	/** Timeout (millis) for asynch status requests. */
	public static final long STATUS_TIMEOUT = 5000L;

	protected Vector list;

	/** Create an ISS_GET_FITS_CommandImpl using the specifed parameters. */
	public ISS_GET_FITS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, COMMAND receivedCommand) {
		super(serverImpl, receivedCommand);
		list = new Vector();
	}

	/**
	 * Overridden to gather the FITS information for the current exposure. The
	 * data is pushed into a List and sent back to the client via the
	 * serverImpl.
	 */
	@Override
	public void processReceivedCommand(INST_TO_ISS receivedCommand) {

		super.processReceivedCommand(receivedCommand);

		issLog.log(2, CLASS, "-", "processReceivedCommand", "Sending status Requests");

		// Asynchronously send status requests for best update.
		//SMM_MonitorClient c_State = SMM_Controller.findMonitor(SHOW.STATE).requestStatus();
		//SMM_MonitorClient c_Source = SMM_Controller.findMonitor(SHOW.SOURCE).requestStatus();
		//SMM_MonitorClient c_Astrometry = SMM_Controller.findMonitor(SHOW.ASTROMETRY).requestStatus();
		//SMM_MonitorClient c_Meteorology = SMM_Controller.findMonitor(SHOW.METEOROLOGY).requestStatus();
		//SMM_MonitorClient c_Time = SMM_Controller.findMonitor(SHOW.TIME).requestStatus();
		//SMM_MonitorClient c_Mechanisms = SMM_Controller.findMonitor(SHOW.MECHANISMS).requestStatus();
		//SMM_MonitorClient c_Autoguider = SMM_Controller.findMonitor(SHOW.AUTOGUIDER).requestStatus();

		// ### START NEW FORMAT

		((BasicTelescope)RCS_Controller.controller.getTelescope()).checkServices();
		
		// ### END NEW FORMAT

		issLog.log(2, CLASS, "-", "processReceivedCommand", "All Requestors are running with timeout "
				+ (STATUS_TIMEOUT / 1000) + " secs");

		// Wait till all requests are in or timedout.
		/*try {
			c_State.waitFor(STATUS_TIMEOUT);
			c_Source.waitFor(STATUS_TIMEOUT);
			c_Astrometry.waitFor(STATUS_TIMEOUT);
			c_Meteorology.waitFor(STATUS_TIMEOUT);
			c_Time.waitFor(STATUS_TIMEOUT);
			c_Mechanisms.waitFor(STATUS_TIMEOUT);
			c_Autoguider.waitFor(STATUS_TIMEOUT);
		} catch (InterruptedException ix) {
			issLog.log(2, CLASS, "-", "processReceivedCommand", "Interrupted waiting for results");

		}*/
		
		// wait for the status updates to propagate back to where they need to go...
		// Basically, each TCS collator has to send a command, the received reply is parsed and handed to 
		// a waiting handler which update the telescope object which then propagates to StatusPool and thence to 
		// the fits headers repository - this should probably register itslef for updates from scope ?
		// If they are not back in time then too bad we cant wait around
		try {
			Thread.sleep(STATUS_TIMEOUT);
		} catch (InterruptedException ix) {
			issLog.log(2, CLASS, "-", "processReceivedCommand", "Interrupted waiting for results");
		}
		
		issLog.log(2, CLASS, "-", "processReceivedCommand", "All status results are back or timed out");

		// Get the Fits info from the local data and TCS-Status.
		list.add(FITS_HeaderInfo.current_TELESCOP);
		// Planetarium, Robotic, Eng, Manual
		list.add(FITS_HeaderInfo.current_TELMODE);

		// Identity.
		if (FITS_HeaderInfo.current_TAGID.getValue() == null)
			FITS_HeaderInfo.current_TAGID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_TAGID);

		if (FITS_HeaderInfo.current_USERID.getValue() == null)
			FITS_HeaderInfo.current_USERID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_USERID);

		if (FITS_HeaderInfo.current_PROGID.getValue() == null)
			FITS_HeaderInfo.current_PROGID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_PROGID);

		if (FITS_HeaderInfo.current_PROPID.getValue() == null)
			FITS_HeaderInfo.current_PROPID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_PROPID);

		if (FITS_HeaderInfo.current_GROUPID.getValue() == null)
			FITS_HeaderInfo.current_GROUPID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_GROUPID);

		if (FITS_HeaderInfo.current_OBSID.getValue() == null)
			FITS_HeaderInfo.current_OBSID.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_OBSID);

		list.add(FITS_HeaderInfo.current_GRPTIMNG);
		list.add(FITS_HeaderInfo.current_GRPUID);
		list.add(FITS_HeaderInfo.current_GRPMONP);
		list.add(FITS_HeaderInfo.current_GRPNUMOB);
		list.add(FITS_HeaderInfo.current_GRPSEECO);
		//list.add(FITS_HeaderInfo.current_GRPLUNCO);
		//list.add(FITS_HeaderInfo.current_GRPMLDCO);
		//list.add(FITS_HeaderInfo.current_GRPSOLCO);
		list.add(FITS_HeaderInfo.current_GRPSKYCO);
		list.add(FITS_HeaderInfo.current_GRPEXTCO);
		list.add(FITS_HeaderInfo.current_GRPAIRCO);
		list.add(FITS_HeaderInfo.current_GRPMINHA);
		list.add(FITS_HeaderInfo.current_GRPMAXHA);
		
		list.add(FITS_HeaderInfo.current_GRPEDATE);
		list.add(FITS_HeaderInfo.current_GRPNOMEX);
		list.add(FITS_HeaderInfo.current_USRDEFOC);
		
		// This is used for data compression. get AMATEUR from TAG ?
		//list.add(FITS_HeaderInfo.current_COMPRESS);

		list.add(FITS_HeaderInfo.current_LATITUDE);
		list.add(FITS_HeaderInfo.current_LONGITUD);

		// Source
		list.add(FITS_HeaderInfo.current_RA);
		list.add(FITS_HeaderInfo.current_DEC);
		// list.add(FITS_HeaderInfo.current_APP_RA);
		// list.add(FITS_HeaderInfo.current_APP_DEC);
		list.add(FITS_HeaderInfo.current_RADECSYS);
		list.add(FITS_HeaderInfo.current_EQUINOX);
		list.add(FITS_HeaderInfo.current_LST);
		// These are from the source.
		list.add(FITS_HeaderInfo.current_CAT_RA);
		list.add(FITS_HeaderInfo.current_CAT_DEC);
		list.add(FITS_HeaderInfo.current_CAT_EQUI);
		list.add(FITS_HeaderInfo.current_CAT_EPOC);
		list.add(FITS_HeaderInfo.current_CAT_NAME);
		list.add(FITS_HeaderInfo.current_OBJECT);
		list.add(FITS_HeaderInfo.current_SRCTYPE);
		// Some of these apply to Fixed sources others to moving.
		list.add(FITS_HeaderInfo.current_PM_RA);
		list.add(FITS_HeaderInfo.current_PM_DEC);
		list.add(FITS_HeaderInfo.current_PARALLAX);
		list.add(FITS_HeaderInfo.current_RADVEL);
		list.add(FITS_HeaderInfo.current_RATRACK);
		list.add(FITS_HeaderInfo.current_DECTRACK);

		list.add(FITS_HeaderInfo.current_NETSTATE);
		list.add(FITS_HeaderInfo.current_ENGSTATE);
		list.add(FITS_HeaderInfo.current_TELSTATE);
		list.add(FITS_HeaderInfo.current_TCSSTATE);
		list.add(FITS_HeaderInfo.current_PWRESTRT);
		list.add(FITS_HeaderInfo.current_PWSHUTDN);

		list.add(FITS_HeaderInfo.current_AZDMD);
		list.add(FITS_HeaderInfo.current_AZPOS);
		list.add(FITS_HeaderInfo.current_AZSTAT);

		list.add(FITS_HeaderInfo.current_ALTDMD);
		list.add(FITS_HeaderInfo.current_ALTPOS);
		list.add(FITS_HeaderInfo.current_ALTSTAT);
		list.add(FITS_HeaderInfo.current_AIRMASS);

		list.add(FITS_HeaderInfo.current_ROTDMD);
		list.add(FITS_HeaderInfo.current_ROTPOS);
		list.add(FITS_HeaderInfo.current_ROTMODE);
		list.add(FITS_HeaderInfo.current_ROTSKYPA);
		list.add(FITS_HeaderInfo.current_ROTSTAT);

		list.add(FITS_HeaderInfo.current_ENC1DMD);
		list.add(FITS_HeaderInfo.current_ENC1POS);
		list.add(FITS_HeaderInfo.current_ENC1STAT);

		list.add(FITS_HeaderInfo.current_ENC2DMD);
		list.add(FITS_HeaderInfo.current_ENC2POS);
		list.add(FITS_HeaderInfo.current_ENC2STAT);

		list.add(FITS_HeaderInfo.current_FOLDDMD);
		list.add(FITS_HeaderInfo.current_FOLDPOS);
		list.add(FITS_HeaderInfo.current_FOLDSTAT);

		list.add(FITS_HeaderInfo.current_PMCDMD);
		list.add(FITS_HeaderInfo.current_PMCPOS);
		list.add(FITS_HeaderInfo.current_PMCSTAT);

		list.add(FITS_HeaderInfo.current_FOCDMD);
		list.add(FITS_HeaderInfo.current_TELFOCUS);
		list.add(FITS_HeaderInfo.current_DFOCUS);
		list.add(FITS_HeaderInfo.current_FOCSTAT);
		list.add(FITS_HeaderInfo.current_MIRSYSST);

		list.add(FITS_HeaderInfo.current_WMSSTAT);
		list.add(FITS_HeaderInfo.current_WMSRAIN);
		list.add(FITS_HeaderInfo.current_WMSMOIST);

		list.add(FITS_HeaderInfo.current_TEMPTUBE);

		list.add(FITS_HeaderInfo.current_WMOILTMP);
		list.add(FITS_HeaderInfo.current_WMSPMT);
		list.add(FITS_HeaderInfo.current_WMFOCTMP);
		list.add(FITS_HeaderInfo.current_WMAGBTMP);

		list.add(FITS_HeaderInfo.current_WMSTEMP);
		list.add(FITS_HeaderInfo.current_WMSDEWPT);

		list.add(FITS_HeaderInfo.current_WINDSPEE);
		list.add(FITS_HeaderInfo.current_WMSPRES);
		list.add(FITS_HeaderInfo.current_WMSHUMID);
		list.add(FITS_HeaderInfo.current_WINDDIR);
		list.add(FITS_HeaderInfo.current_CLOUD);

		list.add(FITS_HeaderInfo.current_REFPRES);

		list.add(FITS_HeaderInfo.current_REFTEMP);

		list.add(FITS_HeaderInfo.current_REFHUMID);

		list.add(FITS_HeaderInfo.current_AUTOGUID);
		list.add(FITS_HeaderInfo.current_AGSTATE);
		list.add(FITS_HeaderInfo.current_AGMODE);
		list.add(FITS_HeaderInfo.current_AGGMAG);
		list.add(FITS_HeaderInfo.current_AGFWHM);
		list.add(FITS_HeaderInfo.current_AGMIRDMD);
		list.add(FITS_HeaderInfo.current_AGMIRPOS);
		list.add(FITS_HeaderInfo.current_AGMIRST);
		list.add(FITS_HeaderInfo.current_AGFOCDMD);
		list.add(FITS_HeaderInfo.current_AGFOCUS);
		list.add(FITS_HeaderInfo.current_AGFOCST);
		list.add(FITS_HeaderInfo.current_AGFILDMD);
		list.add(FITS_HeaderInfo.current_AGFILPOS);
		list.add(FITS_HeaderInfo.current_AGFILST);

		// Sky.
		list.add(FITS_HeaderInfo.current_SCHEDSEE);
		list.add(FITS_HeaderInfo.current_SCHEDPHT);
		list.add(FITS_HeaderInfo.current_SCHEDSKY);
		list.add(FITS_HeaderInfo.current_ESTSEE);

		// Moon.
		//list.add(FITS_HeaderInfo.current_MOONSTAT);
		list.add(FITS_HeaderInfo.current_MOONFRAC);
		list.add(FITS_HeaderInfo.current_MOONDIST);
		list.add(FITS_HeaderInfo.current_MOONALT);

		// Sun
		list.add(FITS_HeaderInfo.current_SUNALT);

		// Misc
		if (FITS_HeaderInfo.current_ACQIMG.getValue() == null)
			FITS_HeaderInfo.current_ACQIMG.setValue("UNKNOWN");
		list.add(FITS_HeaderInfo.current_ACQIMG);

		list.add(FITS_HeaderInfo.current_ACQMODE);
		list.add(FITS_HeaderInfo.current_ACQXPIX);
		list.add(FITS_HeaderInfo.current_ACQYPIX);
		list.add(FITS_HeaderInfo.current_ACQINST);

		list.add(FITS_HeaderInfo.current_BFOCCTRL);
						
		// issLog.log(2, CLASS, "-", "processReceivedCommand",
		// "FITS Headers set to: "
		// + FITS_HeaderInfo.getInstance().toString());

		// maybe log the headers we are returning here...
		if (System.getProperty("log.fits.headers") != null) {
			Iterator ifits = list.iterator();
			while (ifits.hasNext()) {
				FitsHeaderCardImage fits = (FitsHeaderCardImage) ifits.next();
				issLog.log(1, CLASS, "-", "processReceivedCommand", "GET_FITS [" + fits.toString() + "]");
			}
		}

	}

	@Override
	public long calculateTimeToComplete() {
		return 40000L;
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

		GET_FITS_DONE done = new GET_FITS_DONE(receivedCommand.getId());
		done.setSuccessful(true);
		done.setErrorNum(0);
		done.setErrorString("");
		done.setFitsHeader(list);

		return done;
	}

	@Override
	public COMMAND_DONE translateResponse(COMMAND_DONE response) {
		super.translateResponse(response);
		return response;
	}

}

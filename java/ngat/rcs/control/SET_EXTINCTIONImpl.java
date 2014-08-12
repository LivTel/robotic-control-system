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
package ngat.rcs.control;

import ngat.rcs.*;

import ngat.ems.*;
import ngat.net.*;
import ngat.message.GUI_RCS.*;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Tries to set the seeing used by the OSS.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SET_EXTINCTIONImpl.java,v 1.2 2008/05/27 13:31:52 eng Exp eng $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/control/RCS/SET_EXTINCTIONImpl.java,v $
 * </dl>
 * 
 * @author $Author: eng $
 * @version $Revision: 1.2 $
 */

public class SET_EXTINCTIONImpl extends CtrlCommandImpl {

    public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


	public SET_EXTINCTIONImpl(IConnection connection, GUI_TO_RCS command) {
		super(connection, command);
		sdf.setTimeZone(UTC);
	}

	public void handleRequest() {

		SET_EXTINCTION set = (SET_EXTINCTION) command;

		SET_EXTINCTION_DONE done = new SET_EXTINCTION_DONE(command.getId());

		int ext = set.getExtinction();
		double extVal = 1.0; // 1=BAD, 0=good
		String extStr = "SPECTRO";

		switch (ext) {
		case SET_EXTINCTION.SPECTROSCOPIC:
			extVal = 1.0;
			extStr = "SPECTRO";
			break;
		case SET_EXTINCTION.PHOTOMETRIC:
			extVal = 0.0;
			extStr = "PHOTOM";
			break;
		}

		try {
		File file = new File("/occ/data/photom.dat");
		PrintWriter pout = new PrintWriter(new FileWriter(file));

		// e.g. 2014-01-28T16:24:00 PHOTOM 0.0
		pout.println(sdf.format(new Date())+" "+extStr+" "+extVal);
		pout.close();
		pout = null;
		file = null;
		} catch (Exception e) {
		    e.printStackTrace();
		}
		MutableSkyModel skyModel = (MutableSkyModel) RCS_Controller.controller.getSkyModel();
		try {
			skyModel.updateExtinction(extVal, 700.0, 0.5 * Math.PI, 0.0, System.currentTimeMillis(), true);
			done.setSuccessful(true);
		} catch (Exception e) {
			e.printStackTrace();
			done.setSuccessful(false);
		}

		sendDone(done);

	}

}

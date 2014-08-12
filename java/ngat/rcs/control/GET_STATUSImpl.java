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

import ngat.rcs.emm.*;

import ngat.util.*;
import ngat.net.*;
import ngat.message.GUI_RCS.*;

/**
 * Handles the GET_STATUS ctrl command.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: GET_STATUSImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/control/RCS/GET_STATUSImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class GET_STATUSImpl extends CtrlCommandImpl {

	public GET_STATUSImpl(IConnection connection, GUI_TO_RCS command) {
		super(connection, command);
	}

	public void handleRequest() {

		GET_STATUS get = (GET_STATUS) command;

		GET_STATUS_DONE done = new GET_STATUS_DONE(command.getId());

		LegacyStatusProviderRegistry emm = LegacyStatusProviderRegistry
				.getInstance();
		if (emm == null) {
			sendError(done, GET_STATUS.NOT_AVAILABLE, "EMM Registry not found");
			return;
		}

		String cat = get.getCategory();

		if (cat == null) {
			sendError(done, GET_STATUS.UNKNOWN_CATEGORY, "No category supplied");
			return;
		}

		StatusCategory status = null;
		try {
			status = emm.getStatusCategory(cat);
			// System.err.println("GET_STATUS: "+cat+" : "+status);
		} catch (IllegalArgumentException iax) {
			iax.printStackTrace();
			sendError(done, GET_STATUS.UNKNOWN_CATEGORY, "No such category: "
					+ cat);
			return;
		}

		done.setSuccessful(true);
		done.setStatus(status);
		sendDone(done);

	}

}

/**
 * $Log: GET_STATUSImpl.java,v $ /** Revision 1.1 2006/12/12 08:26:29 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:33:59 snf /** Initial
 * revision /**
 */

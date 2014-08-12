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
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

import java.io.*;
import java.util.*;

/** Sets ISS FITS headers during manual operations.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: ISS_SET_HEADERSImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/ISS_SET_HEADERSImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class ISS_SET_HEADERSImpl extends CtrlCommandImpl {

    public ISS_SET_HEADERSImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	ISS_SET_HEADERS sethdrs = (ISS_SET_HEADERS)command;

	ISS_SET_HEADERS_DONE done = new ISS_SET_HEADERS_DONE(command.getId());

	if (RCS_Controller.controller.isOperational()) {
	    sendError(done, ISS_SET_HEADERS.NOT_MANUAL_MODE, 
		      "Cannot modify FITS header mode whilst RCS is OPERATIONAL");
	    return;
	}

	boolean manual = sethdrs.getManual();
	if (manual) {	   
	    FITS_HeaderInfo.current_TELMODE.setValue("MANUAL");
	    FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_MANUAL);
	    
	    String tagName = sethdrs.getTagId();
	    if (tagName != null)
		FITS_HeaderInfo.current_TAGID.setValue(tagName);
	    
	    String userName = sethdrs.getUserId();
	    if (userName != null)
		FITS_HeaderInfo.current_USERID.setValue(userName);
	    
	    String propName = sethdrs.getProposalId();
	    if (propName != null)
		FITS_HeaderInfo.current_PROPID.setValue(propName);
	    
	    String groupName = sethdrs.getGroupId();
	    if (groupName != null)
		FITS_HeaderInfo.current_GROUPID.setValue(groupName);
	    
	    String obsName = sethdrs.getObsId();
	    if (obsName != null)
		FITS_HeaderInfo.current_OBSID.setValue(obsName);
	    
	} else {
	    FITS_HeaderInfo.setTelMode(FITS_HeaderInfo.TELMODE_AUTOMATIC);
	}
	
	done.setSuccessful(true);
	sendDone(done);

    }

}

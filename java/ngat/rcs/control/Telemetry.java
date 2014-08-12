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

import java.io.*;
import java.util.*;

import ngat.net.*;
import ngat.net.datalogger.DataCampSender;
import ngat.net.datalogger.DataForwarder;
import ngat.net.datalogger.DataLogger;
import ngat.util.*;
import ngat.message.GUI_RCS.*;

/** Implements the Telemetry mechanism for sending information back to remote clients.
 *  Mainly used by the TEA and may well be deprecated in practice
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: Telemetry.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/Telemetry.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
*/

public class Telemetry {

    protected static Telemetry instance;

    protected DataLogger dataLogger;

    /** The list of clients who want telemtry data.*/
    protected List targets;

    private Telemetry() { 
	
	targets = new Vector();

	dataLogger = new DataLogger();

	dataLogger.start();
	
    }

    public static Telemetry getInstance() {
	if (instance == null)
	    instance = new Telemetry();
	return instance;
    }

    /** Add this client. If the ID is already known we dont add it.*/
    public void addConnection(String clientId, ConnectionSetupInfo conset, Vector wants) throws IOException {
     
	if (targets.contains(clientId)) 
	    return;

	switch (conset.type) {
	case ConnectionSetupInfo.UDP:
	    DataForwarder df = new DataForwarder(wants, conset.host, conset.port);	
	    dataLogger.addUpdateListener(df);	
	    targets.add(clientId);
	    break;
	case ConnectionSetupInfo.CAMP:
	    DataCampSender ds = new DataCampSender(wants, conset.host, conset.port, 10000L);
	    dataLogger.addUpdateListener(ds);	
	    targets.add(clientId);
	    break;
	default:
	    // We dont handle other protocols yet.
	}
    }

    public void publish(String cat, TelemetryInfo info) {

	try {
	    dataLogger.push(info);
	} catch (InterruptedException ix) {}
	
    }

}

/** $Log: Telemetry.java,v $
/** Revision 1.1  2006/12/12 08:26:29  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:59  snf
/** Initial revision
/** */

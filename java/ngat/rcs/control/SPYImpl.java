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

/** Tries to attach a Spy to a watchable object.
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: SPYImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/SPYImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SPYImpl extends CtrlCommandImpl {

    public SPYImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	SPY spy = (SPY)command;

	SPY_DONE done = new SPY_DONE(command.getId());

	int category = spy.getCategory();

	String target = spy.getTarget();

	switch (category) {

	case SPY.SENSOR:
	    Sensor sensor = SensorsXX.getSensor(target);  
	    if (sensor == null) {
		done.setErrorNum(SPY.UNKNOWN_TARGET);
		done.setSuccessful(false);
		done.setErrorString("Unknown sensor: "+target);
	    } else {
		sensor.setSpy(true);
		done.setSuccessful(true);
	    }
	    break;
	case SPY.FILTER:
	    Filter filter = FiltersXX.getFilter(target);
	    if (filter == null) {
		done.setErrorNum(SPY.UNKNOWN_TARGET);
		done.setErrorString("Unknown filter: "+target);
		done.setSuccessful(false);	
	    } else {
		filter.setSpy(true);
		done.setSuccessful(true);
	    }
	    break;
	case SPY.RULE:
	    Rule rule = MonitorsXX.getRule(target);
	    if (rule == null) {
		done.setErrorNum(SPY.UNKNOWN_TARGET);
		done.setSuccessful(false);
		done.setErrorString("Unknown rule: "+target);
	    } else {	
		rule.setSpy(true);
		done.setSuccessful(true);
	    }
	    break;
	case SPY.MONITOR:
	case SPY.RULESET:
	    done.setErrorNum(SPY.UNKNOWN_CATEGORY);
	    done.setSuccessful(false);
	    done.setErrorString("Unknown category ["+category+"] or not yet available");
	    break;
	default:
	    done.setErrorNum(SPY.UNKNOWN_CATEGORY);
	    done.setSuccessful(false);
	    done.setErrorString("Unknown category ["+category+"].");
	}

	sendDone(done);

    }

}

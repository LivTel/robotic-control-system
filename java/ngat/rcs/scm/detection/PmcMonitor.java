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
package ngat.rcs.scm.detection;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.util.*;

import ngat.message.RCS_TCS.*;

/** Monitors PMC state and notifies when closed.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: DefaultAutoguiderMonitor.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/DefaultAutoguiderMonitor.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class PmcMonitor implements Observer {
    
    protected Object lock = new Object();
   
    protected Vector listeners;

    long startTime;
    long timeStamp;
    long updateTimeStamp;

    boolean pmcIsClosed = false;
    int     pmcStatus;
 
    boolean enableAlerts = false;

    public PmcMonitor() {
	listeners = new Vector();
    }

    /** Sets whether to generate alerts on lost guiding.*/
    public void setEnableAlerts(boolean enable) {
	this.enableAlerts = enable;
	System.err.println("PMCMon:: Alerts are now "+(enableAlerts ? "ENABLED" : "DISABLED"));
    }
   
    /** Implementation of the java.util.Observer interface to handle notifications
     * from the StatusPool on status update. 
     */
    public void update(Observable source, Object args) {

	if ( ! (args instanceof TCS_Status) ) return;

	synchronized (lock) {
	 
	    TCS_Status status = (TCS_Status)args;
	       
	    updateTimeStamp = status.mechanisms.timeStamp;

	     // Monitoring rotator tracking status.
	    pmcStatus =  status.mechanisms.primMirrorCoverPos;
	    	    
	    if (pmcStatus != TCS_Status.POSITION_OPEN) {
	
		if (enableAlerts) {
		    EventQueue.postEvent("X_PMC_CLOSED");
		    
		    int ia = 0;
		    Iterator aslist = listeners.iterator();
		    while (aslist.hasNext()) {			
			PmcStatusListener asl = (PmcStatusListener)aslist.next();	
			System.err.println("PMCMon::PMC CLosed Notification to PSL["+(++ia)+"] "+asl);
			asl.pmcClosed();
		    }
		    
		    // Disable events after firing once
		    setEnableAlerts(false);
		}
	    
	    } else {

		pmcIsClosed = false;
		

	    }
	   	  
	    timeStamp = updateTimeStamp ;

	}

    }

    /** Warning - alerts are now automatically enabled on reset ! */
    public void reset() {
	
	startTime = System.currentTimeMillis();
	timeStamp = startTime;
	
	pmcIsClosed = false;
	setEnableAlerts(true);

    }

    public void addPmcStatusListener(PmcStatusListener asl) {
	if (!listeners.contains(asl)) {
	    listeners.add(asl);
	    System.err.println("PMCMon:: Added PmcStatusListener: "+asl);
	}
    }

    public void removePmcStatusListener(PmcStatusListener asl) {
	System.err.println("PMCMon:: Requested to remove PmcStatusListener: "+asl);
	if (!listeners.contains(asl))
	    return;
	listeners.remove(asl);
	System.err.println("PMCMon:: Removed PmcStatusListener: "+asl);
    }


}

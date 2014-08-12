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

import ngat.util.logging.*;

/** 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * <br>
 * $Id: Monitor.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class Monitor implements Logging {

    /** Counts the number of Monitors currently created.*/
    protected static int count = 0;
    
    /** Records an identifier for this Monitor - may be used in log messages.*/
    protected String id;

    /** The sets of Rule/Event associations to be invoked by this Monitor.*/
    protected Map ruleMap;    
    
    /** The (max) period between invokations of the Monitor's Ruleset.
     * A Monitor may be triggered more often if new data becomes available 
     * for its various sensors.*/
    protected long period;

    /** Records the last time this Monitor was triggered.*/
    protected long latestTrigger;

    /** Logger used by this class.*/
    protected Logger logger;

    /** Set to indicate that this Monitor's detailed operations are to be logged.*/
    protected boolean spy;

    /** Detailed operations 'spy' logger.*/
    protected Logger spyLog;

    /** Create a Monitor using the specified Ruleset and which posts
     * the specified EventID.
   
     */
    public Monitor(String id) {
	this.id = id;
	ruleMap = Collections.synchronizedMap(new HashMap());
	count++;
	logger = LogManager.getLogger("MONITOR");
    }
    
    /** Create a Monitor with a default id.*/
    public Monitor() {
	this("Monitor:"+count);
    }

    /** Associate a Ruleset with an event code.
     * @param ruleset The set of Rules to use.
     * @param eventId The id as specified in EventID which will be 
     * posted if the Ruleset triggers when invoked.
     */
    public void associateRuleset(Ruleset ruleset, String eventId) {
	ruleMap.put(ruleset, eventId);
    }
    
    /** Cause the Monitor to take readings from its Sensors and 
     * invoke the Rules contained in its Ruleset to decide if the
     * attached Event should be fired off. i.e. posted to the
     * EventQueue. The variable OkToTrigger is tested and this
     * method return silently if it is NOT set.
     */
    public void trigger() {
	if ( ! okToTrigger()) {
	   return;
	}

	if (spy)
	    spyLog.log(3, "Monitor", id, "trigger", " TRIGGERED");
	latestTrigger = System.currentTimeMillis();
	Ruleset ruleset = null;
	Iterator it = ruleMap.keySet().iterator();
	while (it.hasNext()) {
	    ruleset = (Ruleset)it.next();
	    if (ruleset.invoke()) {
		String eventId = (String)ruleMap.get(ruleset);
		if (spy)
		    spyLog.log(2, "Monitor", id, "trigger"," Posted Event: "+eventId);
		EventQueue.postEvent(eventId, "");
	    }
	}
    }
    
    
    /** Sets the identity for this Monitor.
     * @param id The name/id of this Monitor.*/
    public void   setId(String id) { this.id = id; }

    /* @return The name/id of this Monitor.*/
    public String getId() { return id; }

    /** Sets the trigger interval.*/
    public void setPeriod(long period) { this.period = period; }

    /** Checks whether this Monitor is allowed to trigger.
     * True if <i>now - lastTrigger > 0.5 * period</i> .
     */
    protected boolean okToTrigger() { 
	boolean ok = ((System.currentTimeMillis() - latestTrigger) > 0.5*period);
	if (spy)
	    spyLog.log(3, "Monitor", id, "oKToTrigger", (ok ? "" : "NOT")+ "TRIGGERED");
	return ok;
    }


    /** @return The number of Monitors created so far.*/
    public static int getCount() { return count; }
    
    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }

    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName) { spyLog = LogManager.getLogger(spyLogName); }

}

/** $Log: Monitor.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.3  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */

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
package ngat.rcs.scm.collation;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
/** Carries out the TCS status monitoring task. Sends regular
 * (configurable-parameter) SHOW requests to the TCS to gather
 * the latest status data and place in the globally accessable
 * status pool.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SMM_MonitorThread.java,v 1.2 2007/01/05 11:08:49 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/SMM_MonitorThread.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class SMM_MonitorThread extends ControlThread {
    
    /** Default value for status request interval (5 secs).*/
    public static final long DEFAULT_INTERVAL = 5000L;

    /** Default value for status request interval (10 secs).*/
    public static final long DEFAULT_TIMEOUT  = 10000L;
    
    /** Multiplication factor for interval backoff.*/
    public static final double  BACKOFF_FACTOR = 1.5;
    
    /** Longest interval to support for backoff (2 minutes).*/
    public static final long BACKOFF_MAX_INTERVAL = 60000L;
    
    /** Table containing the various StatusMonitor instances.*/
    protected static Map monitors;

    /** True if this MonitorThread is allowed to start.*/
    protected boolean enabled;

    /** The interval (msecs) between sending status requests
     * to the TCS.*/
    protected long interval;

    /** Value saved at start of backoff.*/
    protected long normalInterval;

    /** The timeout interval for the client.*/
    protected long clientTimeout;

    /** Value saved at start of backoff.*/
    protected long normalTimeout;

    /** If true, indicates that this monitor is being backed-off - i.e.
     * timeouts and intervals are progressively being increased due to
     * a network communications problem. If this was not performed, the
     * SMM could be generating clientsa faster than old ones are timing-out
     * and disposing.*/
    protected boolean backoff;

    /** The key (one of the constants of the RC_TCS.SHOW command) to
     * indicate which fields we require to obtain.*/
    protected int key;

    /** Reference to the currently executing Client instance.*/
    protected SMM_MonitorClient client;
    
    /** Create a SMM_MonitorThread with specified interval.
     * Runs repeatedly - i.e. PERMANENT.*/
    protected SMM_MonitorThread(int key, long interval) {
	super("StatusMonitor:"+key, true);
	this.interval      = interval;	 
	this.clientTimeout = interval/2;
	this.key           = key;	

	normalInterval = interval;
	normalTimeout  = clientTimeout;
    }
    
    /** Performs initialization operations (?WHAT?).*/
    @Override
	public void initialise() {}
    
    /** Create SM_Clients and sleep for interval.*/
    @Override
	public void mainTask() {
	
	if (backoff) {
	    interval      = Math.min((long)(BACKOFF_FACTOR*interval), BACKOFF_MAX_INTERVAL);
	    clientTimeout = interval/2;
	}

	try { Thread.sleep(interval);} catch (InterruptedException e){}

	SMM_MonitorClient client = new SMM_MonitorClient(getName(), key); 
	client.setTimeout(clientTimeout);
	client.exec();
	
    }

    /** Performs shutdown operations (?WHAT?).*/    
    @Override
	public void shutdown() {}

    /** Single shot status request. The Client returned can be used in a join
     * to allow the caller to block until it has completed / timed-out. The
     * Client's waitFor(timeout) can be used to effect this. In particular -
     * when multiple request are to be sent concurrently a structure like this
     * could be used to effect the operation:-
     * <pre>
     *  long aWhile = 10*1000L; // 10 secs.
     *
     *  // Grab the specific monitors.
     *  SMM_MonitorThread t_State = SMM_MonitorThread.getInstance(SHOW.STATE).
     *  SMM_MonitorThread t_Astro = SMM_MonitorThread.getInstance(SHOW.ASTROMETRY).
     *  SMM_MonitorThread t_Mechs = SMM_MonitorThread.getInstance(SHOW.MECHANISMS).
     *
     *  // By the time these methods return the requests may already be sent.
     *  SMM_MonitorClient c_State = t_State.requestStatus(); 
     *  SMM_MonitorClient c_Astro = t_Astro.requestStatus();
     *  SMM_MonitorClient c_Mechs = t_Mechs.requestStatus();
     *  try {
     *      c_State.waitFor(aWhile);
     *      c_Astro.waitFor(aWhile);
     *      c_Mechs.waitFor(aWhile);
     *  } catch (InterruptedException ix){}
     * </pre>
     * This structure allows several (3) single-shot requests to be sent concurrently 
     * and the caller blocks until either the last of these completes or the 10 second
     * timeout occurs. On completion they will have set the StatusPool accordingly.
     * The caller may now query the StatusPool for these latest values, if required
     * though the StatusPool will have updated any Observers by now if that was what
     * was required.
     *
     **/
    public SMM_MonitorClient requestStatus() {
	SMM_MonitorClient client = new SMM_MonitorClient("single-shot:"+key, key);
	client.exec();
	return client;
    }
   
    /** Set true if this MonitorThread is allowed to start.*/
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Return True if this MonitorThread is allowed to start.*/
    public boolean isEnabled(){ return enabled; }

    /** Sets the interval between status requests.
     * @param interval The interval (msecs).*/
    public void setInterval(long interval) { this.interval = interval; }

    /** @return The interval between status requests.*/
    public long getInterval() { return interval; }
    
    /** Sets the client's timeout (DO NOT make this more than a few times 
     * greater than the interval between requests as this will cause clients
     * to hang about using up thread resources if the TCS dies).
     * @param timeout The timeout interval for the client.*/
    public void setClientTimeout(long timeout) { clientTimeout = timeout;}

    /** @return The current setting of the client timeout interval.*/
    public long getClientTimeout() { return clientTimeout; }

    /** Returns true if this Thread is backing off.*/
    public boolean isBackingOff() { return backoff; }

    /** Called to allow this Thread to backoff by a factor before next request sent
     * due typically to network problems. Resume using normal() method call. 
     * This method saves the current interval and timeout parameters for restoration.
     * After calling for first time, subsequent calls have no effect - the backoff is
     * progressive until a maximum interval is reached.
     */
    public void backoff() {
	if (backoff) return;
	backoff = true;
	normalInterval = interval;
	normalTimeout  = clientTimeout;

	interval      = (long)(BACKOFF_FACTOR*interval);
	clientTimeout = interval/2;
    }

    /** Restore intervals and timeouts to normal (as before backoff).*/
    public void normal() {
	backoff = false;
	interval = normalInterval;
	clientTimeout = interval/2;
    }

}

/** $Log: SMM_MonitorThread.java,v $
/** Revision 1.2  2007/01/05 11:08:49  snf
/** Per instance defined client, problem with requestStatus() usurping the running client.
/**
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/**
/** Revision 1.5  2001/06/08 16:27:27  snf
/** Added telfocus trapping info.
/**
/** Revision 1.4  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.3  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.2  2000/12/22 14:40:37  snf
/** Backup.
/**
/** Revision 1.1  2000/12/20 10:26:09  snf
/** Initial revision
/** */

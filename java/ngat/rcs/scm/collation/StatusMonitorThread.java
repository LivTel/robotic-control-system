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

import ngat.rcs.control.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.GUI_RCS.*;

/** Carries out status monitoring task. Sends regular
 * (configurable-parameters) requests to the TCS, ICS or elsewhere
 * to gather the latest status data.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: StatusMonitorThread.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/StatusMonitorThread.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class StatusMonitorThread extends ControlThread {
    
    /** Default value for status request interval (5 sec).*/
    public static final long DEFAULT_POLLING_INTERVAL = 5000L;

    /** Default value for status request timeout (30 sec).*/
    public static final long DEFAULT_TIMEOUT = 30000L;

    /** The interval (msecs) between making status requests.*/
    protected long pollingInterval;

    /** Reference to the currently executing Client instance.*/
    protected StatusMonitorClient client;
 
    /** Network status category for this SMT's clients.*/
    protected NetworkStatus netStatus;

    /** Logs data to a file or whatever.*/
    protected StatusLogger statusLogger;

    /** Description of this Monitor thread.*/
    String description;

    /** True if this monitor is enabled.*/
    boolean enabled;

    /** True if we are able to log.*/
    boolean logging;

    /** True if the status has <i>just</i> been updated. The method call
     * updated() returns true if so and resets the flag to false.
     * The method markUpdated() sets the flag true whatever.
     * @see updated(), markUpdate().
     */
    protected volatile boolean updated;
   
    /** Create a StatusMonitorThread with specified parameters.
     * @param name       The name of this monitor thread.
     */
    public StatusMonitorThread(String name) {
	super("STATUS_MONITOR:"+name, true);		
    }
   
    /** Initialization.*/
    @Override
	public void initialise() {
	if (statusLogger != null) {
	    try {
		statusLogger.open();
		logging = true;
	    } catch (Exception e) {
		e.printStackTrace();
		logging = false;
	    }
	}
    }
    
    /** Each polling interval: (Re)-initialize the client then request status.
     * If the client itself uses a seperate thread for communication then we 
     * need to wait on this thread - e.g. for JMSMAClient we can call the
     * JMSMAClient.waitFor(timeout) method to stall the calling thread. We
     * dont however know what sort of client we have so can't assume this.
     */
    @Override
	public void mainTask() {

	try { Thread.sleep(pollingInterval);} catch (InterruptedException e){}
	
	if (! enabled) return;

	// fall out if no client is set...
	if (client == null)
	    return;

	try {
	    client.initClient();
	} catch (ClientInitializationException cx) {
	    cx.printStackTrace();
	    return;
	}

	// Blocking operation - otherwise we end up potentially creating huge numbers
	// of clients/threads if (timeout > polling interval).
	client.clientGetStatus();

	if (client.isStatusValid()) {
	    markUpdate();
	    //System.err.println("StatusMonitorThread: "+getName()+" Updating data validity: "+client.isStatusValid());
	    
	    // ## A good place to broadcast the status info to listening guis etc.
	    // ## and we should log it locally as well.
	    StatusCategory status = client.getStatus();
	    if (status != null) {
		long now = System.currentTimeMillis();
		Telemetry.getInstance().publish("STATUS", new StatusInfo(now, client.getName(), status ) );
		if (statusLogger != null && logging) {
		    try {
			statusLogger.publish(status);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    
		}

		//System.err.println("StatusMonitorThread: "+getName()+" Here is where I will notify the StatusUpdateListeners");

	    }
	} else {
	    // We may want to put some sort of backoff in here later.

	}

	if (client.isNetworkAvailable()) {
	    netStatus.update(client.getStatus().getTimeStamp(),
			     NetworkStatus.NETWORK_ONLINE);
	} else {
	    netStatus.update(client.getNetworkTimestamp(),
			     NetworkStatus.NETWORK_OFFLINE); 
	    //System.err.println("SMT:: "+getName()+" Updating network status: "+client.isNetworkAvailable());	     
	}
	//System.err.println("StatusMonitorThread: "+getName()+" Here is where I will notify statusListeners about network status");
	
    }
    
    /** Release resources.*/
    @Override
	public void shutdown() {
	if (statusLogger != null) {
		
	    try {
		statusLogger.close();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} 
	client = null;
    }
    
    /** Set the client.
     * @param client A StatusMonitorClient to get the status details.
     */
    public void setClient(StatusMonitorClient client) { this.client = client; }
    
    /** Set the network status category.
     * @param netStatus The NetworkStatus to which this SMT's client uses for communications.
     */
    public void setNetworkStatus(NetworkStatus netStatus) { this.netStatus = netStatus; }

    /** Set the Datalogger to use with this SMT.
     * @param datalogger The Datalogger to use for logging data.
     */
    public void setStatusLogger(StatusLogger statusLogger) { this.statusLogger = statusLogger; }
    
    /** Sets the interval between status requests.
     * @param pollingInterval The interval (msecs).
     */
    public void setPollingInterval(long pollingInterval) { this.pollingInterval = pollingInterval; }

    /** @return The interval between status requests.*/
    public long getPollingInterval() { return pollingInterval; }

    /** Set True to enable this monitor thread.*/
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Returns True if this monitor thread is enabled.*/
    public boolean isEnabled() { return enabled; }

    /** Sets the description of this monitor thread.*/
    public void setDescription(String description) { this.description = description;}

    /** Returns the description of this monitor thread.*/
    public String getDescription() { return description;}

    /** Sets the updated flag whatever.*/
    public synchronized void markUpdate() {
	updated = true;
    }

    /** If updated is set returns true and resets it.*/
    public synchronized boolean updated() {
	boolean yes = updated;
	if (yes) {
	    updated = false;
	    return true;
	} 
	return false;
    }

    
}

/** $Log: StatusMonitorThread.java,v $
    /** Revision 1.1  2006/12/12 08:30:52  snf
    /** Initial revision
    /**
    /** Revision 1.1  2006/05/17 06:34:57  snf
    /** Initial revision
    /**
    /** Revision 1.6  2004/02/02 15:39:07  snf
    /** Monitors a selected status collection channel.
    /** */

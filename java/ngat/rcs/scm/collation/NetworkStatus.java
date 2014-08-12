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

import java.io.*;
import java.text.*;
import java.util.*;

import ngat.util.*;


/** Holds information relating to the status of a network or other 
 * resource used in obtaining status information.
 *
 * e.g. [ TCS -> CIL/UDP, CCS -> TCP Sockets, URL -> file or web access ]
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: NetworkStatus.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/NetworkStatus.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class NetworkStatus implements StatusCategory, UpdateableStatus, Serializable {
 
    /** Date formatter.*/
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss z");
    
    /** State: Network comms are online.*/
    public static final int NETWORK_ONLINE = 1;
    
     /** State: Network comms are offline.*/
    public static final int NETWORK_OFFLINE = 2;

    /** Records the time this Status was generated.*/
    protected long timeStamp;

    /** Records the current network state.*/
    protected int networkState;

    /** Name to identify this network-status.*/
    protected String name;

    /** Create a NetworkState with the supplied name.*/
    public NetworkStatus(String name) {
	this.name = name;
    }
	
    /** Set the network-state.*/
    public void setState(int networkState) {
	this.networkState = networkState;
    }

    /** Update status with supplied timestamp.*/
    public void update(long timeStamp, int networkState) {
	this.timeStamp    = timeStamp;
	this.networkState = networkState;
    }
    
    /** Update status with current timestamp.*/
    public void update(int networkState) {
	this.timeStamp    = System.currentTimeMillis();
	this.networkState = networkState;
    }

    /** Update with new values. This an override of the UpdateableStatus impl.*/
    public void update(NetworkStatus update) {
	this.timeStamp    = update.getTimeStamp();
	this.networkState = update.getNetworkState();
    }

    /** StatusGrabber/Updater impl.*/
    public void update(StatusCategory status) {
	update((NetworkStatus)status);
    }

    /** Returns the timestamp.*/
    public long getTimeStamp() { return timeStamp; }

    /** Returns the network state.*/
    public int getNetworkState() { return networkState; }

    /** Returns a readable description of the current network condition.*/
    public static String toStateString(int state) {
	switch (state) {
	case NETWORK_ONLINE:
	    return "ONLINE";
	case  NETWORK_OFFLINE:
	    return "OFFLINE";
	default:
	    return "UNKNOWN";
	}
    }

    /** Returns a readable version of the TCS_Status$Segment .*/
    @Override
	public String toString() {
	StringBuffer buffer = new StringBuffer("Network: "+name+" : "+sdf.format(new Date(this.timeStamp))+"(");
	buffer.append("State="+toStateString(networkState));
	buffer.append(")");
	return buffer.toString();
    }

    /** Returns any String Status entries.*/
    public String getStatusEntryId(String key) throws IllegalArgumentException {	 
	throw new IllegalArgumentException("Network: "+name+" : status key: "+key);	
    }
    
    /** Returns any continuous Status entries.*/
    public double getStatusEntryDouble(String key) throws IllegalArgumentException { 
	throw new IllegalArgumentException("Network: "+name+" : status key: "+key);	    
    }
    
    /** Returns any discrete Status entries.*/
    public int getStatusEntryInt(String key) throws IllegalArgumentException {
	if (key.equals("network.state"))
	    return networkState;	  
	else
	    throw new IllegalArgumentException("Network: "+name+" : Unknown status key: "+key);	    
    }

    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. No type conversion 
     * should be attempted.
     */
    public String getStatusEntryRaw(String key) throws IllegalArgumentException {
	if (key.equals("network.state"))
	    return ""+networkState;  
	else
	    throw new IllegalArgumentException("Network: "+name+" : Unknown status key: "+key);	 
    }
    
}

/** $Log: NetworkStatus.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */

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

import ngat.instrument.Instrument;
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

import java.util.*;

import ngat.util.*;


/** InstrumentStatus is a wrapper for a ahashtable.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: InstrumentStatus.java,v 1.1 2006/12/12 08:30:52 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/InstrumentStatus.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public class InstrumentStatus implements SerializableStatusCategory {

    /** Status time stamp.*/
    long timeStamp;

    /** Stores the status key/value pairs.*/
    Hashtable hash;

    boolean currentlySelected = false;

    int onlineStatus = Instrument.OFFLINE;

    /** Create an InstrumentStatus with the supplied key/value mapping.*/
    public InstrumentStatus() {
	hash = new Hashtable();
    }

    /** Update the hashtable with the new mappings.*/
    public void update(Hashtable uHash) {
	hash.putAll(uHash);
    }

    /** Sets whether this instrument is currently selected.*/
    public void setCurrentlySelected(boolean cs) { this.currentlySelected = cs;}
    
    public boolean getCurrentlySelected() { return currentlySelected;}

    /** Sets whether this instrument is online.*/
    public void setOnlineStatus(int ol) { 
	this.onlineStatus = ol;
	switch (ol) {
	case Instrument.OFFLINE:
	    hash.put("network.status", "OFFLINE");
	    break;
	case Instrument.ONLINE:
	    hash.put("network.status", "ONLINE");
	    break;
	}
    }

    public int getOnlineStatus() { return onlineStatus; }

    /** @return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for descriptive (String) status variables.
     */
    public String getStatusEntryId(String key) throws IllegalArgumentException {
	Object value = hash.get(key);
	if (value == null ) return null;
	if (! (value instanceof String) ) 
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not a String ("+value+")");
	return (String)value;
    }
    
    /** @return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for continuous status variables.
     */
    public int getStatusEntryInt(String key) throws IllegalArgumentException {
	Object value = hash.get(key);
	if (value == null )   
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not found");
	if (! (value instanceof Integer) )  
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not an Int ("+value+")");
	return ((Integer)value).intValue();
    }
     
    /** @return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for discrete status variables.
     */
    public double getStatusEntryDouble(String key) throws IllegalArgumentException {
	Object value = hash.get(key);
	if ( value == null ) 
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not found");
	if (! (value instanceof Double) )  
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not a Double ("+value+")");
	return ((Double)value).doubleValue();
    }

    /** @return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method 
     * can be used for any type of variable and does not do type conversion.
     */
    public String getStatusEntryRaw(String key) throws IllegalArgumentException {
	Object value = hash.get(key);
	if ( value == null ) 
	    throw new IllegalArgumentException("CCS_Status: Key: ["+key+"] not found");	
	return value.toString();
    }
    
    /** @return the timestamp of the latest readings.*/
    public long getTimeStamp() {
	return timeStamp;
    }
    
    /** Used to set the timestamp.*/  
    public void setTimeStamp(long timeStamp) {
	this.timeStamp = timeStamp;
    }
    
    @Override
	public String toString() {
    	return "Inst: "+hash;
    }

}

/** $Log: InstrumentStatus.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */

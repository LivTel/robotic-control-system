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
import ngat.message.GUI_RCS.*;

import java.io.*;
import java.util.*;
import java.text.*;

public class InternalStatus implements SerializableStatusCategory {  

    long timeStamp;

    /** The process in control*/
    protected int control;

    /** The the RCS is running in ENGINEERING mode*/
    protected boolean engineering;

    /** The the RCS is currently OPERATIONAL*/
    protected boolean operational;

    /** The RCS exit status on last startup attempt by RCW*/
    protected int lastStatus;

    /** The ID of MCA currently in control or null*/
    protected String agentInControl;

    /** The Name of the MCA currently in control or null*/
    protected String agentName;

    /** The Activity that AIC is doing or null*/
    protected String agentActivity;

  
    /** Set the process in control
     * @param control The process in control.
     */
    public void setControl(int control) { this.control = control; }
    
    /** Get the process in control
     * @return The process in control
     */
    public int getControl() { return control; }
    
    /** Set the the RCS is running in ENGINEERING mode
     * @param engineering The the RCS is running in ENGINEERING mode.
     */
    public void setEngineering(boolean engineering) { this.engineering = engineering; }
    
    /** Get the the RCS is running in ENGINEERING mode
     * @return The the RCS is running in ENGINEERING mode
     */
    public boolean getEngineering() { return engineering; }
    
    /** Set the the RCS is currently OPERATIONAL
     * @param operational The the RCS is currently OPERATIONAL.
     */
    public void setOperational(boolean operational) { this.operational = operational; }
    
    /** Get the the RCS is currently OPERATIONAL
     * @return The the RCS is currently OPERATIONAL
     */
    public boolean getOperational() { return operational; }
    
    /** Set the RCS exit status on last startup attempt by RCW
     * @param lastStatus The RCS exit status on last startup attempt by RCW.
     */
    public void setLastStatus(int lastStatus) { this.lastStatus = lastStatus; }
    
    /** Get the RCS exit status on last startup attempt by RCW
     * @return The RCS exit status on last startup attempt by RCW
     */
    public int getLastStatus() { return lastStatus; }
    
    /** Set the ID of MCA currently in control or null
     * @param agentInControl The ID of MCA currently in control or null.
     */
    public void setAgentInControl(String agentInControl) { this.agentInControl = agentInControl; }
    
    /** Get the ID of MCA currently in control or null
     * @return The ID of MCA currently in control or null
     */
    public String getAgentInControl() { return agentInControl; }
    
    /** Set the Name of the MCA currently in control or null
     * @param agentName The Name of the MCA currently in control or null.
     */
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    /** Get the Name of the MCA currently in control or null
     * @return The Name of the MCA currently in control or null
     */
    public String getAgentName() { return agentName; }
    
    /** Set the Activity that AIC is doing or null
     * @param agentActivity The Activity that AIC is doing or null.
     */
    public void setAgentActivity(String agentActivity) { this.agentActivity = agentActivity; }
    
    /** Get the Activity that AIC is doing or null
     * @return The Activity that AIC is doing or null
     */
    public String getAgentActivity() { return agentActivity; }
    
    
    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for descriptive (String) status variables.
     */
    public String getStatusEntryId(String key) throws IllegalArgumentException {
	if (key.equals("operational")) {
	    if (engineering)
		return "ENGINEERING";
	    else if
		(operational)
		return "OPERATIONAL";
	    else
		return "STANDBY";
	} else if
	    (key.equals("control.process")) {
	    if (control == ID.RCS_PROCESS)
		return "RCS";
	    else if
		(control == ID.WATCHDOG_PROCESS)
		return "WATCHDOG";
	    else
		return "UNKNOWN";
	}
	return "UNKNOWN";
	    
    }
    
    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for continuous status variables.
     */
    public int getStatusEntryInt(String key) throws IllegalArgumentException {
	throw new IllegalArgumentException("No int values available for internal status");
    }
     
    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for discrete status variables.
     */
    public double getStatusEntryDouble(String key) throws IllegalArgumentException {
	throw new IllegalArgumentException("No double values available for internal status");
    }

    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. No type conversion 
     * should be attempted.
     */
    public String getStatusEntryRaw(String key) throws IllegalArgumentException {
	if (key.equals("activity"))
	    return agentActivity;
	else if
	    (key.equals("agent.name"))
	    return agentName;
	else if
	    (key.equals("agent.in.control"))
	    return agentInControl;
	else if 
	    (key.equals("operational")) {
	    if (engineering) {
		return "ENGINEERING";
	    } else if
		(operational) {
		return "OPERATIONAL";
	    } else
		return "STANDBY";
	}
	else
	    return "UNKNOWN";
    }
    
    /** Set the timesatmp.*/
    public void setTimeStamp(long timeStamp) { this.timeStamp =  timeStamp;}
       
    /** Implementors should return the timestamp of the latest readings.*/
    public long getTimeStamp() { return timeStamp; }

    public String toString() { return "Internal: Ctrl: "+getStatusEntryId("control.process")+
    			", Oper: "+getStatusEntryId("operational")+
    			", Aic: "+getAgentInControl();
    }
    
}

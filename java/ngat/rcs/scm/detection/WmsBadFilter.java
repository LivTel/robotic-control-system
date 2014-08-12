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
import ngat.message.RCS_TCS.*;
import ngat.util.logging.*;

import java.util.*;

/** Special filter for WMS status.
 */
public class WmsBadFilter implements Filter{
    
    private static Integer GOOD_VALUE = new Integer(TCS_Status.STATE_OKAY);

    private static Integer BAD_VALUE  = new Integer(TCS_Status.STATE_SUSPENDED);

    private Sensor sensor;

    /** True if the readings so far indicate good state.*/
    private boolean good = true;

    /** How many bad readings so far.*/
    private int badCount = 0;

    /** How many bad readings needed to trigger.*/
    private int badMax;

    /** Time last reading was gained.*/
    private long lastTimeStamp;

    /** The value we are returning now.*/
    private Integer currVal = BAD_VALUE;

    private String  name;
    private boolean spy = false;
    private String  spyLogName;
    private Logger  spyLog;

    public WmsBadFilter(Sensor sensor, String name, int badMax) {
	this.sensor = sensor;
	this.name   = name;
	this.badMax = badMax;
    }

     /** Tell the Sensor to take a sample. Get the reading and
     * if its time is greater than the most recent, add it to
     * the buffer. This method cannot be overridden, use the
     * filteredReading() method to apply the appropriate filtering
     * to the buffered Sensor readings.
     * @return The sample in the buffer, appropriately filtered.
     */
    public Number readout() {

	if (spy)
	    spyLog.log(3, "Filter: "+name+" About to trigger sample from sensor");

	//## Telemetry.publish("SPY", new LogInfo(time, "", "Filter: "+name+" About to trigger sample from sensor");
	
	sensor.sample();
	SensorReading reading = sensor.readout();
	if (spy)
	    spyLog.log(3, "Filter: "+name+" Readout from sensor: "+reading);

	// Only add this if its a new reading and watch first time through !
	// ALSO we should check the readout is VALID with sensor.isValidReading().

	try {
	    
	    if (reading.getTimeStamp() > lastTimeStamp) {
				
		lastTimeStamp = reading.getTimeStamp();
		int value = reading.getDiscreteReading();
		
		// we have a reading so now work out what to return..
		if (value == TCS_Status.STATE_OKAY ||
		    value == TCS_Status.STATE_WARN ||
		    value == TCS_Status.STATE_INVALID) {
		    good = true;
		    badCount = 0;
		    currVal = GOOD_VALUE;
		} else {

		    if (good) {
			// now good, start recording.
			good = false;
			badCount = 1;
		    } else {
			// Still BAD, how many ?
			badCount++;
			if (badCount >= badMax) {
			    // OK
			    currVal = BAD_VALUE;
			} else {
			    // Not OK yet..
			    currVal = GOOD_VALUE;
			}
		    }
		} 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}		

	return currVal;

    }

    
    /** Set the name of this Filter.
     * @param name The anme to set.*/
    public void setName(String name) { this.name = name; }

    /** Returns the name/id of this Filter.
     * @return The anme/id of this Filter.*/
    public String getName() { return name; }

    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }

    public void setSpyLog(String spyLogName) { this.spyLog = LogManager.getLogger(spyLogName); }
    



}

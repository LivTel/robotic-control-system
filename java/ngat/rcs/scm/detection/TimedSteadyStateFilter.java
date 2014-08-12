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

/** Applies a filter to the buffered <b>discrete</b> Sensor
 * readings. The filter only returns a valid reading when the ALL
 * samples in the buffer more recent than a specified time period
 * agree, otherwise it returns a default value. 
 * Readings older than the specified period are removed from the buffer
 * once used, keeping the buffer size managable.
 *
 * ### Keeping the TSSF in the current framework is painful
 * ### and requires some major fudges and re-writes of previously
 * ### finalized code - suggests a need for re-write of the
 * ### architecture of this part of the SCM.
 * 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/TimedSteadyStateFilter.java,v $
 * <br><br>
 * $Id: TimedSteadyStateFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class TimedSteadyStateFilter extends AbstractFilter {
    
    /** The Collection of valid values.*/
    protected Map values;
 
    /** Counts the number of occurrances of each discrete reading.*/
    protected int[] counts;

    /** Number of discrete, valid values.*/
    protected int elements;

    /** Default reading to return if no valid values have been found.*/    
    protected int defaultValue;

    /** The period over which we expect to see the same reading.*/
    protected long period;
   
    /** Create a TimedSteadyStateFilter using the specified Sensor, buffer size and
     * collection of discrete sensor readings. 
     * @param sensor  The Sensor to use.
     * @param period  The period over which we expect to see the same reading.
     * @param valid   The set of valid, discrete readings.
     * @param defaultValue Value returned if no readings stored in buffer.
     */
    public TimedSteadyStateFilter(Sensor     sensor, 
				  long       period, 
				  Collection valid, 
				  int	     defaultValue) {
	super(sensor);
	this.period = period;
	this.defaultValue = defaultValue;

	values = Collections.synchronizedMap(new HashMap());
	
	// Store the expected values against an ascending number index.
	int      ic = 0;
	Iterator it = valid.iterator();
	while (it.hasNext()) {
	    Integer k = (Integer)it.next();
	    values.put(k, new Integer(ic));	   
	    ic++;
	}

	elements = values.size(); 
	counts   = new int[elements];

	// Initialize the count array.
	for (int i = 0; i < elements; i++) {
	    counts[i] = 0;
	}

	System.err.println("TSSF::Init:Details: "+name+" Period:"+period);
    }

    /** Tell the Sensor to take a sample. Get the reading and
     * if its time is greater than the most recent, add it to
     * the buffer. This method cannot be overridden, use the
     * filteredReading() method to apply the appropriate filtering
     * to the buffered Sensor readings.
     * @return The sample in the buffer, appropriately filtered.
     */
    @Override
	public final Number readout() {

	if (spy)
	    spyLog.log(3, "Filter: "+name+" About to trigger sample from sensor");

	System.err.println("TSSF::Call readout:"+name+" About to trigger sample from sensor");

	//## Telemetry.publish("SPY", new LogInfo(time, "", "Filter: "+name+" About to trigger sample from sensor");
	
	sensor.sample();
	SensorReading reading = sensor.readout();

	if (spy)
	    spyLog.log(3, "Filter: "+name+" Readout from sensor: "+reading);

	// Only add this if its a new reading and watch first time through !
	// ALSO we should check the readout is VALID with sensor.isValidReading().
	if (buffer.size() == 0) {
	    buffer.add(reading);	    
	} else {
	    // ###crude way to protect against buffer having one element or 
	    // ###not returning a discrete reading..
	    try {

		SensorReading last = (SensorReading)buffer.get(buffer.size()-1);

		// Check this reading is more recent than the last one we looked at.
		if (reading.getTimeStamp() > last.getTimeStamp()) {

		    if (spy)
			spyLog.log(2, "Filter: "+name+" Adding reading to buffer: Current size: "+buffer.size()); 

		    buffer.add(reading);

		    // Dont include reading if its too recent relative to last one we looked at.	    
		} else {
		    if (spy)
			spyLog.log(2, "Filter: "+name+" TS Diff: "+(reading.getTimeStamp() - last.getTimeStamp()));

		    System.err.println("TSSF::Call readout:Not adding new reading with TSDelta:"+
				       (reading.getTimeStamp() - last.getTimeStamp()));
		    
		}
	    } catch (Exception e) {
		logger.log(3, "Exception OCCURRED: "+e);
	    }
	}
	
	// Remove the oldest reading once we have passed the sample period.
	// At this point we could do some optimizing to make
	// the filteredReading() work faster - e.g. without
	// having to scan the entire buffer each call...
	//	if (buffer.size() > samples)
	//  buffer.remove(0);
	
	
	return filteredReading();
    }
    
    /** Applies a steady-state timed filter to the buffered discrete Sensor
     * readings.
     * @return If the buffer values all agree then this value, otherwise a default
     */
    @Override
	public Number filteredReading() {
	
	// Check we have any readings.	
	if (buffer.size() == 0) {
	    if (spy)
		spyLog.log(2, "SteadyStateFilter: "+name+" Buffer size: "+buffer.size()+" Return default: "+defaultValue );
	    System.err.println("TSSF::Call FilteredReading:Buffer zero-size test: "+name+
			       " Buffer size: "+buffer.size()+" Return default: "+defaultValue );
	    return new Integer(defaultValue);
	}
	
	// Initialize the count array. 
	if (spy)
	    spyLog.log(2, "SteadyStateFilter:"+name+" initializing count array with "+counts.length+" cells");
	
	System.err.println("TSSF::Call FilteredReading:Init count:"+name+" initializing count array with "+counts.length+" cells");
	for (int i = 0; i < counts.length; i++) {
	    counts[i] = 0;
	}
	
	SensorReading reading = null;
	int ct       = 0;
	int maxCount = 0;
	int maxKey   = defaultValue;
	int index    = 0;
	int keyVal   = 0;
	Integer key  = null;
	
	SensorReading last  = (SensorReading)buffer.get(buffer.size()-1);
	long latestTime = last.getTimeStamp();
	SensorReading first = (SensorReading)buffer.get(0);
	long firstTime = first.getTimeStamp();
	
	if ((latestTime - firstTime) > period) {
	    
	    maxKey = defaultValue;
	
	} else {
	    
	    // Loop over buffered samples.
	    // Find count index with largest size.
	    Iterator it = buffer.iterator();
	    while (it.hasNext()) {
		reading = (SensorReading)it.next();	  
		keyVal  = reading.getDiscreteReading();
		key     = new Integer(keyVal);
		
		if ( ! values.containsKey(key) ) {
		    if (spy)
			spyLog.log(2, "SteadyStateFilter:"+name+" At index: "+index+" Valid[] does not contain: "+key); 
		    continue;	 
		}
		
		index = ((Integer)values.get(key)).intValue();
		ct = counts[index];
		counts[index] = ct+1;
		if (ct >= maxCount) {
		    maxCount = ct+1;
		    maxKey= keyVal;
		}
		
		if (spy)
		    spyLog.log(2, "SteadyStateFilter:"+name+
			       " Chk Buff Rdg: "+key+
			       " Index: "+index+
			       " BS="+buffer.size()+
			       " Ct[i]="+counts[index]+
			       " Mxct="+maxCount+
			       " MxKy="+maxKey);
		System.err.println("TSSF::Call FilteredReading:Chk Buff Rdgs: "+key+
				   " Index(=i): "+index+
				   " BS="+buffer.size()+
				   " Ct[i]="+counts[index]+
				   " Mxct="+maxCount+
				   " MxKy="+maxKey);
	    }
	
	    if (maxCount != buffer.size())
		maxKey = defaultValue; 
	    
	    if (spy)
		spyLog.log(2, "SteadyStateFilter:"+name+" Returning: "+maxKey+" From buffer with: "+buffer.size()+" Samples.");
	}

	// Test to see if we can remove the first element from the buffer yet.
	// The oldest and second oldest items need to be older than P, 
	// If the second item is not old enough then we would
	// fail next time round even though the data is really ok.
	if (buffer.size() > 1) {
	    SensorReading second = (SensorReading)buffer.get(1);
	    long secondTime = second.getTimeStamp();
	    if ((latestTime - secondTime) > period) {
		System.err.println("TSSF::Call FilteredReading:Removing oldest reading from buffer");
		buffer.remove(0);
	    }
	    
	}
	
	System.err.println("TSSF::Call FilteredReading:Returning value: "+maxKey+
			   " From buffer with: "+buffer.size()+" Samples.");
	
	return new Integer(maxKey);
	
    }
    
}

/** $Log: TimedSteadyStateFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/***/

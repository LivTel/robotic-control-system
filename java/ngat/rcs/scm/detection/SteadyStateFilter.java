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
 * samples in the buffer agree, otherwise it returns a default
 * value. The buffer of historic readings advances
 * each time a new (more recent) reading is obtained from the Sensor,
 * thus the buffer is cyclic. i.e. only stores last N readings.
 *
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/SteadyStateFilter.java,v $
 * <br><br>
 * $Id: SteadyStateFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class SteadyStateFilter extends AbstractFilter {
    
    /** The Collection of valid values.*/
    protected Map values;
 
    /** Counts the number of occurrances of each discrete reading.*/
    protected int[] counts;

    /** Number of discrete, valid values.*/
    protected int elements;

    /** Default reading to return if no valid values have been found.*/    
    protected int defaultValue;
   
    /** Create a SteadyStateFilter using the specified Sensor, buffer size and
     * collection of discrete sensor readings. 
     * @param sensor  The Sensor to use.
     * @param samples The number of samples to buffer.
     * @param vaild   The set of valid, discrete readings.
     */
    public SteadyStateFilter(Sensor sensor, int samples, Collection valid, int defaultValue) {
	super(sensor, samples);
	values = Collections.synchronizedMap(new HashMap());
	this.defaultValue = defaultValue;

	// Store the values against an ascending number index.
	int      ic = 0;
	Iterator it = valid.iterator();
	while (it.hasNext()) {
	    Integer k = (Integer)it.next();
	    values.put(k, new Integer(ic));
	    System.err.println("SSF: Index: "+ic+" -> "+k);
	    ic++;
	}

	elements = values.size(); 
	counts   = new int[elements];

	// Initialize the count array.
	for (int i = 0; i < elements; i++) {
	    counts[i] = 0;
	}
	
    }

    /** Applies a steady-state filter to the buffered discrete Sensor
     * readings.
     * @return If the buffer values all agree then this value, otherwise a default
     */
    @Override
	public Number filteredReading() {
	// Check we have any readings.
	
	if (buffer.size() == 0) {
	    if (spy)
		spyLog.log(2, "SteadyStateFilter: "+name+" Buffer size: "+buffer.size()+" Return default: "+defaultValue );
	    return new Integer(defaultValue);
	}
	
	// Initialize the count array. 
	if (spy)
		spyLog.log(2, "SteadyStateFilter:"+name+" initializing count array with "+counts.length+" cells");

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
	    
	}
	
	if (maxCount != samples)
	    maxKey = defaultValue; 
	
	if (spy)
	    spyLog.log(2, "SteadyStateFilter:"+name+" Returning: "+maxKey+" From buffer with: "+buffer.size()+" Samples.");
	return new Integer(maxKey);

    }
    
}

/** $Log: SteadyStateFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/***/

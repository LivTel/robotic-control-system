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
import ngat.util.logging.*;

import java.text.*;
import java.util.*;

/** Generic class for implementing StatusPool based Sensors.
 * The dr and ir fields can be used to store the latest values
 * of the sensed variable to be returned (as a Discrete or 
 * Continuous SensorReading) by the readout() method.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: PoolLookupSensor.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/PoolLookupSensor.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class PoolLookupSensor implements Sensor {
    
    /** Constant - indicates that this Sensor provides Discrete (integer/state) readings.*/
    public static final int DISCRETE_READOUT   = 1;
    
    /** Constant - indicates that this Sensor provides Continuous (double) readings.*/
    public static final int CONTINUOUS_READOUT = 2;

    /** Constant - indicates that this Sensor provides a timestamp.*/
    public static final int TIMESTAMP_READOUT = 3;

    /** Constant - indicates that this Sensor provides a time difference from current time.*/
    public static final int TIMEDIFF_READOUT = 4;

    /** Class name for logging.*/
    public static final String CLASS = "PoolLookupSensor";
    
    /** Used to format log output.*/
    protected static NumberFormat nf = NumberFormat.getInstance();

    /** Used to format log output.*/
    protected static SimpleDateFormat sdf;

    /** The name of the sensor.*/
    protected String name;
    
    /** Set to indicate that this Filter's detailed operations are to be logged.*/
    protected boolean spy;

    /** Detailed operations 'spy' logger.*/
    protected Logger spyLog;

    /** Discrete Sensor reading.*/
    protected int ir;
    
    /** Continuous Sensor reading.*/
    protected double dr;

    /** Time difference (staleness) reading.*/
    protected long diff;
    
    /** Time when the reading was created.*/
    protected long time;

    /** Whether the current readout is valid.*/
    protected boolean valid;

    /** Type of readout required.*/
    protected int readoutType;
    
    /** Status Key.*/
    protected String key;

    /** StatusGrabber to obtain status readings from the pool.*/
    protected StatusCategory scat;

    static {
	nf.setMaximumFractionDigits(3);
	nf.setMinimumFractionDigits(3);
	sdf = RCS_Controller.iso8601;
    }

    /** Create a PoolSensor, specifying whether analog or digital.
     * @param name The unique ID for this sensor.
     * @param cat  The ID of the StatusGrabber which obtains status from a pool.
     * @param key  The key used to access the Grabber's output.
     * @param type Determines if the output will be continuous or discrete.
     * @exception
     */
    public PoolLookupSensor(String name, String cat, String key, int type) 
	throws IllegalArgumentException {
	this.name     = name;
	LegacyStatusProviderRegistry emmRegistry = LegacyStatusProviderRegistry.getInstance();
	if (emmRegistry == null)
	    throw new IllegalArgumentException("PoolLookupSensor: "+name+" EMM_Registry not defined.");
	
	scat = emmRegistry.getStatusCategory(cat);
	if (scat == null)
	    throw new IllegalArgumentException("PoolLookupSensor: "+name+
					       " StatusCategory: "+scat+
					       " No Status-cat defined.");
	this.key = key;
	this.readoutType = type;

	valid = false;

    }
    
    /** Returns the name/Id of this Sensor.*/
    public String getName() {
	return name;
    }
    
    /** Grabs the Status identified by the key from the StatusCat.
     * If the entry is not valid (e.g. dodgy key) then the readout is
     * set to invalid and can be seen via isValidReadout().
     */
    public void sample() {
	long now = System.currentTimeMillis();
	switch (readoutType) {
	case TIMEDIFF_READOUT:
	    time  = now;
	    valid = true;
	    diff = now - scat.getTimeStamp();
	    break;
	case TIMESTAMP_READOUT:
	    time = scat.getTimeStamp();
	    valid = true;
	    break;
	case DISCRETE_READOUT:
	    try {
		ir   = scat.getStatusEntryInt(key);
		time = scat.getTimeStamp();
		valid = true;
	    } catch (IllegalArgumentException iax) {
		valid = false;
	    }
	    break;
	case CONTINUOUS_READOUT:
	    try {
		dr   = scat.getStatusEntryDouble(key);
		time = scat.getTimeStamp();
		valid = true;
	    } catch (IllegalArgumentException iax) {
		valid = false;
	    }
	    break;
	default:
	    
	}
	//System.err.println("Sample: "+key+" at: "+time);
    }
    
    /** @return The latest readings from the pool sensor.*/
    public SensorReading readout() {
	//System.out.println("pool-sensor-readout:");

	SensorReading reading = null;

	switch (readoutType) {
	case TIMEDIFF_READOUT:

	   reading = new SensorReading(diff);

	    if (spy)
		spyLog.log(3, "PoolSensor: "+name+" Timediff reading: ["+diff+"] at"+
			   sdf.format(new Date()));	 
	    
	    // ## Telemetry.publish("SPY", new SensorInfo(time, getName(), TIMEDIFF_READOUT,  reading))
	    
	    return reading;
	case TIMESTAMP_READOUT:

	    reading = new SensorReading(time);
	    
	    if (spy)
		spyLog.log(3, "PoolSensor: "+name+" Timestamp reading: ["+time+"] is "+
			   sdf.format(new Date(time)));	    

	    // ## Telemetry.publish("SPY", new SensorInfo(time, getName(), TIMESTAMP_READOUT,  reading))
	    
	    return reading;
	case DISCRETE_READOUT:
	    
	    reading = new SensorReading(ir, time);

	    if (spy)
		spyLog.log(3, "PoolSensor: "+name+" Discrete reading: ["+ir+"] at "+
			   sdf.format(new Date(time)));	    

	    // ## Telemetry.publish("SPY", new SensorInfo(time, getName(), DISCRETE_READOUT,  reading))
	    
	    return reading;
	case CONTINUOUS_READOUT:

	    reading = new SensorReading(dr, time);
	    
	    if (spy)
		spyLog.log(3, "PoolSensor: "+name+" Continuous reading: ["+dr+"] at "+
			   sdf.format(new Date(time)));

	     // ## Telemetry.publish("SPY", new SensorInfo(time, getName(), CONTINUOUS_READOUT,  reading))
	    
	    
	    return reading;
	default:
	    // Assume continuous. 
	    reading = new SensorReading(dr, time);
	    
	    if (spy)
		spyLog.log(3, "PoolSensor: "+name+" Assume Continuous reading: ["+dr+"] at "+
			   sdf.format(new Date(time)));

	    // ## Telemetry.publish("SPY", new SensorInfo(time, getName(), CONTINUOUS_READOUT,  reading))
	    
	    return reading;
	}

    }
    
    /** Indicates whether the readout is currently valid.*/
    public boolean isValidReadout() { return valid; }

    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }

    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName) { spyLog = LogManager.getLogger(spyLogName); }
    
}

/** $Log: PoolLookupSensor.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */

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


/**
 * Encapsulates the data produced by a Sensor implementation.
 * The data consist of a timestamp and a value which may be
 * real (continuous valued) or int (discrete/quantized).
 * Subclasses may be written to provide specific versions of 
 * this e.g. DiscreteSensorReading, ContinuousSensorReading etc.
 * Further, Discrete sensorreadings may encapsulate the possible
 * values in a set of static final variables
 * The data from a Sensor is normally fed through a Filter.
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * $Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/SensorReading.java,v $
 * 
 * $Id: SensorReading.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 */
public class SensorReading {

    /** The reading stored as an int - discrete valued.*/
    private int iVal;

    /** The reading stored as a double - continuous valued.*/
    private double dVal;
    
    /** The reading stored as a time-quantity in millis - temporal valued.*/
    private long timediff;

    /** The time (msec since 1970) the reading was made.*/
    private long timeStamp;

    /** True if the reading is discrete otherwise continuous.*/
    private boolean discrete;

    /** True if the reading is a time quantity.*/
    private boolean temporal;

    /** Create a SensorReading with continuous value.
     * @param value The continuous valued reading.
     * @param time The time of the sample.
     */
    public SensorReading(double value, long time) {
	dVal = value;
	iVal = (int)dVal;
	discrete = false;
	timeStamp = time;
    }
  
    /** Create a SensorReading with discrete value.
     * @param value The discrete valued reading.
     * @param time The time of the sample.
     */
    public SensorReading(int value, long time) {
	iVal = value;
	dVal = iVal;
	discrete = true;
	timeStamp = time;
    }

    /** Create a SensorReading with temporal value.
     * @param time The time of the time sample.
     */
    public SensorReading(long time) {
	temporal = true;
	timeStamp = time;
    }
    

    /** @return The continuous valued reading.*/
    public double getContinuousReading() { return dVal; }

    /** @return The discrete valued reading.*/
    public int    getDiscreteReading() { return iVal; }

    /** @return The temproal reading.*/
    public long getTemporalReading() { return timediff; }

    /** @return The time (msec since 1970) the reading was made.*/
    public long   getTimeStamp() { return timeStamp; }

    /** @return True if the reading is discrete valued, false if
     * it is continuously variable.*/
    public boolean isDiscrete()   { return discrete; }

    /** @return True if the reading is temporal.*/
    public boolean isTemporal() { return temporal; }


    @Override
	public String toString() { 
	return "[SensorReading : "+(temporal ? "Temporal:"+timeStamp : (discrete ? "Discrete:"+iVal : "Variable:"+dVal))+"]";
    }

}

/** $Log: SensorReading.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.3  2000/12/14 11:53:56  snf
/** Updated.
/**
/** Revision 1.2  2000/12/14 11:42:00  snf
/** Changed dble/int to Discrete/Continuous.
/**
/** Revision 1.1  2000/12/13 11:45:14  snf
/** Initial revision
/**
/** Revision 1.1  2000/11/06 12:45:31  snf
/** Initial revision
/** */

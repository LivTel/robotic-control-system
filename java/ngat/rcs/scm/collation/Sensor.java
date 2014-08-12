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
 * Provides a consistent interface to environmental or other variables which
 * must be read at regular intervals.
 *
 * The following example shows an implementation based on a globally accessable
 * shared data object <i>Data</i>. Note that this implementation always returns 
 * the same value for SensorReading no matter how often it is called until the
 * next update is triggered by calling sample(). An alternative version might 
 * update the timestamp by calling System.currentTimeMillis() in the read() method.
 *
 * <pre>
 * public class DataSensor implements Sensor {
 *
 *     private SensorReading latestReading = new SensorReading();
 *
 *     public void sample() {
 *         latestReading.value     = Data.getSomeValue();
 *         latestReading.timestamp = System.currentTimeMillis();
 *     }
 *
 *     public SensorReading read() {
 *         return latestReading;
 *     }
 *
 * }
 * </pre>
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * $Revision: 1.1 $
 * $Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/Sensor.java,v $
 *
 * $Id: Sensor.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 */
public interface Sensor {
    
    /** Returns the name/Id of this Sensor.*/
    public String getName();

    /** Implementations should use this method to update local variables
     * with the current sensor reading. After sample() is called a call to
     * readout() should return a new reading - if the reading is invalid then a
     * call to isValidReadout() should return false. Filters using such
     * Sensor readings should decide how to deal with invalid readings.*/
    public void sample();
    
    /** Impementations use this method to return a timestamped reading.*/
    public SensorReading readout();

    /** Impementations may use this method to indicate whether the readings are currently valid.*/
    public boolean isValidReadout();

    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy);

    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName);
    
}

/** $Log: Sensor.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.2  2000/12/14 11:41:31  snf
/** Changed update() to sample().
/**
/** Revision 1.1  2000/12/13 11:45:05  snf
/** Initial revision
/**
/** Revision 1.1  2000/11/06 12:29:06  snf
/** Initial revision
/** */

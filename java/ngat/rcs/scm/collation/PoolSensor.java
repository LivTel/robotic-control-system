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
import ngat.util.logging.*;

/** Generic class for implementing StatusPool based Sensors.
 * The dr and ir fields can be used to store the latest values
 * of the sensed variable to be returned (as a Discrete or 
 * Continuous SensorReading) by the readout() method.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: PoolSensor.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/PoolSensor.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public abstract class PoolSensor implements Sensor {
    
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

    /** True if the Sensor creates discrete readings.*/
     protected boolean discrete;
    
    /** Time when the reading was created.*/
     protected long time;
    
    /** Create a PoolSensor, specifying whether analog or digital.
     * @param discrete True if the sensor generates digital readings
     * false if analog.
     */
    public PoolSensor(String name, boolean discrete) {
	this.name     = name;
	this.discrete = discrete;
    }
 
    /** Returns the name/Id of this Sensor.*/
    public String getName() {
	return name;
    }

    /** @return The latest readings from the pool sensor.*/
    public SensorReading readout() {
	//System.out.println("pool-sensor-readout:");
	if (spy)
	    spyLog.log(1, "Sensor: "+name+" Called readout(): Returning: "+(discrete ? ir : dr));
	if (discrete) 
	    return new SensorReading(ir, time);
	else
	    return new SensorReading(dr, time);
    }

    /** Returns true if readout is valid.*/
    public boolean isValidReadout() { return true; } 
    
    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }

    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName) { spyLog = LogManager.getLogger(spyLogName); }
    
}

/** $Log: PoolSensor.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.3  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/18 17:26:55  snf
/** Initial revision
/** */

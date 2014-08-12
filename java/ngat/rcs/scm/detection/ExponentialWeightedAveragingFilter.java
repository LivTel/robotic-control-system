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

/** Filter which performs an exponential weighted average of a set
 * of buffered Sensor readings using a given relaxation time constant.
 * Contributions from successively older samples are reduced such that
 * the overall filtered output is:-<br>
 * <table cols = 2><td align = "center" valign = "center">AVERAGE = </td><td align = "left" valign = "center"><img src = "doc-files/exponential-weighting.gif"></td></table>
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/ExponentialWeightedAveragingFilter.java,v $
 * <br><br>
 * $Id: ExponentialWeightedAveragingFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class ExponentialWeightedAveragingFilter extends AveragingFilter {

    /** Relaxation time constant.*/
    protected double timeConst;

    /** Create an ExponentialWeightedAveragingFilter using specified number
     * of samples and relaxation time constant. 
     * @param sensor The Sensor to take readings from.
     * @param sample The number of Sensor readings to buffer.
     * @param timeConst The relaxation time constant (msec).
     */
    public ExponentialWeightedAveragingFilter(Sensor sensor, int samples, double timeConst) {
	super(sensor, samples);
	this.timeConst = timeConst;
    }

    /** Applies the weighted averaging function to the buffered
     * sensor readings.
     * @return Exponentially weighted average of the buffered
     * Sensor readings using the relaxation constant.
     */
    @Override
	public Number filteredReading() {
	// Check we have any readings.
	if (buffer.size() == 0) return new Double(0.0);
	
	SensorReading reading = null;
	double sum   = 0.0;
	double avge  = 0.0;
	double ww    = 0.0;
	long t0      = System.currentTimeMillis();
	 
	// Loop over buffered samples. Note we scale the exp-arg to make
	// sure its not too large (i.e. subtract current time off it).
	// This has no effect on the result but produces smaller args
	// top and bottom of the equation.
	Iterator it = buffer.iterator();
	while (it.hasNext()) {
	    reading = (SensorReading)it.next();
	    ww = Math.exp(((double)reading.getTimeStamp() - t0)/timeConst);
	    avge = avge + ww * reading.getContinuousReading();
	    sum  = sum  + ww;
	}
	return new Double(avge / sum);
    }

    /** Sets the time constant (msec) to be used for the weighted average.
     * @param timeConst The value to use (msec) for the time constant.*/
    public void   setTimeConst(double timeConst) { this.timeConst = timeConst; }
    
    /** @return The time constant in use.*/
    public double getTimeConst() { return timeConst; }

}

/** $Log: ExponentialWeightedAveragingFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2000/12/22 14:40:37  snf
/** Backup.
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */

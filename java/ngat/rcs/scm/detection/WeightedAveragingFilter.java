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

/** Filter which performs a weighted average of a set of buffered
 * Sensor readings. The form of the averaging is:-<br>
 * <pre>
 *    average = sum samp<sub>i</sub> * weight<sub>i</sub> / sum weight<sub>i</sub>
 * </pre>
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/WeightedAveragingFilter.java,v $
 * <br><br>
 * $Id: WeightedAveragingFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class WeightedAveragingFilter extends AbstractFilter {

    /** Stores the weights (multipliers) to be used to calculate
     * the weighted average of Sensor readings. The size of this
     * array will, if less than the buffer size effectively impose
     * a truncation on the averaging process - i.e. if there are
     * fewer weight values than stored readings, the last few
     * readings will not be included.*/
    protected double[] weights; 
 
    /** Create a WeightedAveragingFilter with fixed number of
     * Sensor readings to average over. 
     * @param sensor The Sensor to take readings from.
     * @param samples The number of Sensor readings to store.
     */
    public WeightedAveragingFilter(Sensor sensor, int samples) {
	super(sensor, samples);
	weights = new double[samples];
    }
    
    /** Create a WeightedAveragingFilter with specified weights. 
     * @param sensor The Sensor to take readings from.
     * @param weights The array of weights to use. The zeroth element
     * is applied to the latest sample.
     */
    public WeightedAveragingFilter(Sensor sensor, double[] weights) {
	super(sensor, weights.length);
	this.weights = weights;
    }
    
    /** Set the weights. The n<super>th</super> element of the
     * array is the multiplier for the n<super>th</super> latest
     * sample. i.e. element #0 is the most recent sample, element 
     * #(weights.length-1) is the oldest. this method also resets
     * the value for the samples field.
     * @param weights The weights to set.
     */
    public void setWeights(double[] weights) {
	this.weights = weights;
	samples = weights.length;
    }
    
    /** @return The weights in use.*/
    public double[] getWeights() { return weights; }

    /** Applies the weighted averaging function to the buffered
     * sensor readings. If less weights are specified than there 
     * are buffered readings, a weight of 0.0 is applied to any 
     * such readings - thus effectively eliminating them.
     * @return Weighted average of the buffered Sensor readings.
     */
    @Override
	public Number filteredReading() {		
	// Check we have any readings.
	if (buffer.size() == 0) return new Double(0.0);
	
	SensorReading reading = null;
	double sum   = 0.0;
	double avge  = 0.0;
	int    index = 0;
	double ww    = 0.0;

	// Loop over buffered samples.
	Iterator it = buffer.iterator();
	while (it.hasNext()) {
	    reading = (SensorReading)it.next();	
	    if (index < weights.length)
		ww = weights[index];
	    else
		ww = 0.0;

	    avge = avge + ww * reading.getContinuousReading();
	    sum  = sum  + ww;
	    index++;
	}
	return new Double(avge / sum);
    }   
    
}

/** $Log: WeightedAveragingFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */

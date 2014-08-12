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

/** Filter which carries out a simple average over its buffered
 * Sensor readings. There is no attempt at weighting or trending.
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/AveragingFilter.java,v $
 * <br><br>
 * $Id: AveragingFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class AveragingFilter extends AbstractFilter {
  
    /** Creates an AveragingFilter witht the specified number of samples
     * to use for the average. 
     * @param sensor The Sensor to take readings from.
     * @param samples The maximum number of Sensor readings to average
     * - this is also the buffer size.
     */
    AveragingFilter(Sensor sensor, int samples) {
	super(sensor, samples);
    }
    
    /** Applies the averaging to the buffered Sensor readings. If there
     * are fewer samples then the average is over those available.
     * @return The averaged sensor readings.
     */
    @Override
	public Number filteredReading() {	
	// Check we have any readings.
	if (buffer.size() == 0) return new Double(0.0);

	SensorReading reading = null;
	double sum   = 0.0;
	double avge  = 0.0;
	int    index = 0;

	// Loop over buffered samples.
	Iterator it = buffer.iterator();
	while (it.hasNext()) {
	    reading = (SensorReading)it.next();
	    avge = avge + reading.getContinuousReading();
	    index++;
	}
	logger.log(2, "Av-Filter", name, "filteredReading","Returned: "+
		   (avge/index)+" using "+index+" samples.");
	return new Double(avge / index);
    }
    
}

/** $Log: AveragingFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */

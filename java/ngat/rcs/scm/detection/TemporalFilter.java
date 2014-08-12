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

/** Filter which carries out a simple pass-through of its single
 * Temporal Sensor reading. 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/TemporalFilter.java,v $
 * <br><br>
 * $Id: TemporalFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class TemporalFilter extends AbstractFilter {
  
    /** Creates an AveragingFilter witht the specified number of samples
     * to use for the average. 
     * @param sensor The Sensor to take readings from.
     * @param samples The maximum number of Sensor readings to average
     * - this is also the buffer size.
     */
    TemporalFilter(Sensor sensor) {
	super(sensor, 1);
    }
    
    /** Applies the averaging to the buffered Sensor readings. If there
     * are fewer samples then the average is over those available.
     * @return The averaged sensor readings.
     */
    @Override
	public Number filteredReading() {	
	// Check we have any readings.
	if (buffer.size() == 0) return new Long(0L);

	SensorReading reading = null;
	long avge = 0L;
	int  index = 0;

	// Loop over buffered samples.
	Iterator it = buffer.iterator();
	while (it.hasNext()) {
	    reading = (SensorReading)it.next();
	    avge = avge + reading.getTemporalReading();
	    index++;
	}
	logger.log(2, "Time-Filter", name, "filteredReading","Returned: "+
		   (avge/index)+" using "+index+" samples.");
	return new Long(avge / index);
    }
    
}

/** $Log: TemporalFilter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/** */

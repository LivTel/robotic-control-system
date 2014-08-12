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
import ngat.util.logging.*;

import java.util.*;

/**
 * Filters are attached to Sensors to carry out some form of filtering operation
 * on the received samples. Filters can buffer Sensor readings in order to carry
 * out averaging etc.
 * 
 * <br>
 * <br>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $ Source $Source:
 *          /home/dev/src/rcs/java/ngat/rcs/scm/
 *          detection/RCS/AbstractFilter.java,v $ <br>
 * <br>
 *          $Id: AbstractFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public abstract class AbstractFilter implements Filter {

	/**
	 * Default size for the Sensor readings buffer - this is the number of
	 * Sensor readings that can be stored for any filtering operations.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 5;

	/** A buffer to hold sensor samples. */
	protected List buffer;

	/** Stores the maximum number of samples to store. */
	protected int samples;

	/** The Sensor to which this Filter is attached. */
	protected Sensor sensor;

	/** The name/id of this Filter. */
	protected String name;

	/** Logger for this class. */
	protected Logger logger;

	/** Set to indicate that this Filter's detailed operations are to be logged. */
	protected boolean spy;

	/** Logger for this class. */
	protected Logger spyLog;

	/**
	 * Create a Filter attached to the specified Sensor and using the default
	 * buffer size.
	 * 
	 * @param sensor
	 *            The Sensor to take readings from.
	 */
	public AbstractFilter(Sensor sensor) {
		this(sensor, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Create a Filter with the specified buffer capacity and using the
	 * specified Sensor.
	 * 
	 * @param buffersize
	 *            The maximum number of Sensor readings which can be buffered.
	 * @param sensor
	 *            The Sensor to take readings from.
	 */
	public AbstractFilter(Sensor sensor, int buffersize) {
		this(sensor, buffersize, "");
	}

	/**
	 * Create a Filter with the specified name, buffer capacity and using the
	 * specified Sensor..
	 * 
	 * @param buffersize
	 *            The maximum number of Sensor readings which can be buffered.
	 * @param sensor
	 *            The Sensor to take readings from.
	 * @param name
	 *            The name/id of this Filter for logging.
	 */
	public AbstractFilter(Sensor sensor, int buffersize, String name) {
		samples = buffersize;
		buffer = new Vector(buffersize);
		this.sensor = sensor;
		this.name = name;
		logger = LogManager.getLogger("TRACE");
	}

	/**
	 * Tell the Sensor to take a sample. Get the reading and if its time is
	 * greater than the most recent, add it to the buffer. This method cannot be
	 * overridden, use the filteredReading() method to apply the appropriate
	 * filtering to the buffered Sensor readings.
	 * 
	 * @return The sample in the buffer, appropriately filtered.
	 */
	public Number readout() {

		if (spy)
			spyLog.log(3, "Filter: " + name + " About to trigger sample from sensor");

		// ## Telemetry.publish("SPY", new LogInfo(time, "",
		// "Filter: "+name+" About to trigger sample from sensor");

		sensor.sample();
		SensorReading reading = sensor.readout();
		if (spy)
			spyLog.log(3, "Filter: " + name + " Readout from sensor: " + reading);
		// Only add this if its a new reading and watch first time through !
		// ALSO we should check the readout is VALID with
		// sensor.isValidReading().
		if (buffer.size() < 1)
			buffer.add(reading);
		else {
			try {
				SensorReading last = (SensorReading) buffer.get(buffer.size() - 1);
				if (reading.getTimeStamp() > last.getTimeStamp()) {
					buffer.add(reading);
					if (spy)
						spyLog.log(2, "Filter: " + name + " Adding reading to buffer: size = " + buffer.size());
				} else {
					if (spy)
						spyLog
								.log(2, "Filter: " + name + " TS Diff: "
										+ (reading.getTimeStamp() - last.getTimeStamp()));
				}
			} catch (Exception e) {
				logger.log(3, "Exception OCCURRED: " + e);
			}
		}
		// Remove the oldest reading once the buffer is full.
		// At this point we could do some optimizing to make
		// the filteredReading() work faster - e.g. without
		// having to scan the entire buffer each call...
		if (buffer.size() > samples)
			buffer.remove(0);
		return filteredReading();
	}

	/**
	 * Concrete sub-classes override this method to perform the filtering
	 * operation appropriate to a specific filter using the contents of the
	 * buffer.
	 * 
	 * @return The filtered readings.
	 */
	public abstract Number filteredReading();

	/**
	 * Attaches the nominated Sensor as the source of readings for this filter.
	 * If the sensor parameter is null this method does nothing and returns
	 * silent.
	 * 
	 * @param sensor
	 *            The <b>non null</b> Sensor to attach.
	 */
	public void attachSensor(Sensor sensor) {
		if (sensor != null)
			this.sensor = sensor;
	}

	/** @return A reference to the currently attached Sensor. */
	public Sensor getSensor() {
		return sensor;
	}

	/**
	 * Set the name of this Filter.
	 * 
	 * @param name
	 *            The anme to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the name/id of this Filter.
	 * 
	 * @return The anme/id of this Filter.
	 */
	public String getName() {
		return name;
	}

	/** Set true to enable 'spy' logging. */
	public void setSpy(boolean spy) {
		this.spy = spy;
	}

	public void setSpyLog(String spyLogName) {
		this.spyLog = LogManager.getLogger(spyLogName);
	}

}

/**
 * $Log: AbstractFilter.java,v $ /** Revision 1.1 2006/12/12 08:31:16 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:35:17 snf /** Initial
 * revision /** /** Revision 1.2 2001/02/16 17:44:27 snf /** *** empty log
 * message *** /** /** Revision 1.1 2000/12/14 11:53:56 snf /** Initial revision
 * /**
 */

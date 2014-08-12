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

import ngat.message.RCS_TCS.TCS_Status;
import ngat.rcs.scm.collation.*;

import java.util.*;

/**
 * Applies a modal filter to the buffered <b>discrete</b> Sensor readings. The
 * most commonly occurring reading is chosen as the filtered result. The buffer
 * of historic readings advances each time a new (more recent) reading is
 * obtained from the Sensor, thus the buffer is cyclic. i.e. only stores last N
 * readings.
 * 
 * There are 2 ways to specify the set of values to test against.
 * <ul>
 * <li>By specifying a list of values via a Collection.
 * <li>By specifying the first amd last values in a sequential range.
 * </ul>
 * 
 * 
 * <br>
 * <br>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $ Source $Source:
 *          /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/ModalFilter.java,v
 *          $ <br>
 * <br>
 *          $Id: ModalFilter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class ModalFilter extends AbstractFilter {

	/** The Collection of valid values. */
	protected Map values;

	/** Counts the number of occurrances of each discrete reading. */
	protected int[] counts;

	/** Number of discrete, valid values. */
	protected int elements;

	/** Default reading to return if no valid values have been found. */
	protected int defaultValue;

	/**
	 * Create a ModalFilter using the specified Sensor, buffer size and range of
	 * discrete sensor readings.
	 * 
	 * @param sensor
	 *            The Sensor to use.
	 * @param samples
	 *            The number of samples to buffer.
	 * @param r1
	 *            The minimum value for a discrete Sensor reading.
	 * @param r2
	 *            The maximum value for a discrete Sensor reading.
	 */
	public ModalFilter(Sensor sensor, int samples, int r1, int r2, int defaultValue) {
		super(sensor, samples);
		values = Collections.synchronizedMap(new HashMap());
		this.defaultValue = defaultValue;

		// Store the values against an ascending number index.
		int ic = 0;
		for (int k = r1; k <= r2; k++) {
			values.put(new Integer(k), new Integer(ic++));
			if (spy)
				spyLog.log(2, "ModalFilter: " + name + " Valid Key [" + ic + "] = " + k + " ("
						+ TCS_Status.codeString(k) + ")");
		}
		if (spy)
			spyLog.log(2, "ModalFilter: " + name + " Default Key  = " + defaultValue + " ("
					+ TCS_Status.codeString(defaultValue) + ")");

		elements = values.size();
		counts = new int[elements];

		// Initialize the count array.
		for (int i = 0; i < elements; i++) {
			counts[i] = 0;
		}

		
		
	}

	/**
	 * Create a ModalFilter using the specified Sensor, buffer size and
	 * collection of discrete sensor readings.
	 * 
	 * @param sensor
	 *            The Sensor to use.
	 * @param samples
	 *            The number of samples to buffer.
	 * @param vaild
	 *            The set of valid, discrete readings.
	 */
	public ModalFilter(Sensor sensor, int samples, Collection valid, int defaultValue) {
		super(sensor, samples);
		values = Collections.synchronizedMap(new HashMap());
		this.defaultValue = defaultValue;

		// Store the values against an ascending number index.
		int ic = 0;
		Iterator it = valid.iterator();
		while (it.hasNext()) {
			Integer k = (Integer) it.next();
			values.put(k, new Integer(ic++));
			if (spy)
				spyLog.log(2, "ModalFilter: " + name + " Valid Key [" + ic + "] = " + k + " ("
						+ TCS_Status.codeString(k) + ")");
		}
		if (spy)
			spyLog.log(2, "ModalFilter: " + name + " Default Key  = " + defaultValue + " ("
					+ TCS_Status.codeString(defaultValue) + ")");

		elements = values.size();
		counts = new int[elements];

		// Initialize the count array.
		for (int i = 0; i < elements; i++) {
			counts[i] = 0;
		}
	
		
	}

	/**
	 * Applies a modal filter to the buffered discrete Sensor readings.
	 * 
	 * @return The most common (modal) reading.
	 */
	@Override
	public Number filteredReading() {
		// Check we have any readings.

		if (buffer.size() == 0) {
			if (spy)
				spyLog.log(2, "ModalFilter: " + name + " Buffer size: " + buffer.size() + " Return default: "
						+ defaultValue);
			return new Integer(defaultValue);
		}

		// Initialize the count array.
		if (spy)
			spyLog.log(2, "ModalFilter:" + name + " initializing count array with " + counts.length + " cells");

		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}

		SensorReading reading = null;
		int ct = 0;
		int maxCount = 0;
		int maxKey = defaultValue;
		int index = 0;
		int keyVal = 0;
		Integer key = null;

		// Loop over buffered samples.
		// Find count index with largest size.
		Iterator it = buffer.iterator();
		while (it.hasNext()) {
			reading = (SensorReading) it.next();
			keyVal = reading.getDiscreteReading();
			key = new Integer(keyVal);

			if (!values.containsKey(key)) {
				if (spy)
					spyLog.log(2, "ModalFilter:" + name + " Valid[] does not contain a counting index for: " + keyVal
							+ " = [" + TCS_Status.codeString(keyVal) + "]");
				continue;
			}

			index = ((Integer) values.get(key)).intValue();
			if (spy)
				spyLog.log(2, "ModalFilter:" + name + " Checking Buffered Reading: " + keyVal + " at index: " + index
						+ " = " + TCS_Status.codeString(keyVal) + "]");
			ct = counts[index];
			ct++;
			counts[index] = ct;
			if (ct > maxCount) {
				maxCount = ct;
				maxKey = keyVal;
			}
		}

		if (spy)
			spyLog.log(2, "ModalFilter:" + name + " Returning: " + maxKey + " (" + TCS_Status.codeString(maxKey)
					+ ") From buffer with: " + buffer.size() + " Samples.");
		return new Integer(maxKey);

	}

}

/**
 * $Log: ModalFilter.java,v $ /** Revision 1.1 2006/12/12 08:31:16 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:35:17 snf /** Initial
 * revision /** /** Revision 1.1 2000/12/14 11:53:56 snf /** Initial revision
 * /**
 */

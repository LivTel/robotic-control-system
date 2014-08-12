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

import ngat.util.*;
import java.util.*;

public class OssStatus implements SerializableStatusCategory {

	long timeStamp;

	protected boolean schedulerOnline;

	private boolean baseModelsOnline;

	private boolean synopticModelsOnline;

	/**
	 * @return the schedulerOnline
	 */
	public boolean isSchedulerOnline() {
		return schedulerOnline;
	}

	/**
	 * @param schedulerOnline
	 *            the schedulerOnline to set
	 */
	public void setSchedulerOnline(boolean schedulerOnline) {
		this.schedulerOnline = schedulerOnline;
	}

	/**
	 * @return the baseModelsOnline
	 */
	public boolean isBaseModelsOnline() {
		return baseModelsOnline;
	}

	/**
	 * @param baseModelsOnline
	 *            the baseModelsOnline to set
	 */
	public void setBaseModelsOnline(boolean baseModelsOnline) {
		this.baseModelsOnline = baseModelsOnline;
	}

	/**
	 * @return the synopticModelsOnline
	 */
	public boolean isSynopticModelsOnline() {
		return synopticModelsOnline;
	}

	/**
	 * @param synopticModelsOnline
	 *            the synopticModelsOnline to set
	 */
	public void setSynopticModelsOnline(boolean synopticModelsOnline) {
		this.synopticModelsOnline = synopticModelsOnline;
	}

	/**
	 * Implementors should return status identified by the supplied key or throw
	 * an IllegalArgumentException if no such status exists. This method is
	 * intended for descriptive (String) status variables.
	 */
	public String getStatusEntryId(String key) throws IllegalArgumentException {
		if (key.equals("scheduler.online"))
			return (schedulerOnline ? "ONLINE" : "OFFLINE");
		else if (key.equals("base.models.online"))
			return (baseModelsOnline ? "ONLINE" : "OFFLINE");
		else if (key.equals("synoptic.models.online"))
			return (synopticModelsOnline ? "ONLINE" : "OFFLINE");
		else
			throw new IllegalArgumentException("No ID status for: " + key);
	}

	/**
	 * Implementors should return status identified by the supplied key or throw
	 * an IllegalArgumentException if no such status exists. This method is
	 * intended for continuous status variables.
	 */
	public int getStatusEntryInt(String key) throws IllegalArgumentException {
		throw new IllegalArgumentException("No int values available for internal status");
	}

	/**
	 * Implementors should return status identified by the supplied key or throw
	 * an IllegalArgumentException if no such status exists. This method is
	 * intended for discrete status variables.
	 */
	public double getStatusEntryDouble(String key) throws IllegalArgumentException {
		throw new IllegalArgumentException("No double status for: " + key);
	}

	/**
	 * Implementors should return status identified by the supplied key or throw
	 * an IllegalArgumentException if no such status exists. No type conversion
	 * should be attempted.
	 */
	public String getStatusEntryRaw(String key) throws IllegalArgumentException {
		throw new IllegalArgumentException("No raw values available for OSS status");
	}

	/** Set the timesatmp. */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/** Implementors should return the timestamp of the latest readings. */
	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public String toString() {
		return "OSS Status: At: " + (new Date(timeStamp)) + ": " + "Scheduler: "
				+ (schedulerOnline ? "ONLINE" : "OFFLINE") + " BaseModels: " + (baseModelsOnline ? "ONLINE" : "OFFLINE")
				+ " SynopticModels: " + (synopticModelsOnline ? "ONLINE" : "OFFLINE");

	}

}

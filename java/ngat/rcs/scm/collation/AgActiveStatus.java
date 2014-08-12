package ngat.rcs.scm.collation;

import java.util.Hashtable;

import ngat.util.SerializableStatusCategory;

public class AgActiveStatus implements SerializableStatusCategory {

	/** Status time stamp. */
	private long timeStamp;

	/** Stores the status key/value pairs. */
	private Hashtable hash;

	private boolean online;

	private boolean active;

	private double temperature;

	private String name;

	/**
	 * @return the online
	 */
	public boolean isOnLine() {
		return online;
	}

	/**
	 * @param online
	 *            the online to set
	 */
	public void setOnLine(boolean onLine) {
		this.online = onLine;
		//hash.put("online", new Boolean(online));
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		//hash.put("active", new Boolean(active));
	}

	/**
	 * @return the temeprature
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * @param temeprature
	 *            the temeprature to set
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
		//hash.put("temperature", new Double(temperature));
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
		//hash.put("name", name);
	}

	/**
	 * @param timeStamp
	 *            the timeStamp to set
	 */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public double getStatusEntryDouble(String key) throws IllegalArgumentException {
		Object value = hash.get(key);
		if (value == null)
			throw new IllegalArgumentException("SkyStatus: Key: [" + key + "] not found");
		if (!(value instanceof Double))
			throw new IllegalArgumentException("SkyStatus: Key: [" + key + "] not a Double (" + value + ")");
		return ((Double) value).doubleValue();
	}

	public String getStatusEntryId(String key) throws IllegalArgumentException {
		throw new IllegalArgumentException("SkyStatus: There are no valid id keys");
	}

	public int getStatusEntryInt(String key) throws IllegalArgumentException {
		throw new IllegalArgumentException("SkyStatus: There are no valid int keys");
	}

	public String getStatusEntryRaw(String key) throws IllegalArgumentException {
		Object value = hash.get(key);
		if (value == null)
			throw new IllegalArgumentException("SkyStatus: Key: [" + key + "] not found");
		return value.toString();
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public String toString() {
		return "AgActiveStatus: "+name+", Online: "+online+", Active: "+active+", Temp: "+temperature;
	}

}

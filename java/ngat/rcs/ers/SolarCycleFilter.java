/**
 * 
 */
package ngat.rcs.ers;

import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.SolarCycleStatus;

/**
 * @author eng
 * 
 */
public class SolarCycleFilter extends DiscreteFilter {

	/** Where the site is. */
	private transient ISite site;

	/** Time before sunset to switch to night-time. */
	private long preSunsetOffset;

	/** Time before sunrise to switch to daytime. */
	private long preSunriseOffset;

	/** Astro calculator. */
	private transient AstrometrySiteCalculator astro;

	/** Solar calculator. */
	private transient SolarCalculator solar;

	/**
	 * @param site
	 */
	public SolarCycleFilter(String name, ISite site) {
		super(name);
		this.site = site;
	
		astro = new BasicAstrometrySiteCalculator(site);

		solar = new SolarCalculator();

	}

	/**
	 * @return Time before sunset to switch to nighttime.
	 */
	public long getPreSunsetOffset() {
		return preSunsetOffset;
	}

	/**
	 * @param preSunsetOffset
	 */
	public void setPreSunsetOffset(long preSunsetOffset) {
		this.preSunsetOffset = preSunsetOffset;
	}

	/**
	 * @return Time before sunrise to switch to daytime.
	 */
	public long getPreSunriseOffset() {
		return preSunriseOffset;
	}

	/**
	 * @param preSunriseOffset
	 */
	public void setPreSunriseOffset(long preSunriseOffset) {
		this.preSunriseOffset = preSunriseOffset;
	}

	/**
	 * @see ngat.rcs.ers.Filter#getFilterName()
	 */
	public String getFilterName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.newenv.Filter#filterUpdate(long, java.lang.Number)
	 */
	public int processUpdate(long time, int value) {

		try {
			// ignore number value, we just want the time.
			Coordinates sun = solar.getCoordinates(time);

			// work out if sun is rising or setting...
			double sunalt = astro.getAltitude(sun, time);

			if (sunalt > 0.0) {
				
				long ttsunset = astro.getTimeUntilNextSet(sun, 0.0, time);
				if (ttsunset < preSunsetOffset)
					return SolarCycleStatus.NIGHT_TIME;
				else
					return SolarCycleStatus.DAY_TIME;

			} else {
				
				long ttsunrise = astro.getTimeUntilNextRise(sun, 0.0, time);
				if (ttsunrise < preSunriseOffset)
					return SolarCycleStatus.DAY_TIME;
				else
					return SolarCycleStatus.NIGHT_TIME;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 0; // UNKNOWN
		}

	}
	
	public String toString() {
		return "SolarCycleFilter: " + getFilterName() + ", using:"+sourceDescription;
	}

}

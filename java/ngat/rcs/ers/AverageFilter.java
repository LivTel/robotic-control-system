/**
 * 
 */
package ngat.rcs.ers;

import java.util.List;
import java.util.Vector;

/**
 * @author eng
 * 
 */
public class AverageFilter extends ContinuousFilter {

	protected List<Sample> samples;

	protected long timespan;

	/**
	 * @param name The name of the filter.
	 * @param timespan The period to extract the average from.
	 */
	public AverageFilter(String name, long timespan) {
		super(name);
		this.timespan = timespan;
		samples = new Vector<Sample>();
	}

	@Override
	protected double processUpdate(long time, double dvalue) {
		samples.add(new Sample(time, dvalue));
		// remove samples older than (time-timespan)
		while (samples.size() > 0 && samples.get(0).time < time - timespan)
			samples.remove(0);

		double fvalue = 0.0;
		for (int i = 0; i < samples.size(); i++) {
			fvalue += samples.get(i).value.doubleValue();
		}
		fvalue /= Math.max(1.0, samples.size());
		return fvalue;

	}

	@Override
	public String toString() {
		return "AverageFilter: " + getFilterName() + " " + (timespan / 1000) + "s, using:"+sourceDescription;
	}

}

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
public class ExponentialAverageFilter extends ContinuousFilter {

	protected List<Sample> samples;

	protected long timespan;
	
	protected long decayTime;
	
	/**
	 * @param name The name of the filter.
	 * @param timespan The period to extract the average from.
	 * @param decayTime The decay constant.
	 */
	public ExponentialAverageFilter(String name, long timespan, long decayTime) {
		super(name);
		this.timespan = timespan;
		this.decayTime = decayTime;
		samples = new Vector<Sample>();
	}
	
	@Override
	protected double processUpdate(long time, double dvalue) {
		samples.add(new Sample(time, dvalue));
		// remove samples older than (time-timespan)
		while (samples.size() > 0 && samples.get(0).time < time - timespan)
			samples.remove(0);

		double fvalue = 0.0;
		double tvalue = 0.0;
		for (int i = 0; i < samples.size(); i++) {
			double dfvalue = samples.get(i).value.doubleValue();
			double stime = (samples.get(i).time);
			fvalue += dfvalue*Math.exp(-(time-stime)/decayTime);
			tvalue += Math.exp(-(time-stime)/decayTime);
		}
		
		return fvalue/tvalue;
	}
	
	@Override
	public String toString() {
		return "ExpAverageFilter: " + getFilterName() + " " + (timespan / 1000) + "s, tau: "+(decayTime/1000)+"s, using:"+sourceDescription;
	}
}

/**
 * 
 */
package ngat.rcs.ers;

import java.util.List;
import java.util.Vector;

import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class AverageFilter extends ContinuousFilter {

	protected List<Sample> samples;

	protected long timespan;
	
	/** Logger. */
	private transient LogGenerator slogger;

	/**
	 * @param name The name of the filter.
	 * @param timespan The period to extract the average from.
	 */
	public AverageFilter(String name, long timespan) 
	{
		super(name);
		this.timespan = timespan;
		samples = new Vector<Sample>();
		
		Logger alogger = LogManager.getLogger("ERS"); 
		slogger = alogger.generate().system("RCS")
				.subSystem("Reactive")
				.srcCompClass(this.getClass().getSimpleName())
				.srcCompId(name);
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
		
		slogger.create().info().level(3).block("processUpdate")
		.msg(String.format("%6s time=%tT timeSpan=%tT samplesize=%6d value=%6.2f average value=%6.2f \n",
			name, 
			time,
			timespan,
			samples.size(),
			dvalue,
			fvalue)).send();
		
		return fvalue;

	}

	@Override
	public String toString() {
		return "AverageFilter: " + getFilterName() + " " + (timespan / 1000) + "s, using:"+sourceDescription;
	}

}

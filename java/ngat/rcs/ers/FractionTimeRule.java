/**
 * 
 */
package ngat.rcs.ers;

import java.util.List;
import java.util.Vector;

/** A rule for which the criterion must be satisfied for a specified fraction of a measured stability period.
 * @author eng
 *
 */
public class FractionTimeRule implements Rule {

	/** Name of the rule.*/
	protected String name;
	
	/** The length of time over which the criterion is measured.*/
	protected long stabilityPeriod;
	
	/** The fraction of the stabilityPeriod for which the criterion must be satisfied.*/
	protected double stabilityFraction;
	
	protected int countStableSamples;
	
	protected long firstSampleTime;
	
	protected List<Boolean> samples;
	
	/**
	 * @param name
	 * @param stabilityPeriod
	 * @param stabilityFraction
	 */
	public FractionTimeRule(String name, long stabilityPeriod, double stabilityFraction) {
		super();
		this.name = name;
		this.stabilityPeriod = stabilityPeriod;
		this.stabilityFraction = stabilityFraction;
		
		samples = new Vector<Boolean>();
		countStableSamples = 0;
	}

	/** 
	 * @return filter name.
	 * @see ngat.rcs.ers.Rule#getRuleName()
	 */
	public String getRuleName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Rule#ruleUpdate(long, boolean)
	 */
	public double ruleUpdate(long time, boolean criterion) {
	
		samples.add(criterion);
		
		if (samples.size() == 1) 
			firstSampleTime = time;			
		
		// always add 1 to count if this crit is ok
		countStableSamples += (criterion ? 1:0);
		
		// if sample buffer is full, lose stuff of the front and adjust start time
		long meanSampleInterval = (long)((double)(time-firstSampleTime)/(double)samples.size());
		if (time - firstSampleTime > stabilityPeriod) {
					
			firstSampleTime += meanSampleInterval;
			boolean firstSampleCriterion = samples.get(0).booleanValue();
			// sub 1 from count if first sample crit was ok and thus was counted
			countStableSamples -= (firstSampleCriterion ? 1:0);		
			samples.remove(0);
		}
		
		// work out the rule trigger value: rtv = ns*msi > frac*fulltime
		double actualCritSamples = (double)countStableSamples*(double)meanSampleInterval;
		double requiredCritSamples = stabilityPeriod*stabilityFraction;
		
		return Math.min(1.0, actualCritSamples/requiredCritSamples);
				
	}

}

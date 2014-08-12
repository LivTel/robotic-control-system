/**
 * 
 */
package ngat.rcs.ers.test;

import ngat.rcs.ers.AverageFilter;
import ngat.rcs.ers.Criterion;
import ngat.rcs.ers.FractionTimeRule;
import ngat.rcs.ers.LTCriterion;
import ngat.rcs.ers.Rule;
import ngat.rcs.ers.TimeRule;

/**
 * @author eng
 *
 */
public class SimpleTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// setup 1 filter with 30 second averaging
		AverageFilter avf = new AverageFilter("AV_HUM", 30*1000L);
		
		// Add a criterion for f value < 70%
		Criterion le70 = new LTCriterion("LT_70", 0.7);

		// And a rule based on this criterion for 10 minutes
		Rule r5m = new TimeRule("T10", 10*60*1000L);

		// Add a criterion for f value < 10%
		Criterion le10 = new LTCriterion("LT_10", 0.1);
		
		// Another rule which uses a 20% fractional crit
		Rule r80 = new FractionTimeRule("F20", 10*60*1000L, 0.2);
		
		// now lets start updating the filter
		
		//long start = System.currentTimeMillis();
		long start =0;
		long time = start;
		
		double tau = 20*60*1000.0;
		
		while (time < start + 65*60*1000L) {
			
			// fake sensor value is sinusoidal
			double t = time;
			double s = 0.5*(1.0 + Math.cos(2*Math.PI*t/tau)) + Math.random()*0.25-0.125  ;
		
			// update filter with latest sensor sample (S)
			double fvalue = avf.filterUpdate(time, s);
			
			// update crit with latest filter output (F)
			boolean cvalue = le70.criterionUpdate(time, fvalue);
			
			// update rule with criterion signal (C) (may change this to a fuzzy value in (0,1))
			double rvalue = r5m.ruleUpdate(time, cvalue);
			
			// update crit with latest filter output (F)
			boolean cvalue2 = le10.criterionUpdate(time, fvalue);						
			
			// update rule with criterion signal (C) (may change this to a fuzzy value in (0,1))
			double rvalue2 = r80.ruleUpdate(time, cvalue2);
			
			System.err.printf("At time (sec): %6.2f : %4.2f %4.2f  %1d %4.2f %4.2f \n", (t/1000.0), s, fvalue, 
					(cvalue2 ? 1:0), rvalue, rvalue2);
			
			time += 10000; // 10 second sensor updates
		}
		
		
	}

}

/**
 * 
 */
package ngat.rcs.ers.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ngat.rcs.ers.Rule;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class Conjunct implements Rule {

	private String name;

	Map<String, Double> ruleValues;
	
	/** Logger. */
	private transient LogGenerator slogger;
	
	/**
	 * @param name
	 */
	public Conjunct(String name) {
		this.name = name;
		ruleValues = new HashMap<String, Double>();
		
		Logger alogger = LogManager.getLogger("ERS"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("Reactive")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId(name);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.newenv.Rule#getRuleName()
	 */
	public String getRuleName() {
		return name;
	}

	/**
	 * Update the rule's criterion. In this case it makes little sense, the
	 * criterion is the average of all the criteria of its subrules. We just
	 * ignore the value of criterion and check subrules recorded states.
	 * 
	 * @see ngat.rcs.ers.Rule#ruleUpdate(long, boolean)
	 */
	public double ruleUpdate(long time, boolean criterion) {
		double output = 0.0;
		Iterator<String> ir = ruleValues.keySet().iterator();
		while (ir.hasNext()) {
			String subruleName = ir.next();
			double subruleValue = ruleValues.get(subruleName);
			output += subruleValue;
		}
		if (ruleValues.size() == 0)
			return 0.0;
		// normally the average of the subrule values but could be weighted
		output /= (ruleValues.size());
	
		slogger.create().info().level(3).block("ruleUpdate")
			.msg(String.format("%6s t=%tT out=%4.2f \n",name, time, output))
			.send();
		
		return output;
	}
	
	public double subruleUpdate(String ruleName, long time, double value) {
		ruleValues.put(ruleName, value);
		double output = 0.0;
		Iterator<String> ir = ruleValues.keySet().iterator();
		while (ir.hasNext()) {
			String subruleName = ir.next();
			double subruleValue = ruleValues.get(subruleName);
			output += subruleValue;
		}
		if (ruleValues.size() == 0)
			return 0.0;
		// normally the average of the subrule values but could be weighted
		output /= (ruleValues.size());
		
		slogger.create().info().level(3).block("subruleUpdate")
			.msg(String.format("%6s t=%tT out=%4.2f \n",name, time, output))
			.send();
		return output;
		
	}
	
	/**
	 * Add a feed-in subrule for this conjunct.
	 * 
	 * @param subrule
	 *            The subrule which feeds in.
	 */
	public void addSubrule(Rule subrule) {
		ruleValues.put(subrule.getRuleName(), new Double(0.0));
	}

	public String toString() {return "CONJUNCT: "+name;}
	
}

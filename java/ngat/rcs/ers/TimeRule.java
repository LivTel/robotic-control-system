/**
 * 
 */
package ngat.rcs.ers;



import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class TimeRule implements Rule {
	
	/** Name of the rule.*/
	protected String name;
	
	/**The length of time the criterion must be satisfied for.*/
	protected long stabilityPeriod;
	
	/** True if the criterion is satisfied.*/
	protected boolean criterionAlreadySatisfied;
	
	/** Time at which the criterion first became satisfied.*/
	protected long criterionStabilityStart;
	
	/** Logger. */
	private transient LogGenerator slogger;

	/**
	 * @param stabilityPeriod
	 */
	public TimeRule(String name, long stabilityPeriod) {
		super();
		this.name = name;
		this.stabilityPeriod = stabilityPeriod;
		criterionAlreadySatisfied = false;
		
		Logger alogger = LogManager.getLogger("ERS"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("Reactive")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId(name);
		
	}


	public String getRuleName() {
		return name;
	}


	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Rule#ruleUpdate(long, boolean)
	 */
	public double ruleUpdate(long time, boolean criterion) {
		
		
		//boolean trigger = false;
		double triglevel = 0.0;
		
		if (criterionAlreadySatisfied) {
			if (criterion) {
				if (time - criterionStabilityStart > stabilityPeriod)
					//trigger = true;
				triglevel = 1.0;
				else 
					triglevel = (double)(time - criterionStabilityStart)/(double)stabilityPeriod;
			} else {
				//trigger = false;
				triglevel = 0.0;
			}
		} else {
			if (criterion) {
				criterionStabilityStart = time;
				//trigger = false;
				triglevel = 0.0;
			} else {
				//trigger = false;
				triglevel = 0.0;
			}
		}
				
		criterionAlreadySatisfied = criterion; 
	
		slogger.create().info().level(3).block("ruleUpdated")
			.msg(String.format("%6s t=%tT cas=%4b cv=%4b css=%tT sp=%6d dt=%6d trig=%4.2f \n",
				name, 
				time, 
				criterionAlreadySatisfied,
				criterion,
				criterionStabilityStart, 
				(stabilityPeriod/1000),
				((time-criterionStabilityStart)/1000),
				triglevel)).send();
		
		
		return triglevel;
	}

    @Override
	public String toString() {
	return "TimedRule: "+name+" Stability-period: "+(stabilityPeriod/1000)+"s";
    }

}

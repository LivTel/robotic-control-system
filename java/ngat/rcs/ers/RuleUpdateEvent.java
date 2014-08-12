/**
 * 
 */
package ngat.rcs.ers;

/**
 * @author eng
 *
 */
public class RuleUpdateEvent extends ReactiveEvent {

	private String ruleName;
	
	private boolean ruleTriggered;

	/**
	 * @param statusTimeStamp
	 * @param ruleName
	 * @param ruleTriggered
	 */
	public RuleUpdateEvent(long statusTimeStamp, String ruleName,
			boolean ruleTriggered) {
		super(statusTimeStamp);
		this.ruleName = ruleName;
		this.ruleTriggered = ruleTriggered;
	}

	public String getRuleName() {
		return ruleName;
	}

	public boolean isRuleTriggered() {
		return ruleTriggered;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString()+", Rule: "+ruleName+", Triggered: "+ruleTriggered;
	}
	
	

}

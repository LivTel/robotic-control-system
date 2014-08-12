/**
 * 
 */
package ngat.rcs.ers;

/**
 * @author eng
 *
 */
public class CriterionUpdateEvent extends ReactiveEvent {

	private String criterionName;
	
	private boolean criterionOutput;

	/**
	 * @param statusTimeStamp
	 * @param criterionOutput
	 */
	public CriterionUpdateEvent(long statusTimeStamp, String criterionName, boolean criterionOutput) {
		super(statusTimeStamp);
		this.criterionName = criterionName;
		this.criterionOutput = criterionOutput;
	}

	public String getCriterionName() {
		return criterionName;
	}

	public boolean isCriterionOutput() {
		return criterionOutput;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString()+", Criterion: "+criterionName+", Criterion: "+criterionOutput;
	}
	
	

}

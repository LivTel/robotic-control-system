/**
 * 
 */
package ngat.rcs.ers;

/** Criterion for LessTahn test.
 * @author eng
 *
 */
public class LTCriterion implements Criterion {

	private String name;
	
	private double maximum;

	/**
	 * @param maximum
	 */
	public LTCriterion(String name, double maximum) {
		super();
		this.name = name;
		this.maximum = maximum;
	}

	public String getCriterionName() {
		return name;
	}
	
	public boolean criterionUpdate(long time, double dvalue) {
		return criterionUpdate(time, new Double(dvalue));
	}
	
	public boolean criterionUpdate(long time, Number value) {
		return (value.doubleValue() < maximum);
	}
	
	public String toString() { return "LT "+name+" < "+maximum; } 
	
}

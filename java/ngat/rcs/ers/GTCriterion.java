/**
 * 
 */
package ngat.rcs.ers;

/** Criterion for GreaterThan test.
 * @author eng
 *
 */
public class GTCriterion implements Criterion {

	private String name;
	
	private double minimum;
	
	/**
	 * @param minimum
	 */
	public GTCriterion(String name, double minimum) {
		super();
		this.name = name;
		this.minimum = minimum;
	}

	public String getCriterionName() {
		return name;
	}
	
	public boolean criterionUpdate(long time, double dvalue) {
		return criterionUpdate(time, new Double(dvalue));
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Criterion#criterionUpdate(long, java.lang.Number)
	 */
	public boolean criterionUpdate(long time, Number value) {
		return (value.doubleValue() > minimum);	
	}

	public String toString() { return "GT "+name+" > "+minimum; } 

}

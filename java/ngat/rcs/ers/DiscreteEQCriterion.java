/**
 * 
 */
package ngat.rcs.ers;

/** Criterion to test if a sample is Equal to the specified value.
 * @author eng
 *
 */
public class DiscreteEQCriterion implements Criterion {

	private String name;
	
	private int testValue;
	
	/**
	 * @param name
	 * @param testValue
	 */
	public DiscreteEQCriterion(String name, int testValue) {
		super();
		this.name = name;
		this.testValue = testValue;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Criterion#getCriterionName()
	 */
	public String getCriterionName() {
		return name;
	}

	public boolean criterionUpdate(long time, int ivalue) {
		return criterionUpdate(time, new Integer(ivalue));
	}
	
	
	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Criterion#criterionUpdate(long, java.lang.Number)
	 */
	public boolean criterionUpdate(long time, Number value) {
		return (value.intValue() == testValue);
	}
	
	public String toString() { return "DEQ "+name+" = "+testValue; } 

}

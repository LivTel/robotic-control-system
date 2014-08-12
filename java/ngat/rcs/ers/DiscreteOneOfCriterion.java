/**
 * 
 */
package ngat.rcs.ers;

/** Criterion which triggers if the supplied value matches any of a set of specified values.
 * @author eng
 *
 */
public class DiscreteOneOfCriterion implements Criterion {

	private String name;
	
	private int[] testValues;
	
	
	/**
	 * @param name
	 * @param testValue
	 */
	public DiscreteOneOfCriterion(String name, int[] testValues) {
		super();
		this.name = name;
		this.testValues = testValues;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Criterion#getCriterionName()
	 */
	public String getCriterionName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Criterion#criterionUpdate(long, java.lang.Number)
	 */
	public boolean criterionUpdate(long time, Number value) {
		for (int i = 0; i < testValues.length; i++) {
			if (value.intValue() == testValues[i])
				return true;
		}
		return false;
	}
	
	public String toString() { return "ANY "+name+" of "+testValues.length+" values"; } 

}

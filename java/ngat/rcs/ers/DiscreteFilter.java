/**
 * 
 */
package ngat.rcs.ers;

/**
 * @author eng
 *
 */
public abstract class DiscreteFilter implements Filter {

	protected String name;
	
	protected int ivalue;
	
	protected String filterDescription;
	
	protected String sourceName;
	
	protected String sourceDescription;
	
	/**
	 * @param name
	 */
	public DiscreteFilter(String name) {
		super();
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getFilterName() {
		return name;
	}

	public String getFilterDescription() {
		return filterDescription;
	}

	public void setFilterDescription(String filterDescription) {
		this.filterDescription = filterDescription;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getSourceDescription() {
		return sourceDescription;
	}

	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Filter#filterUpdate(long, java.lang.Number)
	 */
	public Number filterUpdate(long time, Number value) {
		ivalue = value.intValue();
		return processUpdate(time, ivalue);
	}

	protected abstract int processUpdate(long time, int ivalue);
	
}

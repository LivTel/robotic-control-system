/**
 * 
 */
package ngat.rcs.ers;

/**
 * @author eng
 *
 */
public abstract class ContinuousFilter implements Filter {

	protected String name;
	
	protected double dvalue;
	
	protected String filterDescription;
	
	protected String sourceName;
	
	protected String sourceDescription;
	
	/**
	 * @param name
	 */
	public ContinuousFilter(String name) {
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

	public double filterUpdate(long time, double dvalue) {
		return filterUpdate(time, new Double(dvalue)).doubleValue();
	}
	
	/* (non-Javadoc)
	 * @see ngat.rcs.newenv.Filter#filterUpdate(long, java.lang.Number)
	 */
	public Number filterUpdate(long time, Number value) {
		dvalue = value.doubleValue();
		return processUpdate(time, dvalue);
	}
	
	protected abstract double processUpdate(long time, double dvalue);

}

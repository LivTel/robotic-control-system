/**
 * 
 */
package ngat.rcs.ers;

/**
 * @author eng
 *
 */
public class FilterUpdateEvent extends ReactiveEvent {

	private String filterName;
	
	private Number sensorInput;
	
	private Number filterOutput;

	/**
	 * @param statusTimeStamp
	 * @param sensorInput
	 * @param filterOutput
	 */
	public FilterUpdateEvent(long statusTimeStamp, String filterName, Number sensorInput,
			Number filterOutput) {
		super(statusTimeStamp);
		this.filterName = filterName;
		this.sensorInput = sensorInput;
		this.filterOutput = filterOutput;
	}

	
	
	public String getFilterName() {
		return filterName;
	}



	public Number getSensorInput() {
		return sensorInput;
	}

	public Number getFilterOutput() {
		return filterOutput;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString()+", Filter: "+filterName+", Input: "+sensorInput.doubleValue()+", Output: "+filterOutput.doubleValue();
	}
	
	

}

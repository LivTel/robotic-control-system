/**
 * 
 */
package ngat.rcs.calib;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public class MorningSkyFlatCalibration extends CalibrationOperation {

	private InstrumentDescriptor id;

	/** Create a MorningSkyFlatCalibration.
	 * @param score The score.
	 * @param id The descriptor of the instrument involved.
	 */
	public MorningSkyFlatCalibration(InstrumentDescriptor id, double score) {
		super(score);
		this.id = id;
	}
	
	/**
	 * @return the instrument descriptor.
	 */
	public InstrumentDescriptor getInstrumentDescriptor() {
		return id;
	}
	
	@Override
	public String toString() {
			return "MSF:"+id.getInstrumentName()+", s="+getScore();
	}
	
}

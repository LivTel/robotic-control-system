/**
 * 
 */
package ngat.rcs.calib;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public class EveningSkyFlatCalibration extends CalibrationOperation {

	private InstrumentDescriptor id;

	/**
	 * @param score
	 * @param id
	 */
	public EveningSkyFlatCalibration(InstrumentDescriptor id, double score) {
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
		return "ESF:"+id.getInstrumentName()+", s="+getScore();
}
}

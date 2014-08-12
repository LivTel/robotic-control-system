/**
 * 
 */
package ngat.rcs.calib;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public class TelFocusCalibration extends CalibrationOperation {

	private InstrumentDescriptor focusInstId;

	/**
	 * @param score
	 * @param focusInstId
	 */
	public TelFocusCalibration(InstrumentDescriptor focusInstId, double score)  {
		super(score);
		this.focusInstId = focusInstId;
	}
	
	/**
	 * @return the instrument descriptor.
	 */
	public InstrumentDescriptor getInstrumentDescriptor() {
		return focusInstId;
	}
	
	@Override
	public String toString() {
		return "TELFOC:"+focusInstId.getInstrumentName()+", s="+getScore();
}
}

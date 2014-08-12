/**
 * 
 */
package ngat.rcs.calib;

/** A calibration operation - nominally a Task.
 * @author eng
 *
 */
public class CalibrationOperation {

	private double score;

	/**
	 * @param score
	 */
	public CalibrationOperation(double score) {
		super();
		this.score = score;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}
	
	
	
}

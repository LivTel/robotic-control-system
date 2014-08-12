/**
 * 
 */
package ngat.rcs.test;

import ngat.rcs.iss.FITS_HeaderInfo;

/**
 * @author eng
 *
 */
public class SetTargetHeaders {

	/** Set target headers to see if we get a crash
	 * @param args
	 */
	public static void main(String[] args) {
	
		FITS_HeaderInfo.fillFitsTargetHeaders(null);

	}

}

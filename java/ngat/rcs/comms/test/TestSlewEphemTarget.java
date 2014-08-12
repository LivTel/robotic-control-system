/**
 * 
 */
package ngat.rcs.comms.test;

import ngat.message.RCS_TCS.SLEW;
import ngat.phase2.EphemerisSource;
import ngat.rcs.comms.LT_RGO_TCS_CommandTranslatorFactory;

/**
 * @author eng
 *
 */
public class TestSlewEphemTarget {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			System.setProperty("astrometry.impl", "ngat.astrometry.TestCalculator");
			System.setProperty("TCS_MODE", "SYS_AUTO");
			
			EphemerisSource src = new EphemerisSource("Track");
			src.clearEphemeris();
			
			long start= System.currentTimeMillis();
			long t = start;
			double ra = Math.random()*2.0*Math.PI;
			double dec = (Math.random()-0.5)*Math.PI;
			double rr = Math.toRadians((Math.random()-0.5)/60);
			double dr = Math.toRadians((Math.random()-0.5)/60);
			System.err.println("I shall be using rates of: "+
					(240.0*Math.toDegrees(rr))+"s/s, "+(3600.0*Math.toDegrees(dr))+"as/s");
			while (t < start+3600*1000L) {
			
				ra += rr*300;
				dec += dr*300;
				
				src.addCoordinate(t, ra, dec, rr, dr);
				t += 5*60*1000L; // 5 minutes
			}
			
			
			SLEW slew = new SLEW("test");
			slew.setNstrack(true);
			slew.setSource(src);
			
			LT_RGO_TCS_CommandTranslatorFactory trans = LT_RGO_TCS_CommandTranslatorFactory.getInstance();
			
			String done = (String)trans.translateCommand(slew);
			
			System.err.println("EXEC: "+done);
			
		} catch (Exception e) {
				e.printStackTrace();
		}

	}

}

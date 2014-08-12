/**
 * 
 */
package ngat.rcs.test;

import ngat.astrometry.BasicCardinalPointingCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.astrometry.Position;
import ngat.instrument.CCD;
import ngat.phase2.ExtraSolarSource;
import ngat.phase2.XExtraSolarTarget;
//import ngat.rcs.tmm.CardinalPointingCalculator;

/** Compare the TMM and New Astro Cardinal Pointing calculators.
 * @author eng
 *
 */
public class CompareCPCalculators {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
		double IOFF = Math.toRadians(70.0);
		
		double ra = Math.random()*Math.PI*2.0;
		double dec = (Math.random() - 0.5)*Math.PI;
		
		double LAT = Math.toRadians(28.0);
		double LON = Math.toRadians(-17.0);
		
		ISite site = new BasicSite("obs", LAT, LON);
		Position.setViewpoint(LAT, LON);
		
		// offset +/- 90
		double instOffset = (Math.random() - 0.5)*Math.PI;
		
		long t1 = System.currentTimeMillis();
		long t2 = t1 + 30*60*1000L;
		
		// NEW
		XExtraSolarTarget target = new XExtraSolarTarget("test");
		target.setRa(ra);
		target.setDec(dec);
		
		ngat.astrometry.CardinalPointingCalculator cpnew = new BasicCardinalPointingCalculator(site);
		
		double snew = cpnew.getBestCardinalAngle(target, IOFF-instOffset, t1, t2);
		
		// OLD
		ExtraSolarSource src = new ExtraSolarSource("test");
		src.setRA(ra);
		src.setDec(dec);
		
		CCD ccd = new CCD("imager");
		ccd.setRotatorOffset(IOFF-instOffset);
		
		//CardinalPointingCalculator cpold = new CardinalPointingCalculator();
		
		//double sold = cpold.getBestCardinalPointingAngle(src, ccd, t1, t2);
		
		/*System.err.printf("Cardinal solution for: (%s, %s) offset: %4.2f  O= %4.2f N= %4.2f \n ", 
				AstroFormatter.formatHMS(ra,":"),
				AstroFormatter.formatDMS(dec,":"),
				Math.toDegrees(instOffset), 
				Math.toDegrees(sold), 
				Math.toDegrees(snew));
		*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}

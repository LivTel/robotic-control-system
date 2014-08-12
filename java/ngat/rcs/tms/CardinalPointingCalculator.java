package ngat.rcs.tms;

import ngat.phase2.*;
import ngat.astrometry.*;
import ngat.util.logging.*;
import ngat.instrument.*;
import ngat.rcs.*;

public class CardinalPointingCalculator {

    /**
     * Add to limits as a buffer against uncertain sky->mount conversion near
     * limits (degs).
     */
    static final double ROTATOR_LIMIT_BUFFER = 4.0;

    static Logger taskLog = LogManager.getLogger("TASK");

    static final String CLASS = "CPCalculator";

    /**
     * Returns true if the nominated cardinal pointing sky angle is feasible for
     * the given target and instrument offset for the period t1 thro t2.
     * 
     * @param skyAngle
     *            The Cardinal point sky angle to test (rads).
     * @param source
     *            The target which will be tracked at that angle.
     * @param instrumentOffset
     *            Instrument rotator offset.
     * @param t1
     *            Start of tracking period.
     * @param t2
     *            End of tracking period.
     */
    public static boolean isFeasibleCardinalPoint(double skyAngle, Source source, Instrument instrument, long t1,
						  long t2) {

	long now = System.currentTimeMillis();

	double latitude = RCS_Controller.getLatitude();
	double domeLimit = RCS_Controller.getDomelimit();

	// Work out the altaz at start and end of observation.
	// TODO this requires src.getPosition(t) which does not exist for most
	// targets !
	Position targetStart = source.getPosition();
	double dec = targetStart.getDec();
	double azm1 = targetStart.getAzimuth(t1);
	double alt1 = targetStart.getAltitude(t1);

	Position targetEnd = source.getPosition();
	double azm2 = targetEnd.getAzimuth(t2);
	double alt2 = targetEnd.getAltitude(t2);

	// Can we see the bugger ?
	if (alt1 < domeLimit) {
	    taskLog.log(2, CLASS, "-", "isFeasibleCardinalPoint", "CP routine: Start position: Target Azm = "
			+ Position.toDegrees(azm1, 2) + ", Alt = " + Position.toDegrees(alt1, 2) + " TARGET IS SET");
	    return false;
	}

	// Will we see the bugger ?
	if (alt2 < domeLimit) {
	    taskLog.log(2, CLASS, "-", "iFeasibleCardinalPoint", "CP routine: End position: Target Azm = "
			+ Position.toDegrees(azm2, 2) + ", Alt = " + Position.toDegrees(alt2, 2) + " TARGET WILL SET");
	    return false;
	}

	taskLog.log(2, CLASS, "-", "iFeasibleCardinalPoint", "CP routine: Start position: Target Azm = "
		    + Position.toDegrees(azm1, 2) + ", Alt = " + Position.toDegrees(alt1, 2)
		    + ", End position: Target Azm = " + Position.toDegrees(azm2, 2) + ", Alt = "
		    + Position.toDegrees(alt2, 2));

	// transform to [0-360] acw from south for calcBearing.
	double baz1 = azm1;
	if (azm1 < Math.PI)
	    baz1 = Math.PI - azm1;
	else
	    baz1 = 3.0 * Math.PI - azm1;

	// calls SlaDbear(az1, alt1, az2, alt2)
	double p = JSlalib.calcBearing(baz1, alt1, 0.0, latitude);

	// transform to [0-360] acw from south for calcBearing.
	double baz2 = azm2;
	if (azm2 < Math.PI)
	    baz2 = Math.PI - azm2;
	else
	    baz2 = 3.0 * Math.PI - azm2;

	// calls SlaDbear(az1, alt1, az2, alt2)
	double p2 = JSlalib.calcBearing(baz2, alt2, 0.0, latitude);

	double instOffset = instrument.getRotatorOffset();
	taskLog.log(2, CLASS, "-", "iFeasibleCardinalPoint", "CP routine: Using instrument rotator offset: "
		    + Position.toDegrees(instOffset, 2));

	double maxDtl = -999; // detect maximum Dist-to-limit

	// Checking for valid CPs

	double pc = p + instOffset + skyAngle; // add instrument rotation
	// correction and sky PA
	double pd = p2 + instOffset + skyAngle; // add instrument rotation
	// correction and sky PA

	// back into range: if inst-offset is ever negative this might not
	// work...may need to upwards correct

	// 1. T1

	// correct angles
	while (pc > Math.toRadians(240))
	    pc -= 2.0 * Math.PI;

	while (pc < Math.toRadians(-240))
	    pc += 2.0 * Math.PI;

	double pc2 = pc;
	System.err.println("CP routine: Start of obs...");

	// calculate alternative wrap angles...
	if (Math.toDegrees(pc) < -120.0) {
	    pc2 = pc + 2.0 * Math.PI;

	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R1 = "+Position.toDegrees(pc,3)+" R2 =  "+Position.toDegrees(pc2,3));
	} else if (Math.toDegrees(pc) > 120.0) {
	    pc2 = pc - 2.0 * Math.PI;
	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R1 = "+Position.toDegrees(pc,3)+" R2 = "+Position.toDegrees(pc2,3));
	} else {
	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R = "+Position.toDegrees(pc,3));
	}

	// 2. T2

	while (pd > Math.toRadians(240))
	    pd -= 2.0 * Math.PI;

	while (pd < Math.toRadians(-240))
	    pd += 2.0 * Math.PI;

	double pd2 = pd;
	System.err.println("CP routine: End of obs...");

	// calculate alternative wrap angles...
	if (Math.toDegrees(pd) < -120.0) {
	    pd2 = pd + 2.0 * Math.PI;

	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R1 = "+Position.toDegrees(pd,3)+" R2 =  "+Position.toDegrees(pd2,3));
	} else if (Math.toDegrees(pd) > 120.0) {
	    pd2 = pd - 2.0 * Math.PI;
	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R1 = "+Position.toDegrees(pd,3)+" R2 = "+Position.toDegrees(pd2,3));
	} else {
	    System.err.println("CP routine: Sky: "+Position.toDegrees(skyAngle,2)+
			       " R = "+Position.toDegrees(pd,3));
	}

	// work out if pc or pc2 is ok and if pd or pd2 is ok within limits..
	boolean pcok = false;
	boolean pc2ok = false;
	boolean pdok = false;
	boolean pd2ok = false;
	// TODO replace hard-coded +-90 with reduced angles (-85, +85)??
	if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pc) && Math.toDegrees(pc) < 90 - ROTATOR_LIMIT_BUFFER)
	    pcok = true;

	if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pc2) && Math.toDegrees(pc2) < 90 - ROTATOR_LIMIT_BUFFER)
	    pc2ok = true;

	if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pd) && Math.toDegrees(pd) < 90 - ROTATOR_LIMIT_BUFFER)
	    pdok = true;

	if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pd2) && Math.toDegrees(pd2) < 90 - ROTATOR_LIMIT_BUFFER)
	    pd2ok = true;

	double d = 0.0; // distance to limit

	if ((pcok || pc2ok) && (pdok || pd2ok)) {
	    // System.err.println("CP routine: SKY "+Position.toDegrees(skyAngle,2)+" is VALID cardinal point");

	    // work out which end point is nearest to a limit
	    // either pc to pd or pc2 to pd2? determine from dec which side of Z
	    // then decide
	    // on direction of rotation cw or acw
	    if (dec < latitude) {
		// rotator angle increasing (towards -90)

		double dd = Math.abs(Math.toDegrees(pd) + 90 - ROTATOR_LIMIT_BUFFER);
		double dd2 = Math.abs(Math.toDegrees(pd2) + 90 - ROTATOR_LIMIT_BUFFER);
		d = Math.min(dd, dd2);

		System.err.println("CP routine: Rotate -ve - Approaches within "+d+" of ACW limit");

	    } else {
		// rotator angle decreasing (towards +90)

		double dd = Math.abs(90 - Math.toDegrees(pd) - ROTATOR_LIMIT_BUFFER);
		double dd2 = Math.abs(90 - Math.toDegrees(pd2) - ROTATOR_LIMIT_BUFFER);
		d = Math.min(dd, dd2);

		System.err.println("CP routine: Rotate +ve - Approaches within "+d+" of CW limit");
	    }

	    if (d > 0.0)
		return true;

	} 
	System.err.println("CP routine: SKY " + Position.toDegrees(skyAngle, 2) + " is NOT usable");

	return false;

    }

    /**
     * Returns the best (i.e. longest time to limits) cardinal pointing sky
     * angle for the given target and instrument offset for the period t1 thro
     * t2.
     * 
     * @param source
     *            The target which will be tracked at that angle.
     * @param instrumentOffset
     *            Instrument rotator offset.
     * @param t1
     *            Start of tracking period.
     * @param t2
     *            End of tracking period.
     * @return The best cardinal point sky angle (rads).
     * @throws CardinalPointingSolutionException
     *             If a problem occurs.
     */
    public static double getBestCardinalPointingAngle(Source source, Instrument instrument, long t1, long t2)
	throws CardinalPointingSolutionException {

	long now = System.currentTimeMillis();

	double latitude = RCS_Controller.getLatitude();
	double domeLimit = RCS_Controller.getDomelimit();
	taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "Perform CP Calculation for Target: " + source
		    + " at Site with Lat: " + Position.toDegrees(latitude, 2) + " using Limit: "
		    + Position.toDegrees(domeLimit, 2));
	// Work out the altaz at start and end of observation.
	// TODO this requires src.getPosition(t) which does not exist for most
	// targets !
	Position targetStart = source.getPosition();
	double dec = targetStart.getDec();
	double azm1 = targetStart.getAzimuth(t1);
	double alt1 = targetStart.getAltitude(t1);

	Position targetEnd = source.getPosition();
	double azm2 = targetEnd.getAzimuth(t2);
	double alt2 = targetEnd.getAltitude(t2);

	// Can we see the bugger ?
	if (alt1 < domeLimit) {
	    taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "CP routine: Start position: Target Azm = "
			+ Position.toDegrees(azm1, 2) + ", Alt = " + Position.toDegrees(alt1, 2) + " TARGET IS SET");

	    // FAIL soas to alert mgr.
	    throw new CardinalPointingSolutionException("Target set at start of period");
	}

	// Will we see the bugger ?
	if (alt2 < domeLimit) {
	    taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "CP routine: End position: Target Azm = "
			+ Position.toDegrees(azm2, 2) + ", Alt = " + Position.toDegrees(alt2, 2) + " TARGET WILL SET");
	    throw new CardinalPointingSolutionException("Target likely to set before end of period");
	}

	taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "CP routine: Start position: Target Azm = "
		    + Position.toDegrees(azm1, 2) + ", Alt = " + Position.toDegrees(alt1, 2)
		    + ", End position: Target Azm = " + Position.toDegrees(azm2, 2) + ", Alt = "
		    + Position.toDegrees(alt2, 2));

	// transform to [0-360] acw from south for calcBearing.
	double baz1 = azm1;
	if (azm1 < Math.PI)
	    baz1 = Math.PI - azm1;
	else
	    baz1 = 3.0 * Math.PI - azm1;

	// calls SlaDbear(az1, alt1, az2, alt2)
	double p = JSlalib.calcBearing(baz1, alt1, Math.PI, latitude);
	//double p = getPara(source, t1);
	
	// transform to [0-360] acw from south for calcBearing.
	double baz2 = azm2;
	if (azm2 < Math.PI)
	    baz2 = Math.PI - azm2;
	else
	    baz2 = 3.0 * Math.PI - azm2;

	// calls SlaDbear(az1, alt1, az2, alt2)
	double p2 = JSlalib.calcBearing(baz2, alt2, Math.PI, latitude);
	//double p2 = getPara(source, t2);
	
	double instOffset = instrument.getRotatorOffset();
	taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "CP routine: Using instrument rotator offset: "
		    + Position.toDegrees(instOffset, 2));

	System.err.printf("CC: using ioff: %4.2f\n", Math.toDegrees(instOffset));
	
	double maxDtl = -999; // detect maximum Dist-to-limit
	double selectAngle = -1; // selected CP value
	int countValid = 0; // count valid CPs

	// Checking for valid CPs
	for (int isky = 0; isky < 4; isky++) {

	    double sky = Math.toRadians(isky * 90.0);

	    double pc = p + instOffset + sky; // add instrument rotation
	    // correction and sky PA
	    double pd = p2 + instOffset + sky; // add instrument rotation
	    // correction and sky PA

	    // back into range: if inst-offset is ever negative this might not
	    // work...may need to upwards correct

	    // 1. T1

	    // correct angles
	    while (pc > Math.toRadians(240))
		pc -= 2.0 * Math.PI;

	    while (pc < Math.toRadians(-240))
		pc += 2.0 * Math.PI;

	    double pc2 = pc;
	    System.err.println("CP routine: Start of obs...");

	    // calculate alternative wrap angles...
	    if (Math.toDegrees(pc) < -120.0) {
		pc2 = pc + 2.0 * Math.PI;

		System.err.println("CP routine: Sky: " + Position.toDegrees(sky, 2) + " R1 = "
				   + Position.toDegrees(pc, 3) + " R2 =  " + Position.toDegrees(pc2, 3));
	    } else if (Math.toDegrees(pc) > 120.0) {
		pc2 = pc - 2.0 * Math.PI;
		System.err.println("CP routine: Sky: " + Position.toDegrees(sky, 2) + " R1 = "
				   + Position.toDegrees(pc, 3) + " R2 = " + Position.toDegrees(pc2, 3));
	    } else {
		System.err.println("CP routine: Sky: " + Position.toDegrees(sky, 2) + " R = "
				   + Position.toDegrees(pc, 3));
	    }

	    // 2. T2

	    while (pd > Math.toRadians(240))
		pd -= 2.0 * Math.PI;

	    while (pd < Math.toRadians(-240))
		pd += 2.0 * Math.PI;

	    double pd2 = pd;
	    System.err.println("CP routine: End of obs...");

	    // calculate alternative wrap angles...
	    if (Math.toDegrees(pd) < -120.0) {
		pd2 = pd + 2.0 * Math.PI;

		System.err.println("CP routine: Sky1: " + Position.toDegrees(sky, 2) + " R1 = "
				   + Position.toDegrees(pd, 3) + " R2 =  " + Position.toDegrees(pd2, 3));
	    } else if (Math.toDegrees(pd) > 120.0) {
		pd2 = pd - 2.0 * Math.PI;
		System.err.println("CP routine: Sky2: " + Position.toDegrees(sky, 2) + " R1 = "
				   + Position.toDegrees(pd, 3) + " R2 = " + Position.toDegrees(pd2, 3));
	    } else {
		System.err.println("CP routine: Sky3: " + Position.toDegrees(sky, 2) + " R = "
				   + Position.toDegrees(pd, 3));
	    }

	    // work out if pc or pc2 is ok and if pd or pd2 is ok within
	    // limits..
	    boolean pcok = false;
	    boolean pc2ok = false;
	    boolean pdok = false;
	    boolean pd2ok = false;
	    if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pc) && Math.toDegrees(pc) < 90 - ROTATOR_LIMIT_BUFFER)
		pcok = true;

	    if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pc2) && Math.toDegrees(pc2) < 90 - ROTATOR_LIMIT_BUFFER)
		pc2ok = true;

	    if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pd) && Math.toDegrees(pd) < 90 - ROTATOR_LIMIT_BUFFER)
		pdok = true;

	    if (-90 + ROTATOR_LIMIT_BUFFER < Math.toDegrees(pd2) && Math.toDegrees(pd2) < 90 - ROTATOR_LIMIT_BUFFER)
		pd2ok = true;

	    if ((pcok || pc2ok) && (pdok || pd2ok)) {
		System.err.println("CP routine: SKY " + Position.toDegrees(sky, 2) + " IS VALID CARDINAL POINT");
		countValid++;

		// work out which end point is nearest to a limit
		// either pc to pd or pc2 to pd2? determine from dec which side
		// of Z then decide
		// on direction of rotation cw or acw
		if (dec < latitude) {
		    // rotator angle increasing (towards +90)

		    //double dd = Math.abs(Math.toDegrees(pd) + 90 - ROTATOR_LIMIT_BUFFER);
		    //double dd2 = Math.abs(Math.toDegrees(pd2) + 90 - ROTATOR_LIMIT_BUFFER);
		    
		    double dd = Math.abs( 90 - ROTATOR_LIMIT_BUFFER - Math.toDegrees(pd));
		    double dd2 = Math.abs( 90 - ROTATOR_LIMIT_BUFFER - Math.toDegrees(pd2));
			   
		    double d = Math.min(dd, dd2);
		    if (d > maxDtl) {
			selectAngle = sky;
			maxDtl = d;
		    }
		    System.err.println("CP routine: Approaches within " + d + " of ACW limit");

		} else {
		    // rotator angle decreasing (towards -90)

		    //double dd = Math.abs(90 - ROTATOR_LIMIT_BUFFER - Math.toDegrees(pd));
		    //double dd2 = Math.abs(90 - ROTATOR_LIMIT_BUFFER - Math.toDegrees(pd2));
		    double dd = Math.abs(Math.toDegrees(pd) - 90 + ROTATOR_LIMIT_BUFFER);
		    double dd2 = Math.abs(Math.toDegrees(pd2) - 90 + ROTATOR_LIMIT_BUFFER);
		    double d = Math.min(dd, dd2);
		    if (d > maxDtl) {
			selectAngle = sky;
			maxDtl = d;
		    }
		    System.err.println("CP routine: Approaches within " + d + " of CW limit");
		}

	    } else
		System.err.println("CP routine: SKY " + Position.toDegrees(sky, 2) + " IS NOT USABLE");

	} // next rot angle

	if (countValid == 0) {
	    taskLog.log(2, CLASS, "-", "", "CP routine: There were NO valid Cardinal points");
	    throw new CardinalPointingSolutionException("No valid cardinal points were found");
	}

	taskLog.log(2, CLASS, "-", "getBestCardinalPoint", "CP routine: There were " + countValid
		    + " Valid Cardinal points, Selecting sky offset: " + Position.toDegrees(selectAngle, 2) + " with "
		    + maxDtl + " degs to limit");

	return selectAngle;

    }


	private static double getPara(Source source, long time) {
		Position c = source.getPosition();
		double ha = source.getPosition().getHA(time);
		double lat = Position.phi;
		double cp = Math.cos(lat);
		double sp = Math.sin(lat);
		double dec = c.getDec();
		double sqsz = cp * Math.sin(ha);
		double cqsz = sp * Math.cos(dec) - cp * Math.sin(dec) * Math.cos(ha);
		double p3 = (( sqsz != 0.0 || cqsz != 0.0 ) ? Math.atan2 (sqsz, cqsz) : 0.0);
		return p3;		
	}
	
    
    
}
